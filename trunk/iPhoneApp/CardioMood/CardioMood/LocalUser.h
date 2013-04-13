//
//  LocalUser.h
//  CardioMood
//
//  Created by Alexander O. Taraymovich on 12.04.13.
//  Copyright (c) 2013 Alexander O. Taraymovich. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <CoreData/CoreData.h>


@interface LocalUser : NSManagedObject

@property (nonatomic, retain) NSNumber * userId;

@end
