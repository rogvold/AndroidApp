//
//  User.h
//  ClientServerInteraction.iOS
//
//  Created by Yuriy Pogrebnyak on 02.04.13.
//  Copyright (c) 2013 Alexander O. Taraymovich. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface User : NSObject

@property (nonatomic, retain) NSString* about;
@property (nonatomic, retain) NSString* department;
@property (nonatomic, retain) NSString* description;
@property (nonatomic, retain) NSString* diagnosis;
@property (nonatomic, retain) NSString* email;
@property (nonatomic, retain) NSString* password;
@property (nonatomic, retain) NSString* firstName;
@property (nonatomic, retain) NSString* lastName;
@property (nonatomic, retain) NSString* statusMessage;
@property (nonatomic) int age;
@property (nonatomic) int sex;
@property (nonatomic) double height;
@property (nonatomic) double weight;

@end