//
//  ClientServerInteraction.m
//  ClientServerInteraction
//
//  Created by Alexander O. Taraymovich on 01.04.13.
//  Copyright (c) 2013 Alexander O. Taraymovich. All rights reserved.
//

#import "ClientServerInteraction.h"

@interface ClientServerInteraction()

+(void)httpPostJsonWithUrl:(NSString*)url withQueryString:(NSString*)queryString withJson:(NSString*)json completion:(void (^)(id response, NSError* error))completionBlock withHandler:(void (^)(NSURLResponse *urlResponse,NSData *responseData, NSError *error))handler;

+(NSString*)prepareQueryString:(NSDictionary*)params;

+(void)commonBoolRequest:(NSString*)suffix withEmail:(NSString*)email withPassword:(NSString*)password completion:(void (^)(NSNumber* response, NSError* error))completionBlock;

+(void (^)(NSURLResponse *urlResponse,NSData *responseData, NSError *error))createBoolHandler:(void (^)(NSNumber* response, NSError* error))completionBlock;

+(void (^)(NSURLResponse *urlResponse,NSData *responseData, NSError *error))createUserHandler:(void (^)(User* response, NSError* error))completionBlock;

+(NSString*)serializeUser:(User*) user;

+(User*)deserializeUser:(NSDictionary*) dict;

+(void)commonUploadRequest:(NSString*)suffix withEmail:(NSString*)email withPassword:(NSString*)password rates:(NSArray*) rates start:(long long) start create:(int)create completion:(void (^)(NSNumber* response, NSError* error))completionBlock;

+(NSString*)serializeSession:(NSString*)email withPassword:(NSString*)password rates:(NSArray*) rates start:(long long) start create:(int)create;

@end

@implementation ClientServerInteraction

NSString* const kBaseUrl = @"http://www.cardiomood.com/BaseProjectWeb/";
NSString* const kResources = @"resources/";
NSString* const kAuth = @"auth/";
NSString* const kRates = @"rates/";
NSString* const kSecret = @"h7a7RaRtvAVwnMGq5BV6";

+(void)validateEmail:(NSString*)email completion:(void (^)(NSNumber* response, NSError* error))completionBlock {
    [ClientServerInteraction commonBoolRequest:@"check_existence" withEmail:email withPassword:nil completion:completionBlock];
}

+(void)registerUser:(NSString*)email withPassword:(NSString*)password completion:(void (^)(NSNumber* response, NSError* error))completionBlock {
    [ClientServerInteraction commonBoolRequest:@"register" withEmail:email withPassword:password completion:completionBlock];
}

+(void)checkData:(NSString*)email withPassword:(NSString*)password completion:(void (^)(NSNumber* response, NSError* error))completionBlock {
    [ClientServerInteraction commonBoolRequest:@"check_data" withEmail:email withPassword:password completion:completionBlock];
}

+(void)getInfo:(NSString*)email withPassword:(NSString*)password completion:(void (^)(User* response, NSError* error))completionBlock {
    NSString* url = [NSString stringWithFormat:@"%@%@%@info", kBaseUrl, kResources, kAuth];
    NSString* queryString = [ClientServerInteraction prepareQueryString:[NSDictionary dictionaryWithObjectsAndKeys:kSecret, @"secret", email, @"email", password, @"password", nil]];
    [ClientServerInteraction httpPostJsonWithUrl:url withQueryString:queryString withJson:@"" completion:completionBlock withHandler:[ClientServerInteraction createUserHandler:completionBlock]];
}

+(void)updateInfo:(User*)user completion:(void (^)(NSNumber* response, NSError* error))completionBlock {
    NSString* url = [NSString stringWithFormat:@"%@%@%@update_info_form", kBaseUrl, kResources, kAuth];
    NSString* json = [ClientServerInteraction serializeUser:user];
    NSString* queryString = [ClientServerInteraction prepareQueryString:[NSDictionary dictionaryWithObjectsAndKeys:kSecret, @"secret", nil]];
    [ClientServerInteraction httpPostJsonWithUrl:url withQueryString:queryString withJson:[NSString stringWithFormat:@"json=%@", json] completion:completionBlock withHandler:[ClientServerInteraction createBoolHandler:completionBlock]];
}

+(void)upload:(NSString*)email withPassword:(NSString*)password rates:(NSArray*) rates start:(long long) start completion:(void (^)(NSNumber* response, NSError* error))completionBlock {
    [ClientServerInteraction commonUploadRequest:@"upload" withEmail:email withPassword:password rates:rates start:start create:1 completion:completionBlock];
}

+(void)sync:(NSString*)email withPassword:(NSString*)password rates:(NSArray*) rates start:(long long) start completion:(void (^)(NSNumber* response, NSError* error))completionBlock {
    [ClientServerInteraction commonUploadRequest:@"sync" withEmail:email withPassword:password rates:rates start:start create:0 completion:completionBlock];
}




// private

+(void)httpPostJsonWithUrl:(NSString*)url withQueryString:(NSString*)queryString withJson:(NSString*)json completion:(void (^)(id response, NSError* error))completionBlock withHandler:(void (^)(NSURLResponse *urlResponse,NSData *responseData, NSError *error))handler {
    //queryString = [queryString stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding];
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

+(void)commonBoolRequest:(NSString*)suffix withEmail:(NSString*)email withPassword:(NSString*)password completion:(void (^)(NSNumber* response, NSError* error))completionBlock {
    NSString* url = [NSString stringWithFormat:@"%@%@%@%@", kBaseUrl, kResources, kAuth, suffix];
    NSString* queryString = [ClientServerInteraction prepareQueryString:[NSDictionary dictionaryWithObjectsAndKeys:kSecret, @"secret", email, @"email", password, @"password", nil]];
    [ClientServerInteraction httpPostJsonWithUrl:url withQueryString:queryString withJson:@"" completion:completionBlock withHandler:[ClientServerInteraction createBoolHandler:completionBlock]];
}

+(void (^)(NSURLResponse *urlResponse,NSData *responseData, NSError *error))createBoolHandler:(void (^)(NSNumber* response, NSError* error))completionBlock {
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
        completionBlock([NSNumber numberWithBool:resp], nil);
    };
}

+(void (^)(NSURLResponse *urlResponse,NSData *responseData, NSError *error))createUserHandler:(void (^)(User* response, NSError* error))completionBlock {
    return ^( NSURLResponse *urlResponse,NSData *responseData, NSError *error){
        if (error != nil) {
            completionBlock(nil, error);
            return;
        }
        NSDictionary* json = [NSJSONSerialization
                              JSONObjectWithData:responseData
                              
                              options:kNilOptions
                              error:&error];
        if (json != nil && [json objectForKey:@"error"] != nil) {
            error = [NSError errorWithDomain:@"server.response.error" code:42 userInfo:[json objectForKey:@"error"]];
            completionBlock(nil, error);
            return;
        }
        User* user = [ClientServerInteraction deserializeUser:json];
        completionBlock(user, nil);
    };
}

+(NSString*)serializeUser:(User*) user {
    NSArray* objects = [NSArray arrayWithObjects:user.about, [NSString stringWithFormat:@"%d", user.age], user.description, user.department, user.diagnosis, user.email, user.firstName, user.lastName, [NSString stringWithFormat:@"%f", user.height], user.password, [NSString stringWithFormat:@"%d", user.sex], user.statusMessage, [NSString stringWithFormat:@"%f", user.weight], nil];
                        
    NSArray* keys = [NSArray arrayWithObjects:@"about", @"age", @"description", @"department", @"diagnosis", @"email", @"firstName", @"lastName", @"height", @"password", @"sex", @"statusMessage", @"weight", nil];
    NSData* data = [NSJSONSerialization dataWithJSONObject:[NSDictionary dictionaryWithObjects:objects forKeys:keys] options:NSJSONWritingPrettyPrinted error:nil];
    NSString* jsonString = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
    
    return jsonString;
}

+(User*)deserializeUser:(NSDictionary*) dict {
    User* user = [User alloc];
    user.about = [dict objectForKey:@"about"];
    user.age = [[dict objectForKey:@"age"] integerValue];
    user.description = [dict objectForKey:@"description"];
    user.department = [dict objectForKey:@"department"];
    user.diagnosis = [dict objectForKey:@"diagnosis"];
    user.email = [dict objectForKey:@"email"];
    user.firstName = [dict objectForKey:@"firstName"];
    user.lastName = [dict objectForKey:@"lastName"];
    user.height = [[dict objectForKey:@"height"] doubleValue];
    user.password = [dict objectForKey:@"password"];
    user.sex = [[dict objectForKey:@"sex"] integerValue];
    user.statusMessage = [dict objectForKey:@"statusMessage"];
    user.weight = [[dict objectForKey:@"weight"] doubleValue];
    return user;
}

+(void)commonUploadRequest:(NSString*)suffix withEmail:(NSString*)email withPassword:(NSString*)password rates:(NSArray*) rates start:(long long) start create:(int)create completion:(void (^)(NSNumber* response, NSError* error))completionBlock {
    NSString* url = [NSString stringWithFormat:@"%@%@%@%@", kBaseUrl, kResources, kRates, suffix];

    NSString* json = [ClientServerInteraction serializeSession:email withPassword:password rates:rates start:start create:create];
    
    [ClientServerInteraction httpPostJsonWithUrl:url withQueryString:@"" withJson:json completion:completionBlock withHandler:[ClientServerInteraction createBoolHandler:completionBlock]];
}

+(NSString*)serializeSession:(NSString*)email withPassword:(NSString*)password rates:(NSArray*) rates start:(long long) start create:(int)create {
    NSArray* objects = [NSArray arrayWithObjects:email, password, rates, [NSString stringWithFormat:@"%lld", start], [NSNumber numberWithInt:create], nil];
    
    NSArray* keys = [NSArray arrayWithObjects:@"email", @"password", @"rates", @"start", @"create", nil];
    NSData* data = [NSJSONSerialization dataWithJSONObject:[NSDictionary dictionaryWithObjects:objects forKeys:keys] options:NSJSONWritingPrettyPrinted error:nil];
    NSString* json = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
    return json;
}
                    







@end
