//
//  HttpRequest_HttpRequest_FrameworkOnly.h
//  ClientServerInteraction.iOS
//
//  Created by Yuriy Pogrebnyak on 07.04.13.
//  Copyright (c) 2013 Alexander O. Taraymovich. All rights reserved.
//

#import "HttpRequest.h"

@interface HttpRequest ()

+(void)httpPostJsonWithUrl:(NSString*)url :(NSString*)content :(void (^)(int code, id response, NSError* error, ServerResponseError* serverError))completionBlock
                          :(void (^)(NSURLResponse *urlResponse,NSData *responseData, NSError *error))handler;

+(void (^)(NSURLResponse *urlResponse,NSData *responseData, NSError *error))createHandler :(void (^)(int code, id response, NSError* error, ServerResponseError* serverError))completionBlock :(Class)responseClass;

@end
