//
//  AccessToken.h
//  ClientServerInteraction.iOS
//
//  Created by Yuriy Pogrebnyak on 06.04.13.
//  Copyright (c) 2013 Alexander O. Taraymovich. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface AccessToken : NSObject

@property (nonatomic, retain) NSString* token;

@property (nonatomic, retain) NSNumber* expiredDate;

@end
