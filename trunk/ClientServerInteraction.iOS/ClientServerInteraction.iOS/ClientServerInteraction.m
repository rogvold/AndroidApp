//
//  ClientServerInteraction.m
//  ClientServerInteraction
//
//  Created by Alexander O. Taraymovich on 01.04.13.
//  Copyright (c) 2013 Alexander O. Taraymovich. All rights reserved.
//

#import "ClientServerInteraction.h"

@interface ClientServerInteraction()

+(void)httpPostJsonWithUrl:(NSString*)url :(NSString*)content :(void (^)(int code, id response, NSError* error, ServerResponseError* serverError))completionBlock
                          :(void (^)(NSURLResponse *urlResponse,NSData *responseData, NSError *error))handler;

+(NSString*)prepareQueryString:(NSDictionary*)params;

+(void (^)(NSURLResponse *urlResponse,NSData *responseData, NSError *error))createHandler :(void (^)(int code, id response, NSError* error, ServerResponseError* serverError))completionBlock :(Class)responseClass;

+(NSString*)serialize:(id)obj;

+(id)deserialize:(id)obj :(Class)class;

+(void)deserializeResponse:(NSDictionary*)json :(Class)responseClass :(void (^)(int code, id response, NSError* error, ServerResponseError* serverError))completionBlock;

+(NSString*)serializeUser:(User*) user;

+(User*)deserializeUser:(NSDictionary*) dict;

+(NSString*)serializeSession:(NSString*)email withPassword:(NSString*)password rates:(NSArray*) rates start:(long long) start create:(int)create;

+(void)baseRequest:(NSString*)urlSuffix :(NSDictionary*)queryContent :(id)jsonObject :(void (^)(int code, id response, NSError* error, ServerResponseError* serverError))completionBlock :(Class)responseClass;

+(NSNumber*)deserializeBool:(NSString*)value;

+(AccessToken*)deserializeAccessToken:(NSDictionary*)dict;

+(ServerResponseError*)deserializeServerError:(NSDictionary*)dict;

@end

@implementation ClientServerInteraction

NSString* const kBaseUrl = @"http://www.cardiomood.com/BaseProjectWeb/";
NSString* const kResources = @"resources/";
NSString* const kAuth = @"SecureAuth/";
NSString* const kRates = @"rates/";
NSString* const kSecret = @"h7a7RaRtvAVwnMGq5BV6";
NSString* const kToken = @"token/";

+(void)baseRequest:(NSString*)urlSuffix :(NSDictionary*)queryContent :(id)jsonObject :(void (^)(int code, id response, NSError* error, ServerResponseError* serverError))completionBlock :(Class)responseClass{
    NSString* url = [NSString stringWithFormat:@"%@%@%@", kBaseUrl, kResources, urlSuffix];
    NSString* json = nil;
    if (jsonObject != nil)
        json = [ClientServerInteraction serialize:jsonObject];
    NSMutableDictionary* cDict = [NSMutableDictionary dictionaryWithDictionary:queryContent];
    if (json != nil) {
        [cDict setObject:json forKey:@"json"];
    }        
    NSString* content = [ClientServerInteraction prepareQueryString:cDict];
    [ClientServerInteraction httpPostJsonWithUrl:url :content :completionBlock :[ClientServerInteraction createHandler:completionBlock :responseClass]];
    
}

+(void)authorize:(NSString*)email :(NSString*)password :(NSString*)deviceId :(void (^)(int code, AccessToken* response, NSError* error, ServerResponseError* serverError))completionBlock {
    [ClientServerInteraction baseRequest:[NSString stringWithFormat:@"%@%@", kToken, @"authorize"] :[NSDictionary dictionaryWithObjectsAndKeys:email, @"email", password, @"password", deviceId, @"deviceId", nil] :nil :completionBlock :[AccessToken class]];
}

+(void)validateEmail:(NSString*)email :(void (^)(int code, NSNumber* response, NSError* error, ServerResponseError* serverError))completionBlock {
    [ClientServerInteraction baseRequest:[NSString stringWithFormat:@"%@%@", kAuth, @"check_existence"] :[NSDictionary dictionaryWithObjectsAndKeys:email, @"email", nil] :nil :completionBlock :[NSNumber class]];
}

+(void)registerUser:(NSString*)email :(NSString*)password :(void (^)(int code, NSNumber* response, NSError* error, ServerResponseError* serverError))completionBlock {
    [ClientServerInteraction baseRequest:[NSString stringWithFormat:@"%@%@", kAuth, @"register"] :[NSDictionary dictionaryWithObjectsAndKeys:email, @"email", password, @"password", nil] :nil :completionBlock :[NSNumber class]];
}

+(void)checkData:(NSString*)email :(NSString*)password :(void (^)(int code, NSNumber* response, NSError* error, ServerResponseError* serverError))completionBlock {
    [ClientServerInteraction baseRequest:[NSString stringWithFormat:@"%@%@", kAuth, @"register"] :[NSDictionary dictionaryWithObjectsAndKeys:email, @"email", password, @"password", nil] :nil :completionBlock :[NSNumber class]];
}

+(void)getInfo:(NSString*)token :(void (^)(int code, User* response, NSError* error, ServerResponseError* serverError))completionBlock {
    [ClientServerInteraction baseRequest:[NSString stringWithFormat:@"%@%@", kAuth, @"info"] :[NSDictionary dictionaryWithObjectsAndKeys:token, @"token", nil] :nil :completionBlock :[User class]];
}

+(void)updateInfo:(User*)user :(NSString*)token :(void (^)(int code, NSNumber* response, NSError* error, ServerResponseError* serverError))completionBlock {
    [ClientServerInteraction baseRequest:[NSString stringWithFormat:@"%@%@", kAuth, @"update_info"] :[NSDictionary dictionaryWithObjectsAndKeys:token, @"token", nil] :user :completionBlock :[NSNumber class]];
}

+(void)upload:(NSString*)email withPassword:(NSString*)password rates:(NSArray*) rates start:(long long) start completion:(void (^)(NSNumber* response, NSError* error))completionBlock {
 
}

+(void)sync:(NSString*)email withPassword:(NSString*)password rates:(NSArray*) rates start:(long long) start completion:(void (^)(NSNumber* response, NSError* error))completionBlock {

}




// private

+(void)httpPostJsonWithUrl:(NSString*)url :(NSString*)content :(void (^)(int code, id response, NSError* error, ServerResponseError* serverError))completionBlock
                          :(void (^)(NSURLResponse *urlResponse,NSData *responseData, NSError *error))handler {
    NSMutableURLRequest *request =
    [[NSMutableURLRequest alloc] initWithURL:
     [NSURL URLWithString:url]];
    
    [request setHTTPMethod:@"POST"];
    [request setValue:@"application/x-www-form-urlencoded" forHTTPHeaderField:@"Content-Type"];
    
    
    [request setHTTPBody:[content
                          dataUsingEncoding:NSUTF8StringEncoding]];
    
    [NSURLConnection sendAsynchronousRequest:request queue:[NSOperationQueue mainQueue] completionHandler:handler];
}

+(NSString*)prepareQueryString:(NSDictionary*)params {
    if (params == nil)
        return @"";
    NSString* result = @"";    
    for (id key in params) {
        id value = [params objectForKey:key];
        result = [NSString stringWithFormat:[result isEqual: @""] ? @"%@%@=%@" : @"%@&%@=%@", result, key, value];
    }
    return result;
}


+(void (^)(NSURLResponse *urlResponse,NSData *responseData, NSError *error))createHandler :(void (^)(int code, id response, NSError* error, ServerResponseError* serverError))completionBlock :(Class)responseClass {
    return ^( NSURLResponse *urlResponse,NSData *responseData, NSError *error){
        if (error != nil) {
            completionBlock(CLIENT_ERROR, nil, error, nil);
            return;
        }
        NSDictionary* json = [NSJSONSerialization
                              JSONObjectWithData:responseData
                              
                              options:kNilOptions
                              error:&error];
        if (error != nil) {
            completionBlock(CLIENT_ERROR, nil, error, nil);
            return;
        }
        [ClientServerInteraction deserializeResponse:json :responseClass :completionBlock];
    };
}

+(void)deserializeResponse:(NSDictionary*)json :(Class)responseClass :(void (^)(int code, id response, NSError* error, ServerResponseError* serverError))completionBlock {
    int responseCode = [[json objectForKey:@"responseCode"] intValue];
    if (responseCode == RESPONSE_ERROR) {
        completionBlock(SERVER_ERROR, nil, nil, [ClientServerInteraction deserialize:[json objectForKey:@"error"] :[ServerResponseError class]]);
        return;
    }
    if (responseCode == OK) {
        completionBlock(OK, [ClientServerInteraction deserialize:[json objectForKey:@"data"] :responseClass], nil, nil);
        return;
    }
    
}

+(id)deserialize:(id)obj :(Class)class {
    if (class == [User class])
        return [ClientServerInteraction deserializeUser:obj];
    if (class == [NSNumber class])
        return [ClientServerInteraction deserializeBool:obj];
    if (class == [AccessToken class])
        return [ClientServerInteraction deserializeAccessToken:obj];
    if (class == [ServerResponseError class])
        return [ClientServerInteraction deserializeServerError:obj];
    return nil;    
}

+(NSString*)serialize:(id)obj {
    if ([obj class] == [User class])
        return [ClientServerInteraction serializeUser:obj];
    return nil;
}

+(NSString*)serializeUser:(User*) user {
    NSArray* objects = [NSArray arrayWithObjects:user.about, user.birthDate, user.description, user.department, user.diagnosis, user.email, user.firstName, user.lastName, user.height, user.password, user.sex, user.statusMessage, user.weight, nil];
                        
    NSArray* keys = [NSArray arrayWithObjects:@"about", @"birthDate", @"description", @"department", @"diagnosis", @"email", @"firstName", @"lastName", @"height", @"password", @"sex", @"statusMessage", @"weight", nil];
    NSData* data = [NSJSONSerialization dataWithJSONObject:[NSDictionary dictionaryWithObjects:objects forKeys:keys] options:NSJSONWritingPrettyPrinted error:nil];
    NSString* jsonString = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
    
    return jsonString;
}

+(User*)deserializeUser:(NSDictionary*) dict {
    User* user = [User alloc];
    user.about = [dict objectForKey:@"about"];
    user.birthDate = [dict objectForKey:@"birthDate"];
    user.description = [dict objectForKey:@"description"];
    user.department = [dict objectForKey:@"department"];
    user.diagnosis = [dict objectForKey:@"diagnosis"];
    user.email = [dict objectForKey:@"email"];
    user.firstName = [dict objectForKey:@"firstName"];
    user.lastName = [dict objectForKey:@"lastName"];
    user.height = [dict objectForKey:@"height"];
    user.password = [dict objectForKey:@"password"];
    user.sex = [dict objectForKey:@"sex"];
    user.statusMessage = [dict objectForKey:@"statusMessage"];
    user.weight = [dict objectForKey:@"weight"];
    user.userId = [dict objectForKey:@"userId"];
    return user;
}

+(NSString*)serializeSession:(NSString*)email withPassword:(NSString*)password rates:(NSArray*) rates start:(long long) start create:(int)create {
    NSArray* objects = [NSArray arrayWithObjects:email, password, rates, [NSString stringWithFormat:@"%lld", start], [NSNumber numberWithInt:create], nil];
    
    NSArray* keys = [NSArray arrayWithObjects:@"email", @"password", @"rates", @"start", @"create", nil];
    NSData* data = [NSJSONSerialization dataWithJSONObject:[NSDictionary dictionaryWithObjects:objects forKeys:keys] options:NSJSONWritingPrettyPrinted error:nil];
    NSString* json = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
    return json;
}

+(NSNumber*)deserializeBool:(NSString*)value {
    NSNumberFormatter * formatter = [[NSNumberFormatter alloc] init];
    [formatter setNumberStyle:NSNumberFormatterDecimalStyle];
    NSNumber * boolNumber = [formatter numberFromString:value];
    return boolNumber;
}

+(AccessToken*)deserializeAccessToken:(NSDictionary*)dict {
    AccessToken* accessToken = [AccessToken alloc];
    accessToken.token = [dict objectForKey:@"token"];
    accessToken.expiredDate = [dict objectForKey:@"expiredDate"];
    return accessToken;
}

+(ServerResponseError*)deserializeServerError:(NSDictionary*)dict {
    ServerResponseError* error = [ServerResponseError alloc];
    error.message = [dict objectForKey:@"message"];
    error.errorCode = [[dict objectForKey:@"code"] intValue];
    return error;
}
                    







@end
