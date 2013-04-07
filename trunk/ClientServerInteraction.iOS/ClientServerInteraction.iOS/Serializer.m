//
//  Serializer.m
//  ClientServerInteraction.iOS
//
//  Created by Yuriy Pogrebnyak on 07.04.13.
//  Copyright (c) 2013 Alexander O. Taraymovich. All rights reserved.
//

#import "Serializer_FrameworkOnly.h"

@implementation Serializer

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

+(void)deserializeResponse:(NSDictionary*)json :(Class)responseClass :(void (^)(int code, id response, NSError* error, ServerResponseError* serverError))completionBlock {
    int responseCode = [[json objectForKey:@"responseCode"] intValue];
    if (responseCode == RESPONSE_ERROR) {
        completionBlock(SERVER_ERROR, nil, nil, [Serializer deserialize:[json objectForKey:@"error"] :[ServerResponseError class]]);
        return;
    }
    if (responseCode == OK) {
        completionBlock(OK, [Serializer deserialize:[json objectForKey:@"data"] :responseClass], nil, nil);
        return;
    }
    
}

+(id)deserialize:(id)obj :(Class)class {
    if (class == [User class])
        return [Serializer deserializeUser:obj];
    if (class == [NSNumber class])
        return [Serializer deserializeBool:obj];
    if (class == [AccessToken class])
        return [Serializer deserializeAccessToken:obj];
    if (class == [ServerResponseError class])
        return [Serializer deserializeServerError:obj];
    if (class == [Session class])
        return [Serializer deserializeSessionsArray:obj];
    if (class == [NSArray class])
        return obj;
    if (class == [NSDictionary class])
        return [Serializer deserializeTension:obj];
    return nil;
}

+(NSString*)serialize:(id)obj {
    if ([obj isKindOfClass:[User class]])
        return [Serializer serializeUser:obj];
    if ([obj isKindOfClass: [NSDictionary class]])
        return [Serializer serializeDictionary:obj];
    return nil;
}

+(NSString*)serializeUser:(User*) user {
    NSArray* objects = [NSArray arrayWithObjects:user.about, user.birthDate, user.description, user.department, user.diagnosis, user.email, user.firstName, user.lastName, user.height, user.password, user.sex, user.statusMessage, user.weight, nil];
    
    NSArray* keys = [NSArray arrayWithObjects:@"about", @"birthDate", @"description", @"department", @"diagnosis", @"email", @"firstName", @"lastName", @"height", @"password", @"sex", @"statusMessage", @"weight", nil];
    NSData* data = [NSJSONSerialization dataWithJSONObject:[NSDictionary dictionaryWithObjects:objects forKeys:keys] options:NSJSONWritingPrettyPrinted error:nil];
    NSString* jsonString = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
    
    return jsonString;
}

+(NSString*)serializeDictionary:(NSDictionary*)dict {
    NSData* data = [NSJSONSerialization dataWithJSONObject:dict options:NSJSONWritingPrettyPrinted error:nil];
    NSString* jsonString = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
    return jsonString;
}

+(NSDictionary*)deserializeTension:(NSArray*)array {
    NSMutableDictionary* tension = [[NSMutableDictionary alloc] init];
    for (id internalArray in array) {
        [tension setObject:internalArray[1] forKey:internalArray[0]];
    }
    return tension;
}

+(NSArray*)deserializeRates:(NSArray*)rates {
    return rates;
}

+(NSArray*)deserializeSessionsArray:(NSArray*) array {
    NSMutableArray* sessions = [[NSMutableArray alloc] init];
    for (id obj in array) {
        [sessions addObject:[Serializer deserializeSession:obj]];
    }
    return sessions;
}

+(Session*)deserializeSession:(NSDictionary*)dict
{
    Session* session = [Session alloc];
    session.sessionId = dict[@"id"];
    session.start = dict[@"start"];
    session.end = dict[@"end"];
    return session;
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
