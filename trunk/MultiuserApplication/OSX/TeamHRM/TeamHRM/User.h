//
//  User.h
//  TeamHRM
//
//  Created by Alexander O. Taraymovich on 17.12.12.
//  Copyright (c) 2012 Alexander O. Taraymovich. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <IOBluetooth/IOBluetooth.h>

@interface User : NSObject

@property (strong) NSString *password;
@property (strong) NSString *username;

@property NSInteger *userId;
@property NSInteger *currentSessionId;
@property (strong) CBPeripheral *connectedPeripheral;
@property (strong) NSString *deviceName;
@property (strong) NSString *deviceId;
@property (strong) NSString *heartRate;
// RR intervals
@property (strong) NSMutableArray *RRs;
// Queue for intervals waiting to be sent
@property (strong) NSMutableArray *RRsToSend;
// Start time for each portion of intervals sending to server
@property (strong) NSDate *startTime;
@property (assign) int create;

@property (strong) NSString *name;
@property (strong) NSString *surname;
@property (strong) NSString *weight;
@property (strong) NSString *height;
@property BOOL isConnected;

@end
