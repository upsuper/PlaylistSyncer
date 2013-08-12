//
//  NSURL+relativePath.m
//  SyncerServer
//
//  Created by Xidorn Quan on 13-8-10.
//  Copyright (c) 2013年 Xidorn Quan. All rights reserved.
//

#import "NSURL+relativePath.h"

@implementation NSURL (relativePath)

- (NSString *)relativePath:(NSURL *)baseURL
{
    NSURL *full = [[self standardizedURL] absoluteURL];
    NSURL *base = [[baseURL standardizedURL] absoluteURL];
    NSArray *fullComponents = [full pathComponents];
    NSArray *baseComponents = [base pathComponents];
    NSUInteger fullCount = [fullComponents count];
    NSUInteger baseCount = [baseComponents count];

    NSMutableArray *result = [NSMutableArray array];
    int i = 0;
    for (; i < baseCount; i++) {
        if (![fullComponents[i] isEqualTo:baseComponents[i]])
            break;
    }
    for (; i < baseCount; i++)
        [result addObject:@".."];
    for (; i < fullCount; i++)
        [result addObject:fullComponents[i]];
    return [[NSURL fileURLWithPathComponents:result] relativePath];
}

@end
