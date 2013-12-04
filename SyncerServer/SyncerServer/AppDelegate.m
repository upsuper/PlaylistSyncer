//
//  AppDelegate.m
//  SyncerServer
//
//  Created by Xidorn Quan on 13-7-9.
//  Copyright (c) 2013年 Xidorn Quan. All rights reserved.
//

#import "AppDelegate.h"
#import "NSURL+relativePath.h"
#import "NSString+UTF8Data.h"
#import <CommonCrypto/CommonCrypto.h>
#include <math.h>

enum {
    kTagEstablish,
    kTagFileList,
    kTagDownloadRequest,
    kTagSendFile,
    kTagSendFileHash,
    kTagSendPlaylist,
};

NSData *kNewLine;

NSString *humanReadableBytes(NSUInteger bytes, BOOL si)
{
    int unit = si ? 1000 : 1024;
    if (bytes < unit)
        return [NSString stringWithFormat:@"%lu B", bytes];
    int exp = (int)(log(bytes) / log(unit));
    char pre = (si ? "kMGTPE" : "KMGTPE")[exp - 1];
    NSString *format = si ? @"%.2f %ciB" : @"%.2f %cB";
    return [NSString stringWithFormat:format, bytes / pow(unit, exp), pre];
}

NSString *trimString(NSString *string)
{
    return [string stringByTrimmingCharactersInSet:
            [NSCharacterSet whitespaceAndNewlineCharacterSet]];
}

@implementation AppDelegate

- (void)applicationDidFinishLaunching:(NSNotification *)aNotification
{
    NSString *libraryPath = [NSHomeDirectory()
                             stringByAppendingPathComponent:@"Music/iTunes/iTunes Music Library.xml"];
    NSURL *mediaDir = [NSURL fileURLWithPath:
                       [NSHomeDirectory() stringByAppendingPathComponent:@"Music/iTunes/iTunes Media/Music"]];
    NSDictionary *musicLibrary = [NSDictionary dictionaryWithContentsOfFile:libraryPath];

    kNewLine = [GCDAsyncSocket LFData];

    NSDictionary *tracks = musicLibrary[@"Tracks"];
    NSArray *playlists = [musicLibrary[@"Playlists"] filteredArrayUsingPredicate:
                 [NSPredicate predicateWithBlock:^BOOL(id playlist, NSDictionary *bindings) {
        if (playlist[@"Visible"] != nil)
            return FALSE;
        if (playlist[@"Distinguished Kind"] != nil)
            return FALSE;
        return TRUE;
    }]];

    trackList = [NSMutableArray array];
    trackFiles = [NSMutableDictionary dictionary];
    playlistData = [NSMutableDictionary dictionaryWithCapacity:[playlists count]];
    for (id playlist in playlists) {
        NSArray *items = playlist[@"Playlist Items"];
        NSMutableArray *itemList = [NSMutableArray arrayWithCapacity:[items count]];
        playlistData[playlist[@"Name"]] = itemList;

        for (id item in items) {
            id trackID = item[@"Track ID"];
            trackID = [trackID stringValue];
            id track = tracks[trackID];
            NSURL *location = [NSURL URLWithString:track[@"Location"]];
            NSString *fileId = [location relativePath:mediaDir];
            [itemList addObject:fileId];

            if ([trackFiles objectForKey:fileId] == nil) {
                id itemData = @{
                    @"Track ID": trackID,
                    @"Name": track[@"Name"],
                    @"Size": track[@"Size"],
                    @"File ID": fileId,
                    @"Location": location
                    };
                [trackList addObject:itemData];
                trackFiles[fileId] = itemData;
            }
        }
    }
    [self.trackTable reloadData];

    [self publishService];
}

- (BOOL)applicationShouldTerminateAfterLastWindowClosed:(NSApplication *)theApplication {
    return YES;
}

- (void)publishService
{
    [transferSocket disconnect];
    transferSocket = nil;

    [self.retryButton setEnabled:NO];
    [self.progressIndicator setIndeterminate:YES];
    [self.progressIndicator startAnimation:self];

    listenSocket = [[GCDAsyncSocket alloc] initWithDelegate:self delegateQueue:dispatch_get_main_queue()];
    BOOL ret = [listenSocket acceptOnPort:0 error:nil];
    assert(ret);

    UInt16 port = [listenSocket localPort];
    netService = [[NSNetService alloc] initWithDomain:@"local."
                                                 type:@"_PlaylistSyncer._tcp."
                                                 name:@""
                                                 port:port];
    [netService setDelegate:self];
    [netService publish];
}

- (void)cleanListening
{
    [netService stop];
    [listenSocket disconnect];
}

- (void)socket:(GCDAsyncSocket *)sock didAcceptNewSocket:(GCDAsyncSocket *)newSocket
{
    [self cleanListening];
    [self.retryButton setEnabled:YES];

    transferSocket = newSocket;
    [transferSocket readDataToData:kNewLine withTimeout:-1 tag:kTagEstablish];
}

- (void)socket:(GCDAsyncSocket *)sock didReadData:(NSData *)data withTag:(long)tag
{
    switch (tag) {
        case kTagEstablish: {
            NSString *confirmInfo = [NSString stringWithUTF8Data:data];
            confirmInfo = trimString(confirmInfo);

            NSAlert *alert = [NSAlert alertWithMessageText:@"Connected"
                                             defaultButton:@"Connect" alternateButton:@"Cancel" otherButton:nil
                                 informativeTextWithFormat:@"Confirm info: %@", confirmInfo];
            NSUInteger result = [alert runModal];
            if (result != NSAlertDefaultReturn) {
                [self publishService];
            } else {
                [self.progressIndicator stopAnimation:self];
                transfer = [NSMutableArray array];
                currentItem = nil;

                for (id item in trackList) {
                    NSString *data = [NSString stringWithFormat:@"%@ %lu\n",
                                      item[@"File ID"], [item[@"Size"] unsignedLongValue]];
                    [transferSocket writeData:[data UTF8Data] withTimeout:-1 tag:kTagFileList];
                }
                [transferSocket writeData:kNewLine withTimeout:-1 tag:kTagFileList];
                [transferSocket readDataToData:kNewLine withTimeout:-1 tag:kTagDownloadRequest];
            }
            break;
        }

        case kTagDownloadRequest: {
            NSString *fileId = [NSString stringWithUTF8Data:data];
            fileId = trimString(fileId);
            if (![fileId isEqualToString:@""]) {
                id item = trackFiles[fileId];
                NSUInteger size = [item[@"Size"] unsignedLongValue];

                [self.progressIndicator setIndeterminate:NO];
                [self.progressIndicator setMaxValue:size];
                [self.progressIndicator setDoubleValue:0];
                [self.progressIndicator startAnimation:self];

                NSData *fileData = [NSData dataWithContentsOfURL:item[@"Location"]];
                if ([fileData length] != size) {
                    [NSApp terminate:self];
                }

                [transfer addObject:item];
                currentItem = item;
                [self.trackTable reloadData];
                [transferSocket writeData:fileData withTimeout:-1 tag:kTagSendFile];

                unsigned char md5[16];
                CC_MD5([fileData bytes], (CC_LONG) [fileData length], md5);
                [transferSocket writeData:[NSData dataWithBytes:md5 length:sizeof(md5)]
                              withTimeout:-1 tag:kTagSendFileHash];

                [transferSocket readDataToData:kNewLine withTimeout:-1 tag:kTagDownloadRequest];
            } else {
                [self.progressIndicator setIndeterminate:YES];
                [self.progressIndicator startAnimation:self];
                currentItem = nil;

                // send playlist
                for (NSString *name in playlistData) {
                    NSString *nameData = [NSString stringWithFormat:@"%@\n", name];
                    [transferSocket writeData:[nameData UTF8Data] withTimeout:-1 tag:kTagSendPlaylist];
                    for (NSString *item in playlistData[name]) {
                        NSString *fileId = [NSString stringWithFormat:@"%@\n", item];
                        [transferSocket writeData:[fileId UTF8Data] withTimeout:-1 tag:kTagSendPlaylist];
                    }
                    [transferSocket writeData:kNewLine withTimeout:-1 tag:kTagSendPlaylist];
                }
                [transferSocket writeData:kNewLine withTimeout:-1 tag:kTagSendPlaylist];
            }
            break;
        }

        default:
            break;
    }

    [self.trackTable reloadData];
}

- (void)socket:(GCDAsyncSocket *)sock didWritePartialDataOfLength:(NSUInteger)partialLength tag:(long)tag
{
    switch (tag) {
        case kTagSendFile:
            [self.progressIndicator incrementBy:partialLength];
            break;

        default:
            break;
    }
}

- (void)socketDidDisconnect:(GCDAsyncSocket *)sock withError:(NSError *)err
{
    if (sock == transferSocket)
        [self publishService];
}

- (NSInteger)numberOfRowsInTableView:(NSTableView *)aTableView
{
    return [transfer count];
}

- (id)tableView:(NSTableView *)aTableView objectValueForTableColumn:(NSTableColumn *)aTableColumn
            row:(NSInteger)rowIndex
{
    NSString *identifier = [aTableColumn identifier];
    id data = transfer[rowIndex];
    if ([identifier isEqualToString:@"status"]) {
        return [NSImage imageNamed:
                data == currentItem ? NSImageNameStatusPartiallyAvailable : NSImageNameStatusAvailable];
    } else if ([identifier isEqualToString:@"name"]) {
        return data[@"Name"];
    } else if ([identifier isEqualToString:@"size"]) {
        return humanReadableBytes([data[@"Size"] unsignedLongValue], NO);
    }
    return nil;
}

- (IBAction)doRetry:(id)sender {
    [self publishService];
}

@end
