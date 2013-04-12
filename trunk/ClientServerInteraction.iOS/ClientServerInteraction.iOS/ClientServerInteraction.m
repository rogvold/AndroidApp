//
//  ClientServerInteraction.m
//  ClientServerInteraction
//
//  Created by Alexander O. Taraymovich on 01.04.13.
//  Copyright (c) 2013 Alexander O. Taraymovich. All rights reserved.
//

#import "ClientServerInteraction.h"
#import "Serializer_FrameworkOnly.h"
#import "HttpRequest_FrameworkOnly.h"

@interface ClientServerInteraction()

+(void)baseRequest:(NSString*)urlSuffix :(NSDictionary*)queryContent :(id)jsonObject :(void (^)(int code, id response, NSError* error, ServerResponseError* serverError))completionBlock :(Class)responseClass;

@end

@implementation ClientServerInteraction

NSString* const kBaseUrl = @"http://www.cardiomood.com/BaseProjectWeb/";
NSString* const kResources = @"resources/";
NSString* const kAuth = @"SecureAuth/";
NSString* const kSessions = @"SecureSessions/";
NSString* const kRates = @"SecureRatesUploading/";
NSString* const kSecret = @"h7a7RaRtvAVwnMGq5BV6";
NSString* const kIndicators = @"SecureIndicators/";
NSString* const kToken = @"token/";


+(void)authorizeWithEmail:(NSString*)email withPassword:(NSString*)password withDeviceId:(NSString*)deviceId completion:(void (^)(int code, AccessToken* response, NSError* error, ServerResponseError* serverError))completionBlock {
    [ClientServerInteraction baseRequest:[NSString stringWithFormat:@"%@%@", kToken, @"authorize"] :[NSDictionary dictionaryWithObjectsAndKeys:email, @"email", password, @"password", deviceId, @"deviceId", nil] :nil :completionBlock :[AccessToken class]];
}

+(void)validateEmail:(NSString*)email completion:(void (^)(int code, NSNumber* response, NSError* error, ServerResponseError* serverError))completionBlock {
    [ClientServerInteraction baseRequest:[NSString stringWithFormat:@"%@%@", kAuth, @"check_existence"] :[NSDictionary dictionaryWithObjectsAndKeys:email, @"email", nil] :nil :completionBlock :[NSNumber class]];
}

+(void)registerUserWithEmal:(NSString*)email withPassword:(NSString*)password completion:(void (^)(int code, NSNumber* response, NSError* error, ServerResponseError* serverError))completionBlock {
    [ClientServerInteraction baseRequest:[NSString stringWithFormat:@"%@%@", kAuth, @"register"] :[NSDictionary dictionaryWithObjectsAndKeys:email, @"email", password, @"password", nil] :nil :completionBlock :[NSNumber class]];
}

+(void)checkDataForEmail:(NSString*)email forPassword:(NSString*)password completion:(void (^)(int code, NSNumber* response, NSError* error, ServerResponseError* serverError))completionBlock {
    [ClientServerInteraction baseRequest:[NSString stringWithFormat:@"%@%@", kAuth, @"check_data"] :[NSDictionary dictionaryWithObjectsAndKeys:email, @"email", password, @"password", nil] :nil :completionBlock :[NSNumber class]];
}

+(void)getInfo:(NSString*)token completion:(void (^)(int code, User* response, NSError* error, ServerResponseError* serverError))completionBlock {
    [ClientServerInteraction baseRequest:[NSString stringWithFormat:@"%@%@", kAuth, @"info"] :[NSDictionary dictionaryWithObjectsAndKeys:token, @"token", nil] :nil :completionBlock :[User class]];
}

+(void)updateInfoForUser:(User*)user token:(NSString*)token completion:(void (^)(int code, NSNumber* response, NSError* error, ServerResponseError* serverError))completionBlock {
    [ClientServerInteraction baseRequest:[NSString stringWithFormat:@"%@%@", kAuth, @"update_info"] :[NSDictionary dictionaryWithObjectsAndKeys:token, @"token", nil] :user :completionBlock :[NSNumber class]];
}

+(void)getAllSessions:(NSString*)token completion:(void (^)(int code, NSArray* response, NSError* error, ServerResponseError* serverError))completionBlock {
    [ClientServerInteraction baseRequest:[NSString stringWithFormat:@"%@%@", kSessions, @"all"] :[NSDictionary dictionaryWithObjectsAndKeys:token, @"token", nil] :nil :completionBlock :[Session class]];
}

+(void)getRatesForSessionId:(NSNumber*)sessionId token:(NSString*)token completion:(void (^)(int code, NSArray* response, NSError* error, ServerResponseError* serverError))completionBlock {
    [ClientServerInteraction baseRequest:[NSString stringWithFormat:@"%@%@", kSessions, @"rates"] :[NSDictionary dictionaryWithObjectsAndKeys:sessionId, @"sessionId", token, @"token", nil] :nil :completionBlock :[NSArray class]];
}

+(void)getTensionForSessionId:(NSNumber*)sessionId token:(NSString*)token completion:(void (^)(int code, NSDictionary* response, NSError* error, ServerResponseError* serverError))completionBlock {
    [ClientServerInteraction baseRequest:[NSString stringWithFormat:@"%@%@/tension", kIndicators, sessionId] :[NSDictionary dictionaryWithObjectsAndKeys:sessionId, @"sessionId", token, @"token", nil] :nil :completionBlock :[NSDictionary class]];
}

+(void)uploadRates:(NSArray*)rates start:(NSNumber*)start create:(NSNumber*)create token:(NSString*)token completion:(void (^)(int code, NSNumber* response, NSError* error, ServerResponseError* serverError))completionBlock {
    [ClientServerInteraction baseRequest:[NSString stringWithFormat:@"%@%@", kRates, @"upload"] :[NSDictionary dictionaryWithObjectsAndKeys:token, @"token", nil] :[NSDictionary dictionaryWithObjectsAndKeys:start, @"start", rates, @"rates", create, @"create", nil] :completionBlock :[NSNumber class]];
}

+(void)syncRates:(NSArray*)rates start:(NSNumber*)start create:(NSNumber*)create token:(NSString*)token completion:(void (^)(int code, NSNumber* response, NSError* error, ServerResponseError* serverError))completionBlock {
    [ClientServerInteraction baseRequest:[NSString stringWithFormat:@"%@%@", kRates, @"sync"] :[NSDictionary dictionaryWithObjectsAndKeys:token, @"token", nil] :[NSDictionary dictionaryWithObjectsAndKeys:start, @"start", rates, @"rates", create, @"create", nil] :completionBlock :[NSNumber class]];
}

// private


+(void)baseRequest:(NSString*)urlSuffix :(NSDictionary*)queryContent :(id)jsonObject :(void (^)(int code, id response, NSError* error, ServerResponseError* serverError))completionBlock :(Class)responseClass{
    NSString* url = [NSString stringWithFormat:@"%@%@%@", kBaseUrl, kResources, urlSuffix];
    NSString* json = nil;
    if (jsonObject != nil)
        json = [Serializer serialize:jsonObject];
    NSMutableDictionary* cDict = [NSMutableDictionary dictionaryWithDictionary:queryContent];
    if (json != nil) {
        [cDict setObject:json forKey:@"json"];
    }
    NSString* content = [Serializer prepareQueryString:cDict];
    [HttpRequest httpPostJsonWithUrl:url :content :completionBlock :[HttpRequest createHandler:completionBlock :responseClass]];
    
}

+(void)checkIfServerIsReachable:(void(^)(bool response))callback {
    Reachability* reach = [Reachability reachabilityWithHostname:kBaseUrl];

    reach.reachableBlock = ^(Reachability*reach)
    {
        callback(true);
    };
    
    reach.unreachableBlock = ^(Reachability*reach)
    {
        callback(false);
    };
    
    [reach startNotifier];
}


                    







@end
