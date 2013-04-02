//
//  ClientServerInteraction.m
//  ClientServerInteraction
//
//  Created by Alexander O. Taraymovich on 01.04.13.
//  Copyright (c) 2013 Alexander O. Taraymovich. All rights reserved.
//

#import "ClientServerInteraction.h"

@interface ClientServerInteraction()

+(void)httpPostJsonWithUrl:(NSString*)url withQueryString:(NSString*)queryString withJson:(NSString*)json completion:(void (^)(bool response, NSError* error))completionBlock withHandler:(void (^)(NSURLResponse *urlResponse,NSData *responseData, NSError *error))handler;

+(NSString*)prepareQueryString:(NSDictionary*)params;

+(void)commonBoolRequest:(NSString*)suffix withEmail:(NSString*)email withPassword:(NSString*)password completion:(void (^)(bool response, NSError* error))completionBlock;

+(void (^)(NSURLResponse *urlResponse,NSData *responseData, NSError *error))createBoolHandler:(void (^)(bool response, NSError* error))completionBlock;

@end

@implementation ClientServerInteraction

NSString* const kBaseUrl = @"http://www.cardiomood.com/BaseProjectWeb/";
NSString* const kResources = @"resources/";
NSString* const kAuth = @"auth/";
NSString* const kRates = @"rates/";
NSString* const kSecret = @"h7a7RaRtvAVwnMGq5BV6";

+(void)validateEmail:(NSString*)email completion:(void (^)(bool response, NSError* error))completionBlock {
    [ClientServerInteraction commonBoolRequest:@"check_existence" withEmail:email withPassword:nil completion:completionBlock];
}

+(void)registerUser:(NSString*)email withPassword:(NSString*)password completion:(void (^)(bool response, NSError* error))completionBlock {
    [ClientServerInteraction commonBoolRequest:@"register" withEmail:email withPassword:password completion:completionBlock];
}

+(void)rcheckData:(NSString*)email withPassword:(NSString*)password completion:(void (^)(bool response, NSError* error))completionBlock {
    [ClientServerInteraction commonBoolRequest:@"check_data" withEmail:email withPassword:password completion:completionBlock];
}

+(void)httpPostJsonWithUrl:(NSString*)url withQueryString:(NSString*)queryString withJson:(NSString*)json completion:(void (^)(bool response, NSError* error))completionBlock withHandler:(void (^)(NSURLResponse *urlResponse,NSData *responseData, NSError *error))handler {
    NSMutableURLRequest *request =
    [[NSMutableURLRequest alloc] initWithURL:
     [NSURL URLWithString:[NSString stringWithFormat:@"%@%@", url, queryString]]];
    
    [request setHTTPMethod:@"POST"];
    [request setValue:@"application/x-www-form-urlencoded" forHTTPHeaderField:@"Content-Type"];
    
    
    [request setHTTPBody:[json
                          dataUsingEncoding:NSUTF8StringEncoding]];
    
    [NSURLConnection sendAsynchronousRequest:request queue:[NSOperationQueue mainQueue] completionHandler:handler];
}

+(NSString*)prepareQueryString:(NSDictionary*)params {
    if (params == nil)
        return @"";
    NSString* result = @"?";    
    for (id key in params) {
        id value = [params objectForKey:key];
        result = [NSString stringWithFormat:[result isEqual: @"?"] ? @"%@%@=%@" : @"%@&%@=%@", result, key, value];
    }
    return result;
}

+(void)commonBoolRequest:(NSString*)suffix withEmail:(NSString*)email withPassword:(NSString*)password completion:(void (^)(bool response, NSError* error))completionBlock {
    NSString* url = [NSString stringWithFormat:@"%@%@%@%@", kBaseUrl, kResources, kAuth, suffix];
    NSString* queryString = [ClientServerInteraction prepareQueryString:[NSDictionary dictionaryWithObjectsAndKeys:kSecret, @"secret", email, @"email", password, @"password", nil]];
    [ClientServerInteraction httpPostJsonWithUrl:url withQueryString:queryString withJson:@"" completion:completionBlock withHandler:[ClientServerInteraction createBoolHandler:completionBlock]];
}

+(void (^)(NSURLResponse *urlResponse,NSData *responseData, NSError *error))createBoolHandler:(void (^)(bool response, NSError* error))completionBlock {
    return ^( NSURLResponse *urlResponse,NSData *responseData, NSError *error){
        if (error != nil) {
            completionBlock(nil, error);
            return;
        }
        NSDictionary* json = [NSJSONSerialization
                              JSONObjectWithData:responseData
                              
                              options:kNilOptions
                              error:&error];
        if ([json objectForKey:@"error"] != nil) {
            error = [NSError errorWithDomain:@"server.response.error" code:42 userInfo:[json objectForKey:@"error"]];
            completionBlock(nil, error);
            return;
        }
        bool resp = [[json objectForKey:@"response"] isEqual: @"1"];
        completionBlock(resp, nil);
    };
}



@end
