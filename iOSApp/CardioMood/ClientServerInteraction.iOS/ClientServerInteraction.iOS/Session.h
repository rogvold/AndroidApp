//
//  Session.h
//  ClientServerInteraction.iOS
//
//  Created by Yuriy Pogrebnyak on 06.04.13.
//  Copyright (c) 2013 Alexander O. Taraymovich. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface Session : NSObject

@property(nonatomic, retain) NSNumber* sessionId;

@property(nonatomic, retain) NSNumber* start;

@property(nonatomic, retain) NSNumber* end;

@end
