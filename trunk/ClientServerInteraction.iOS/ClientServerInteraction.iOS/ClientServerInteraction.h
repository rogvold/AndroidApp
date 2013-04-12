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
#import "Session.h"
#import "Reachability.h"

@interface ClientServerInteraction : NSObject

+(void)authorizeWithEmail:(NSString*)email withPassword:(NSString*)password withDeviceId:(NSString*)deviceId completion:(void (^)(int code, AccessToken* response, NSError* error, ServerResponseError* serverError))completionBlock;

+(void)validateEmail:(NSString*)email completion:(void (^)(int code, NSNumber* response, NSError* error, ServerResponseError* serverError))completionBlock;

+(void)registerUserWithEmal:(NSString*)email withPassword:(NSString*)password completion:(void (^)(int code, NSNumber* response, NSError* error, ServerResponseError* serverError))completionBlock;

+(void)checkDataForEmail:(NSString*)email forPassword:(NSString*)password completion:(void (^)(int code, NSNumber* response, NSError* error, ServerResponseError* serverError))completionBlock;

+(void)getInfo:(NSString*)token completion:(void (^)(int code, User* response, NSError* error, ServerResponseError* serverError))completionBlock;

+(void)updateInfoForUser:(User*)user token:(NSString*)token completion:(void (^)(int code, NSNumber* response, NSError* error, ServerResponseError* serverError))completionBlock;

+(void)uploadRates:(NSArray*)rates start:(NSNumber*)start create:(NSNumber*)create token:(NSString*)token completion:(void (^)(int code, NSNumber* response, NSError* error, ServerResponseError* serverError))completionBlock;

+(void)syncRates:(NSArray*)rates start:(NSNumber*)start create:(NSNumber*)create token:(NSString*)token completion:(void (^)(int code, NSNumber* response, NSError* error, ServerResponseError* serverError))completionBlock;

+(void)getAllSessions:(NSString*)token completion:(void (^)(int code, NSArray* response, NSError* error, ServerResponseError* serverError))completionBlock;

+(void)getRatesForSessionId:(NSNumber*)sessionId token:(NSString*)token completion:(void (^)(int code, NSArray* response, NSError* error, ServerResponseError* serverError))completionBlock;

+(void)getTensionForSessionId:(NSNumber*)sessionId token:(NSString*)token completion:(void (^)(int code, NSDictionary* response, NSError* error, ServerResponseError* serverError))completionBlock;

+(void)checkIfServerIsReachable:(void(^)(bool response))callback;

@end
