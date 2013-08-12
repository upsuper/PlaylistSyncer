//
//  NSURL+relativePath.h
//  SyncerServer
//
//  Created by Xidorn Quan on 13-8-10.
//  Copyright (c) 2013年 Xidorn Quan. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface NSURL (relativePath)

- (NSString *)relativePath:(NSURL *)baseURL;

@end
