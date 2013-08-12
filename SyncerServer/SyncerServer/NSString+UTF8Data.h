//
//  NSString+UTF8Data.h
//  SyncerServer
//
//  Created by Xidorn Quan on 13-8-12.
//  Copyright (c) 2013年 Xidorn Quan. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface NSString (UTF8Data)

+ (NSString *)stringWithUTF8Data:(NSData *)data;

- (NSString *)initWithUTF8Data:(NSData *)data;

- (NSData *)UTF8Data;

@end
