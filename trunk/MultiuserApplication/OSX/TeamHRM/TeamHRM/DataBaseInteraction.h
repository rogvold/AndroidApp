//
//  DataBaseInteraction.h
//  TeamHRM
//
//  Created by Alexander O. Taraymovich on 13.03.13.
//  Copyright (c) 2013 Alexander O. Taraymovich. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <sqlite3.h>

@interface DataBaseInteraction : NSObject {
    sqlite3 *database;
}

- (id)initWithPath:(NSString *)path;
- (NSArray *)performQuery:(NSString *)query;

@end
