//
//  Serializer_Serializer_FrameworkOnly.h
//  ClientServerInteraction.iOS
//
//  Created by Yuriy Pogrebnyak on 07.04.13.
//  Copyright (c) 2013 Alexander O. Taraymovich. All rights reserved.
//

#import "Serializer.h"
#import "ServerResponseError.h"
#import "User.h"
#import "Session.h"
#import "AccessToken.h"

@interface Serializer ()

+(NSString*)prepareQueryString:(NSDictionary*)params;

+(NSString*)serialize:(id)obj;

+(id)deserialize:(id)obj :(Class)class;

+(void)deserializeResponse:(NSDictionary*)json :(Class)responseClass :(void (^)(int code, id response, NSError* error, ServerResponseError* serverError))completionBlock;

+(NSString*)serializeUser:(User*) user;

+(User*)deserializeUser:(NSDictionary*) dict;

+(NSString*)serializeSession:(NSString*)email withPassword:(NSString*)password rates:(NSArray*) rates start:(long long) start create:(int)create;

+(NSNumber*)deserializeBool:(NSString*)value;

+(AccessToken*)deserializeAccessToken:(NSDictionary*)dict;

+(ServerResponseError*)deserializeServerError:(NSDictionary*)dict;

@end
