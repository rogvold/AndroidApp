//
//  ClientServerInteraction.h
//  ClientServerInteraction
//
//  Created by Alexander O. Taraymovich on 01.04.13.
//  Copyright (c) 2013 Alexander O. Taraymovich. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "User.h"
#import "ServerResponseError.h"
#import "AccessToken.h"

@interface ClientServerInteraction : NSObject



//public

+(void)authorize:(NSString*)email :(NSString*)password :(NSString*)deviceId :(void (^)(int code, AccessToken* response, NSError* error, ServerResponseError* serverError))completionBlock;

+(void)validateEmail:(NSString*)email :(void (^)(int code, NSNumber* response, NSError* error, ServerResponseError* serverError))completionBlock;

+(void)registerUser:(NSString*)email :(NSString*)password :(void (^)(int code, NSNumber* response, NSError* error, ServerResponseError* serverError))completionBlock;

+(void)checkData:(NSString*)email :(NSString*)password :(void (^)(int code, NSNumber* response, NSError* error, ServerResponseError* serverError))completionBlock;

+(void)getInfo:(NSString*)token :(void (^)(int code, User* response, NSError* error, ServerResponseError* serverError))completionBlock;

+(void)updateInfo:(User*)user :(NSString*)token :(void (^)(int code, NSNumber* response, NSError* error, ServerResponseError* serverError))completionBlock;

+(void)upload:(NSString*)email withPassword:(NSString*)password rates:(NSArray*) rates start:(long long) start completion:(void (^)(NSNumber* response, NSError* error))completionBlock;

+(void)sync:(NSString*)email withPassword:(NSString*)password rates:(NSArray*) rates start:(long long) start completion:(void (^)(NSNumber* response, NSError* error))completionBlock;


@end
