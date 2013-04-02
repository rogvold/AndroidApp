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
@property (nonatomic, strong) IBOutlet UILabel *heartRateLabel;
@property (nonatomic, strong) IBOutlet UILabel *connectedMonitor;
@property (nonatomic, strong) IBOutlet UITableView *sensorsTable;
@property (nonatomic, strong) IBOutlet UILabel *bpm;
@property (nonatomic, strong) CBCentralManager *manager;
@property (nonatomic, strong) CBPeripheral *currentlyConnectedPeripheral;
@property (nonatomic, strong) CBPeripheral *lastConnectedPeripheral;
@property (nonatomic, strong) NSString *login;
@property (nonatomic, strong) NSString *password;

// RR intervals
@property (retain) NSMutableArray *RRs;
// Queue for intervals waiting to be sent
@property (retain) NSMutableArray *RRsToSend;
// Start time for each portion of intervals sending to server
@property (retain) NSDate *startTime;
@property (assign) int create;

- (void) sendRRs:(NSArray *)rrs;

@end
