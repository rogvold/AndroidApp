//
//  ClientServerInteraction.h
//  ClientServerInteraction
//
//  Created by Alexander O. Taraymovich on 01.04.13.
//  Copyright (c) 2013 Alexander O. Taraymovich. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface ClientServerInteraction : NSObject

//private

+(void)httpPostJsonWithUrl:(NSString*)url withQueryString:(NSString*)queryString withJson:(NSString*)json completion:(void (^)(bool response, NSError* error))completionBlock withHandler:(void (^)(NSURLResponse *urlResponse,NSData *responseData, NSError *error))handler;

+(NSString*)prepareQueryString:(NSDictionary*)params;

+(void)commonBoolRequest:(NSString*)suffix withEmail:(NSString*)email withPassword:(NSString*)password completion:(void (^)(bool response, NSError* error))completionBlock;

+(void (^)(NSURLResponse *urlResponse,NSData *responseData, NSError *error))createBoolHandler:(void (^)(bool response, NSError* error))completionBlock;


//public

+(void)validateEmail:(NSString*)email completion:(void (^)(bool response, NSError* error))completionBlock;

+(void)registerUser:(NSString*)email withPassword:(NSString*)password completion:(void (^)(bool response, NSError* error))completionBlock;

+(void)checkData:(NSString*)email withPassword:(NSString*)password completion:(void (^)(bool response, NSError* error))completionBlock;


@end
