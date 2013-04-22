//
//  ServerResponseError.h
//  ClientServerInteraction.iOS
//
//  Created by Yuriy Pogrebnyak on 06.04.13.
//  Copyright (c) 2013 Alexander O. Taraymovich. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface ServerResponseError : NSObject

extern int const InvalidToken;
extern int const OtherError;

@property (nonatomic, retain) NSString* message;
@property (nonatomic) int errorCode;

@end
