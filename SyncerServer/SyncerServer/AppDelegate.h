//
//  AppDelegate.h
//  SyncerServer
//
//  Created by Xidorn Quan on 13-7-9.
//  Copyright (c) 2013年 Xidorn Quan. All rights reserved.
//

#import <Cocoa/Cocoa.h>
#import "GCDAsyncSocket.h"

@interface AppDelegate : NSObject <
        NSApplicationDelegate, NSTableViewDataSource,
        NSNetServiceDelegate, GCDAsyncSocketDelegate>
{
    NSNetService *netService;
    GCDAsyncSocket *listenSocket;
    GCDAsyncSocket *transferSocket;

    NSMutableArray *trackList;
    NSMutableDictionary *trackFiles;
    NSMutableDictionary *playlistData;
}

@property (assign) IBOutlet NSWindow *window;
@property (weak) IBOutlet NSTableView *trackTable;
@property (weak) IBOutlet NSProgressIndicator *progressIndicator;
@property (weak) IBOutlet NSButton *startButton;
@property (weak) IBOutlet NSButton *retryButton;
@property (weak) IBOutlet NSTextField *statusLabel;

- (IBAction)doRetry:(id)sender;
- (IBAction)startSync:(id)sender;

@end
