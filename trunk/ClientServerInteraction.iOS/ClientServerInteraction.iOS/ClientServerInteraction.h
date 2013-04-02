//
//  ClientServerInteraction.h
//  ClientServerInteraction
//
//  Created by Alexander O. Taraymovich on 01.04.13.
//  Copyright (c) 2013 Alexander O. Taraymovich. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "User.h"

@interface ClientServerInteraction : NSObject



//public

+(void)validateEmail:(NSString*)email completion:(void (^)(NSNumber* response, NSError* error))completionBlock;

+(void)registerUser:(NSString*)email withPassword:(NSString*)password completion:(void (^)(NSNumber* response, NSError* error))completionBlock;

+(void)checkData:(NSString*)email withPassword:(NSString*)password completion:(void (^)(NSNumber* response, NSError* error))completionBlock;

+(void)getInfo:(NSString*)email withPassword:(NSString*)password completion:(void (^)(User* response, NSError* error))completionBlock;

+(void)updateInfo:(User*)user completion:(void (^)(NSNumber* response, NSError* error))completionBlock;

+(void)upload:(NSString*)email withPassword:(NSString*)password rates:(NSArray*) rates start:(long long) start completion:(void (^)(NSNumber* response, NSError* error))completionBlock;

+(void)sync:(NSString*)email withPassword:(NSString*)password rates:(NSArray*) rates start:(long long) start completion:(void (^)(NSNumber* response, NSError* error))completionBlock;


@end
