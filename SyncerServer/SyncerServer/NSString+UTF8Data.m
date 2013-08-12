//
//  NSString+UTF8Data.m
//  SyncerServer
//
//  Created by Xidorn Quan on 13-8-12.
//  Copyright (c) 2013年 Xidorn Quan. All rights reserved.
//

#import "NSString+UTF8Data.h"

@implementation NSString (UTF8Data)

+ (NSString *)stringWithUTF8Data:(NSData *)data
{
    return [[NSString alloc] initWithUTF8Data:data];
}

- (NSString *)initWithUTF8Data:(NSData *)data
{
    return [self initWithData:data encoding:NSUTF8StringEncoding];
}

- (NSData *)UTF8Data
{
    return [self dataUsingEncoding:NSUTF8StringEncoding];
}

@end
