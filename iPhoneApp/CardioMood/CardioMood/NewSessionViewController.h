//
//  NewSessionViewController.h
//  CardioMood
//
//  Created by Alexander O. Taraymovich on 07.04.13.
//  Copyright (c) 2013 Alexander O. Taraymovich. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <CoreBluetooth/CoreBluetooth.h>
#import "KeychainItemWrapper.h"

@interface NewSessionViewController : UIViewController

@property (nonatomic, strong) CBPeripheral *connectedPeripheral;
@property (nonatomic, strong) CBCentralManager *manager;
@property (nonatomic, strong) NSMutableArray *intervals;
@property (nonatomic, strong) NSMutableArray *intervalsToSend;
@property (nonatomic, strong) NSDate *startTime;
@property (nonatomic, strong) NSString *username;
@property (nonatomic, strong) NSString *password;
@property (nonatomic, strong) NSString *token;
@property (nonatomic, strong) KeychainItemWrapper *keychainItem;
@property (nonatomic, strong) IBOutlet UILabel *heartRateLabel;
@property (nonatomic, strong) IBOutlet UILabel *connectedDeviceLabel;
@property (nonatomic, strong) NSNumber *create;
@property (nonatomic, strong) NSManagedObjectContext *managedObjectContext;

@end
