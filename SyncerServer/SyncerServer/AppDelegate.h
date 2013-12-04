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

    id currentItem;
    NSMutableArray *transfer;
}

@property (assign) IBOutlet NSWindow *window;
@property (weak) IBOutlet NSTableView *trackTable;
@property (weak) IBOutlet NSProgressIndicator *progressIndicator;
@property (weak) IBOutlet NSButton *retryButton;

- (IBAction)doRetry:(id)sender;

@end
