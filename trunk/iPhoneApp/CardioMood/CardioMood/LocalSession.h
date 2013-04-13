//
//  LocalSession.h
//  CardioMood
//
//  Created by Alexander O. Taraymovich on 12.04.13.
//  Copyright (c) 2013 Alexander O. Taraymovich. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <CoreData/CoreData.h>

@class LocalUser;

@interface LocalSession : NSManagedObject

@property (nonatomic, retain) NSDate * startTime;
@property (nonatomic, retain) LocalUser *user;
@property (nonatomic, retain) NSString *rates;

@end
