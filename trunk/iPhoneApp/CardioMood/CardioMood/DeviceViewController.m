//
//  DeviceViewController.m
//  CardioMood
//
//  Created by Alexander O. Taraymovich on 07.10.12.
//  Copyright (c) 2012 Alexander O. Taraymovich. All rights reserved.
//

#import "DeviceViewController.h"
#import <CoreBluetooth/CoreBluetooth.h>
#import <CoreFoundation/CoreFoundation.h>
#include <mach/mach.h>
#import "KeychainItemWrapper.h"
#import <ClientServerInteraction.h>
#import "NewSessionViewController.h"

@interface DeviceViewController () <CBCentralManagerDelegate, CBPeripheralDelegate, UITableViewDataSource, UITableViewDelegate>

//@property (strong) NSMutableArray *heartRate;
@end

@implementation DeviceViewController

@synthesize heartRateMonitors;
@synthesize manager;
@synthesize sensorsTable;
@synthesize currentlyConnectedPeripheral;
@synthesize lastConnectedPeripheral;

- (void)viewDidLoad
{
    [super viewDidLoad];
    heartRateMonitors = [NSMutableArray array];
    manager = [[CBCentralManager alloc] initWithDelegate:self queue:nil];
    [self startScan];
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation
{
    return (interfaceOrientation == UIInterfaceOrientationPortrait);
}

#pragma mark - Start/Stop Scan methods

// Use CBCentralManager to check whether the current platform/hardware supports Bluetooth LE.
- (BOOL) isLECapableHardware
{
    NSString * state = nil;
    switch ([manager state]) {
        case CBCentralManagerStateUnsupported:
            state = @"The platform/hardware doesn't support Bluetooth Low Energy.";
            break;
        case CBCentralManagerStateUnauthorized:
            state = @"The app is not authorized to use Bluetooth Low Energy.";
            break;
        case CBCentralManagerStatePoweredOff:
            state = @"Bluetooth is currently powered off.";
            break;
        case CBCentralManagerStatePoweredOn:
            return TRUE;
        case CBCentralManagerStateUnknown:
        default:
            return FALSE;
    }
    NSLog(@"Central manager state: %@", state);
    return FALSE;
}

// Request CBCentralManager to scan for heart rate peripherals using service UUID 0x180D
- (void) startScan
{
    [manager scanForPeripheralsWithServices:@[[CBUUID UUIDWithString:@"180D"]]
                                         options:nil];
}

// Request CBCentralManager to stop scanning for heart rate peripherals
- (void) stopScan
{
    [manager stopScan];
}

#pragma mark - CBCentralManager delegate methods

// Invoked when the central manager's state is updated.
- (void) centralManagerDidUpdateState:(CBCentralManager *)central
{
    [self isLECapableHardware];
}

// Invoked when the central discovers heart rate peripheral while scanning.
- (void) centralManager:(CBCentralManager *)central
  didDiscoverPeripheral:(CBPeripheral *)aPeripheral
      advertisementData:(NSDictionary *)advertisementData
                   RSSI:(NSNumber *)RSSI
{
    NSMutableArray *peripherals = [self mutableArrayValueForKey:@"heartRateMonitors"];
    if( ![heartRateMonitors containsObject:aPeripheral] && aPeripheral.name != nil)
        [peripherals addObject:aPeripheral];
    
    if([lastConnectedPeripheral UUID] != nil)
    {
        [manager retrievePeripherals:@[(id)lastConnectedPeripheral.UUID]];
    }
    
    [sensorsTable reloadData];
}

#pragma mark TableView delegate methods
- (void) tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *) indexPath {
    CBPeripheral *device = heartRateMonitors[[indexPath row]];
    if (![device isConnected]) {
        if ([currentlyConnectedPeripheral isConnected]) {
            [manager cancelPeripheralConnection:currentlyConnectedPeripheral];
        }
        currentlyConnectedPeripheral = device;
        [manager connectPeripheral:currentlyConnectedPeripheral
                                options:@{CBConnectPeripheralOptionNotifyOnDisconnectionKey: @YES}];
    }
    else {
        [manager cancelPeripheralConnection:currentlyConnectedPeripheral];
        currentlyConnectedPeripheral = nil;
        [sensorsTable reloadData];
    }
    [sensorsTable reloadData];
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
	
	UITableViewCell	*cell;
	CBPeripheral	*device;
	NSInteger		row	= [indexPath row];
    static NSString *cellID = @"DeviceList";
    
	cell = [tableView dequeueReusableCellWithIdentifier:cellID];
	if (!cell)
		cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleValue1 reuseIdentifier:cellID];
    
	device = heartRateMonitors[row];
    
    if ([[device name] length])
        [[cell textLabel] setText:[device name]];
    else
        [[cell textLabel] setText:@"Peripheral"];
    
    [[cell detailTextLabel] setText: [device isConnected] ? @"Connected" : @"Not connected"];
    
	return cell;
}

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
    return 1;
}
- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    return [heartRateMonitors count];
}

// Invoked when the central manager retrieves the list of known peripherals.
// Automatically connect to first known peripheral
- (void)centralManager:(CBCentralManager *)central didRetrievePeripherals:(NSArray *)peripherals
{
    NSLog(@"Retrieved peripheral: %u - %@", [peripherals count], peripherals);
    [self stopScan];
    if([peripherals count] >= 1) {
        [manager connectPeripheral:peripherals[0] options:@{CBConnectPeripheralOptionNotifyOnDisconnectionKey: @YES}];
        currentlyConnectedPeripheral = peripherals[0];
        [sensorsTable reloadData];
    }
}

// Invoked when a connection is succesfully created with the peripheral.
// Discover available services on the peripheral
- (void) centralManager:(CBCentralManager *)central didConnectPeripheral:(CBPeripheral *)aPeripheral
{
    NSLog(@"connected");
    [aPeripheral setDelegate:self];
    [aPeripheral discoverServices:nil];
    [self.startButton setEnabled:YES];
    [self setLastConnectedPeripheral:aPeripheral];
    [sensorsTable reloadData];
}

// Invoked when an existing connection with the peripheral is torn down.
// Reset local variables
- (void) centralManager:(CBCentralManager *)central
didDisconnectPeripheral:(CBPeripheral *)aPeripheral
                  error:(NSError *)error
{
    currentlyConnectedPeripheral = nil;
    [sensorsTable reloadData];
    [self startScan];
}

// Invoked when the central manager fails to create a connection with the peripheral.
- (void) centralManager:(CBCentralManager *)central
didFailToConnectPeripheral:(CBPeripheral *)aPeripheral
                  error:(NSError *)error
{
    NSLog(@"Fail to connect to peripheral: %@ with error = %@", aPeripheral, [error localizedDescription]);
}

#pragma mark - CBPeripheral delegate methods

// Invoked upon completion of a -[discoverServices:] request.
// Discover available characteristics on interested services
- (void) peripheral:(CBPeripheral *)aPeripheral didDiscoverServices:(NSError *)error
{
    for (CBService *aService in aPeripheral.services) {
        NSLog(@"Service found with UUID: %@", aService.UUID);

        /* Heart Rate Service */
        if ([aService.UUID isEqual:[CBUUID UUIDWithString:@"180D"]]) {
            [aPeripheral discoverCharacteristics:nil forService:aService];
        }

        /* Device Information Service */
        if ([aService.UUID isEqual:[CBUUID UUIDWithString:@"180A"]]) {
            [aPeripheral discoverCharacteristics:nil forService:aService];
        }
        
        /* Battery Service */
        if ([aService.UUID isEqual:[CBUUID UUIDWithString:@"180F"]])
        {
            [aPeripheral discoverCharacteristics:nil forService:aService];
        }

        /* GAP (Generic Access Profile) for Device Name */
        if ([aService.UUID isEqual:[CBUUID UUIDWithString:CBUUIDGenericAccessProfileString]]) {
            [aPeripheral discoverCharacteristics:nil forService:aService];
        }
    }
}

// Invoked upon completion of a -[discoverCharacteristics:forService:] request.
// Perform appropriate operations on interested characteristics
- (void) peripheral:(CBPeripheral *)aPeripheral
didDiscoverCharacteristicsForService:(CBService *)service
              error:(NSError *)error
{
    if ([service.UUID isEqual:[CBUUID UUIDWithString:@"180D"]]) {
        for (CBCharacteristic *aChar in service.characteristics) {
            //NSLog(@"Characteristic 1: %@", aChar.uUID);
            // Set notification on heart rate measurement
            if ([aChar.UUID isEqual:[CBUUID UUIDWithString:@"2A37"]]) {
                [aPeripheral setNotifyValue:YES forCharacteristic:aChar];
                NSLog(@"Found a Heart Rate Measurement Characteristic");
            }

            // Write heart rate control point
            if ([aChar.UUID isEqual:[CBUUID UUIDWithString:@"2A39"]]) {
                uint8_t val = 1;
                NSData* valData = [NSData dataWithBytes:(void*)&val length:sizeof(val)];
                [aPeripheral writeValue:valData forCharacteristic:aChar type:CBCharacteristicWriteWithResponse];
            }
        }
    }
    
    if ([service.UUID isEqual:[CBUUID UUIDWithString:@"180F"]])
    {
        for (CBCharacteristic *aChar in service.characteristics)
        {
            /* Set notification on battery level measurement */
            if ([aChar.UUID isEqual:[CBUUID UUIDWithString:@"2A19"]])
            {
                [aPeripheral readValueForCharacteristic:aChar];
                NSLog(@"Found a Battery level Characteristic");
            }
        }
    }
}

// Invoked upon completion of a -[readValueForCharacteristic:] request
// or on the reception of a notification/indication.
- (void) peripheral:(CBPeripheral *)aPeripheral
didUpdateValueForCharacteristic:(CBCharacteristic *)characteristic
              error:(NSError *)error
{
    if ([characteristic.UUID isEqual:[CBUUID UUIDWithString:@"2A19"]])
    {
        if( (characteristic.value)  || !error )
        {
            /* Update UI with heart rate data */
            //[self updateHRMData:aPeripheral withData:characteristic.value];
            const uint8_t *reportData = [characteristic.value bytes];
            int level = reportData[0];
            self.batteryLevel.hidden = false;
            self.batteryLevelLabel.hidden = false;
            [self.batteryLevel setProgress:level / (double) 100];
            NSLog(@"Battery level: %d", level);
        }
    }
}

-(void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender{
    if([segue.identifier isEqualToString:@"startSessionSegue"]){
        NewSessionViewController *detailViewController = (NewSessionViewController *)segue.destinationViewController;
        detailViewController.connectedPeripheral = self.currentlyConnectedPeripheral;
        detailViewController.manager = manager;
    }
}

@end
