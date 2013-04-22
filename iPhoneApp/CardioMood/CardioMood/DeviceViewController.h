//
//  DeviceViewController.h
//  CardioMood
//
//  Created by Alexander O. Taraymovich on 07.10.12.
//  Copyright (c) 2012 Alexander O. Taraymovich. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <CoreBluetooth/CoreBluetooth.h>

@interface DeviceViewController : UIViewController
@property (nonatomic, strong) NSMutableArray *heartRateMonitors;
@property (nonatomic, strong) IBOutlet UITableView *sensorsTable;
@property (nonatomic, strong) CBCentralManager *manager;
@property (nonatomic, strong) CBPeripheral *currentlyConnectedPeripheral;
@property (nonatomic, strong) CBPeripheral *lastConnectedPeripheral;
@property (nonatomic, strong) IBOutlet UIProgressView *batteryLevel;
@property (nonatomic, strong) IBOutlet UILabel *batteryLevelLabel;
@property (nonatomic, strong) IBOutlet UIBarButtonItem *startButton;

@end
