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

@interface DeviceViewController () <CBCentralManagerDelegate, CBPeripheralDelegate, UITableViewDataSource, UITableViewDelegate>

//@property (strong) NSMutableArray *heartRate;
@end

@implementation DeviceViewController
@synthesize heartRateMonitors;
@synthesize heartRateLabel;
@synthesize connectedMonitor;
@synthesize manager;
@synthesize sensorsTable;
@synthesize bpm;
@synthesize currentlyConnectedPeripheral;
@synthesize lastConnectedPeripheral;

@synthesize RRs;
@synthesize RRsToSend;
@synthesize startTime;
@synthesize create;
@synthesize login;
@synthesize password;

- (void)loadView
{
    [super loadView];
    heartRateMonitors = [NSMutableArray array];
    RRs = [NSMutableArray array];
    RRsToSend = [NSMutableArray array];
    startTime = nil;
    create = 1;
    manager = [[CBCentralManager alloc] initWithDelegate:self queue:nil];
    [self startScan];
}

- (void) viewDidUnload
{
    [self setCurrentlyConnectedPeripheral:nil];
    [self setSensorsTable:nil];
    
    [super viewDidUnload];
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
    [manager scanForPeripheralsWithServices:[NSArray arrayWithObject:[CBUUID UUIDWithString:@"180D"]]
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
    NSMutableArray *peripherals = [self mutableArrayValueForKey:@"CardioMoods"];
    if( ![heartRateMonitors containsObject:aPeripheral] && aPeripheral.name != nil)
        [peripherals addObject:aPeripheral];
    
    if([lastConnectedPeripheral UUID] != nil)
    {
        [manager retrievePeripherals:[NSArray arrayWithObject:(id)lastConnectedPeripheral.UUID]];
    }
    
    [sensorsTable reloadData];
}

#pragma mark TableView delegate methods
- (void) tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *) indexPath {
    CBPeripheral *device = [heartRateMonitors objectAtIndex:[indexPath row]];
    if (![device isConnected]) {
        if ([currentlyConnectedPeripheral isConnected]) {
            [manager cancelPeripheralConnection:currentlyConnectedPeripheral];
        }
        currentlyConnectedPeripheral = device;
        [manager connectPeripheral:currentlyConnectedPeripheral
                                options:[NSDictionary dictionaryWithObject:
                                         [NSNumber numberWithBool:YES]
                                                                    forKey:
                                         CBConnectPeripheralOptionNotifyOnDisconnectionKey]];
        connectedMonitor.text = [currentlyConnectedPeripheral name];
        connectedMonitor.enabled = TRUE;
        heartRateLabel.enabled = TRUE;
        bpm.enabled = TRUE;
    }
    else {
        [manager cancelPeripheralConnection:currentlyConnectedPeripheral];
        connectedMonitor.text = @"No Connected Device";
        connectedMonitor.enabled = FALSE;
        heartRateLabel.text = @"0";
        heartRateLabel.enabled = FALSE;
        bpm.enabled = FALSE;
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
    
	device = [heartRateMonitors objectAtIndex:row];
    
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
        [manager connectPeripheral:[peripherals objectAtIndex:0] options:[NSDictionary dictionaryWithObject:[NSNumber numberWithBool:YES] forKey:CBConnectPeripheralOptionNotifyOnDisconnectionKey]];
        connectedMonitor.text = [[peripherals objectAtIndex:0] name];
        connectedMonitor.enabled = TRUE;
        heartRateLabel.enabled = TRUE;
        bpm.enabled = TRUE;
        currentlyConnectedPeripheral = [peripherals objectAtIndex:0];
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
    [self setLastConnectedPeripheral:aPeripheral];
    startTime = [NSDate date];
    [sensorsTable reloadData];
}

// Invoked when an existing connection with the peripheral is torn down.
// Reset local variables
- (void) centralManager:(CBCentralManager *)central
didDisconnectPeripheral:(CBPeripheral *)aPeripheral
                  error:(NSError *)error
{
    connectedMonitor.text = @"No Connected Device";
    connectedMonitor.enabled = FALSE;
    heartRateLabel.text = @"0";
    heartRateLabel.enabled = FALSE;
    bpm.enabled = FALSE;
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
            
            // Read body sensor location
            if ([aChar.UUID isEqual:[CBUUID UUIDWithString:@"2A38"]]) {
                [aPeripheral readValueForCharacteristic:aChar];
                NSLog(@"Found a Body Sensor Location Characteristic");
            }
            
            // Write heart rate control point
            if ([aChar.UUID isEqual:[CBUUID UUIDWithString:@"2A39"]]) {
                uint8_t val = 1;
                NSData* valData = [NSData dataWithBytes:(void*)&val length:sizeof(val)];
                [aPeripheral writeValue:valData forCharacteristic:aChar type:CBCharacteristicWriteWithResponse];
            }
        }
    }
    
    if ([service.UUID isEqual:[CBUUID UUIDWithString:CBUUIDGenericAccessProfileString]]) {
        for (CBCharacteristic *aChar in service.characteristics) {
            // Read device name
            if ([aChar.UUID isEqual:[CBUUID UUIDWithString:CBUUIDDeviceNameString]]) {
                [aPeripheral readValueForCharacteristic:aChar];
                NSLog(@"Found a Device Name Characteristic");
            }
        }
    }
    
    if ([service.UUID isEqual:[CBUUID UUIDWithString:@"180A"]]) {
        for (CBCharacteristic *aChar in service.characteristics) {
            // Read manufacturer name
            if ([aChar.UUID isEqual:[CBUUID UUIDWithString:@"2A29"]]) {
                [aPeripheral readValueForCharacteristic:aChar];
                NSLog(@"Found a Device Manufacturer Name Characteristic");
            }
        }
    }
}

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
        NSLog(@"RR intervals aren't present");
    } else {
        NSLog(@"RR intervals are present");
        NSMutableArray* rrs = [NSMutableArray array];
        uint16_t rr = (CFSwapInt16LittleToHost(*(uint16_t *)(&reportData[rrByte])) * 1000) / 1024;
        while (rr != 0) {
            NSLog(@"%@", [NSString stringWithFormat:@"RR: %d", rr]);
            [rrs addObject:[NSNumber numberWithInt:rr]];
            rrByte += 2;
            rr = (CFSwapInt16LittleToHost(*(uint16_t *)(&reportData[rrByte])) * 1000) / 1024;
        }
        [RRs addObjectsFromArray:rrs];
        [RRsToSend addObjectsFromArray:rrs];
        // Send every 10 intervals to server
        if ([RRsToSend count] >= 10) {
            [self sendRRs:RRsToSend];
            [RRsToSend removeAllObjects];
            startTime = [NSDate date];
        }
    }
    heartRateLabel.text = [NSString stringWithFormat:@"%d", rate];
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
            NSLog(@"received value: %@", characteristic.value);
            // Update UI with heart rate data
            [self updateWithHRMData:characteristic.value];
        }
    }
    // Value for body sensor location received
    else if ([characteristic.UUID isEqual:[CBUUID UUIDWithString:@"2A38"]]) {
        NSData * updatedValue = characteristic.value;
        uint8_t* dataPointer = (uint8_t*)[updatedValue bytes];
        if (dataPointer) {
            uint8_t location = dataPointer[0];
            NSString*  locationString;
            switch (location) {
                case 0:
                    locationString = @"Other";
                    break;
                case 1:
                    locationString = @"Chest";
                    break;
                case 2:
                    locationString = @"Wrist";
                    break;
                case 3:
                    locationString = @"Finger";
                    break;
                case 4:
                    locationString = @"Hand";
                    break;
                case 5:
                    locationString = @"Ear Lobe";
                    break;
                case 6:
                    locationString = @"Foot";
                    break;
                default:
                    locationString = @"Reserved";
                    break;
            }
            NSLog(@"Body Sensor Location = %@ (%d)", locationString, location);
        }
    }
    // Value for device Name received
    else if ([characteristic.UUID isEqual:[CBUUID UUIDWithString:CBUUIDDeviceNameString]]) {
        NSString * deviceName = [[NSString alloc] initWithData:characteristic.value
                                                      encoding:NSUTF8StringEncoding];
        NSLog(@"Device Name = %@", deviceName);
    }
    // Value for manufacturer name received
    else if ([characteristic.UUID isEqual:[CBUUID UUIDWithString:@"2A29"]]) {
        NSString *manufacturer = [[NSString alloc] initWithData:characteristic.value
                                                       encoding:NSUTF8StringEncoding];
        NSLog(@"Manufacturer Name = %@", manufacturer);
    }
}

// Send intervals to server
- (void) sendRRs:(NSArray *)rrs
{
    NSString* jsonString = [self makeJSON:rrs];
    NSURL* url = [NSURL URLWithString:@"http://reshaka.ru:8080/BaseProjectWeb/faces/input"];
    
    
    NSString* stringToSend = [@"json=" stringByAppendingString:jsonString];
    NSData* dataToSend = [stringToSend dataUsingEncoding:NSUTF8StringEncoding];
    
    
    NSMutableURLRequest* request = [[NSMutableURLRequest alloc] initWithURL:url];
    [request setHTTPMethod:@"POST"];
    [request setValue:@"application/x-www-form-urlencoded" forHTTPHeaderField:@"Content-Type"];
    [request setHTTPBody:dataToSend];
    [NSURLConnection connectionWithRequest:request delegate:self];
}

// Make json based on array of rr intervals
-(NSString *) makeJSON:(NSArray *)rrs
{
    NSDateFormatter* dateFormatter = [[NSDateFormatter alloc] init];
    [dateFormatter setDateFormat:@"yyyy-MM-dd HH:mm:ss.SSS"];
    NSString* dateString = [dateFormatter stringFromDate:startTime];
    NSString* uuid = (__bridge NSString *)CFUUIDCreateString(NULL, [currentlyConnectedPeripheral UUID]);
    NSArray* objects = [NSArray arrayWithObjects:dateString, uuid, [currentlyConnectedPeripheral name], rrs, login, password, create == 0 ? @"0" : @"1", nil];
    NSArray* keys = [NSArray arrayWithObjects:@"start", @"device_id", @"device_name", @"rates", @"email", @"password", @"create", nil];
    create = 0;
    NSDictionary* JSONDictionary = [NSDictionary dictionaryWithObjects:objects forKeys:keys];
    NSError* error = nil;
    NSData* jsonData = [NSJSONSerialization dataWithJSONObject:JSONDictionary options:NSJSONWritingPrettyPrinted error:&error];
    NSString* json = nil;
    if (! jsonData)
    {
        NSLog(@"Got an error: %@", error);
    }
    else
    {
        json = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
    }
    return json;
}

@end
