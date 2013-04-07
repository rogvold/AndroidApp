//
//  HttpRequest.m
//  ClientServerInteraction.iOS
//
//  Created by Yuriy Pogrebnyak on 07.04.13.
//  Copyright (c) 2013 Alexander O. Taraymovich. All rights reserved.
//

#import "HttpRequest.h"
#import "ServerResponseError.h"
#import "Serializer_FrameworkOnly.h"

@implementation HttpRequest

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
        [Serializer deserializeResponse:json :responseClass :completionBlock];
    };
}

@end
