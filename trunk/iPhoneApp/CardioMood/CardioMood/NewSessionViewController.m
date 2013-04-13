//
//  NewSessionViewController.m
//  CardioMood
//
//  Created by Alexander O. Taraymovich on 07.04.13.
//  Copyright (c) 2013 Alexander O. Taraymovich. All rights reserved.
//

#import "NewSessionViewController.h"
#import <CoreBluetooth/CoreBluetooth.h>
#import <ClientServerInteraction.h>
#import "KeychainItemWrapper.h"
#import "LocalSession.h"
#import "AppDelegate.h"
#import "LocalUser.h"

@interface NewSessionViewController () <CBPeripheralDelegate>

@property (nonatomic) BOOL localSave;

@end

@implementation NewSessionViewController

@synthesize username;
@synthesize password;
@synthesize token;
@synthesize keychainItem;
@synthesize startTime;
@synthesize intervals;
@synthesize intervalsToSend;
@synthesize connectedPeripheral;
@synthesize create;

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
	[connectedPeripheral setDelegate:self];
    intervals = [NSMutableArray array];
    intervalsToSend = [NSMutableArray array];
    create = [NSNumber numberWithInt:1];
    startTime = [NSDate date];
    self.localSave = NO;
    AppDelegate *appDelegate = (AppDelegate *)[[UIApplication sharedApplication] delegate];
    self.managedObjectContext = appDelegate.managedObjectContext;
    [self.connectedDeviceLabel setText:[connectedPeripheral name]];
    keychainItem = [[KeychainItemWrapper alloc] initWithIdentifier:@"CardioMood" accessGroup:nil];
    password = [keychainItem objectForKey:CFBridgingRelease(kSecValueData)];
    username = [keychainItem objectForKey:CFBridgingRelease(kSecAttrAccount)];
    token = [keychainItem objectForKey:CFBridgingRelease(kSecAttrLabel)];
}

-(void) viewWillDisappear:(BOOL)animated {
    if ([self.navigationController.viewControllers indexOfObject:self]==NSNotFound) {
        [connectedPeripheral setDelegate:nil];
        [self saveIntervalsLocally:intervalsToSend];
        [intervalsToSend removeAllObjects];
    }
    [super viewWillDisappear:animated];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

#pragma mark - CBPeripheral delegate methods

// Update UI with heart rate data received from device
- (void) updateWithHRMData:(NSData *)data
{
    const uint8_t *reportData = [data bytes];
    uint16_t rate = 0;
    // n of byte containing rr
    int rrByte = 2;
    if ((reportData[0] & 0x01) == 0)
    {
        /* uint8 bpm */
        rate = reportData[1];
    }
    else
    {
        /* uint16 bpm */
        rate = CFSwapInt16LittleToHost(*(uint16_t *)(&reportData[1]));
        rrByte++;
    }
    if ((reportData[0] & 0x04) == 1){
        // Energy field is present
        rrByte += 2;
    }
    
    if ((reportData[0] & 0x05) == 0) {
        //NSLog(@"RR intervals aren't present");
    } else {
        //NSLog(@"RR intervals are present");
        NSMutableArray* rrs = [NSMutableArray array];
        uint16_t rr = (CFSwapInt16LittleToHost(*(uint16_t *)(&reportData[rrByte])) * 1000) / 1024;
        while (rr != 0) {
            //NSLog(@"%@", [NSString stringWithFormat:@"RR: %d", rr]);
            [rrs addObject:[NSNumber numberWithInt:rr]];
            rrByte += 2;
            rr = (CFSwapInt16LittleToHost(*(uint16_t *)(&reportData[rrByte])) * 1000) / 1024;
        }
        [intervals addObjectsFromArray:rrs];
        [intervalsToSend addObjectsFromArray:rrs];
        // Send every 10 intervals to server
        if ([intervalsToSend count] >= 10) {
            [ClientServerInteraction checkIfServerIsReachable:^(bool response) {
                if (response && !self.localSave)
                {
                    [ClientServerInteraction uploadRates:intervalsToSend start:[NSNumber numberWithLongLong:(long long)[startTime timeIntervalSince1970] * 1000] create:create token:token completion:^(int code, NSNumber *response, NSError *error, ServerResponseError *serverError) {
                        if (code == 1)
                        {
                            create = [NSNumber numberWithInt:0];
                            [intervalsToSend removeAllObjects];
                            startTime = [NSDate date];
                        }
                    }];
                }
                else
                {
                    self.localSave = YES;
                }
            }];
        }
    }
    self.heartRateLabel.text = [NSString stringWithFormat:@"%d", rate];
}

// Invoked upon completion of a -[readValueForCharacteristic:] request
// or on the reception of a notification/indication.
- (void) peripheral:(CBPeripheral *)aPeripheral
didUpdateValueForCharacteristic:(CBCharacteristic *)characteristic
              error:(NSError *)error
{
    
    // Updated value for heart rate measurement received
    if ([characteristic.UUID isEqual:[CBUUID UUIDWithString:@"2A37"]]) {
        if(characteristic.value || !error) {
            // Update UI with heart rate data
            [self updateWithHRMData:characteristic.value];
        }
    }
}

-(void)saveIntervalsLocally:(NSMutableArray *)rates
{
    LocalSession *session = (LocalSession *)[NSEntityDescription insertNewObjectForEntityForName:@"LocalSession" inManagedObjectContext:self.managedObjectContext];
    [session setStartTime:startTime];
    LocalUser *user = (LocalUser *)[NSEntityDescription insertNewObjectForEntityForName:@"LocalUser" inManagedObjectContext:self.managedObjectContext];
    
    NSString *userId = [keychainItem objectForKey:CFBridgingRelease(kSecAttrComment)];
    [user setUserId:[NSNumber numberWithInt:[userId intValue]]];
    NSError *error;
    @synchronized(self.managedObjectContext)
    {
        if(![self.managedObjectContext save:&error]){
            //This is a serious error saying the record
            //could not be saved. Advise the user to
            //try again or restart the application.
        }
    }
    [session setUser:user];
    [session setRates:[rates componentsJoinedByString:@","]];
    @synchronized(self.managedObjectContext)
    {
        if(![self.managedObjectContext save:&error]){
            //This is a serious error saying the record
            //could not be saved. Advise the user to
            //try again or restart the application.
        }
    }
}

@end
