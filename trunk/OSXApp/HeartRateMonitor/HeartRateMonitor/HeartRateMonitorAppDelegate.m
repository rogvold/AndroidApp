/*
     File: HeartRateMonitorAppDelegate.m
 Abstract: Implementatin of Heart Rate Monitor app using Bluetooth Low Energy (LE) Heart Rate Service. This app demonstrats the use of CoreBluetooth APIs for LE devices.
  Version: 1.0
 
 Disclaimer: IMPORTANT:  This Apple software is supplied to you by Apple
 Inc. ("Apple") in consideration of your agreement to the following
 terms, and your use, installation, modification or redistribution of
 this Apple software constitutes acceptance of these terms.  If you do
 not agree with these terms, please do not use, install, modify or
 redistribute this Apple software.
 
 In consideration of your agreement to abide by the following terms, and
 subject to these terms, Apple grants you a personal, non-exclusive
 license, under Apple's copyrights in this original Apple software (the
 "Apple Software"), to use, reproduce, modify and redistribute the Apple
 Software, with or without modifications, in source and/or binary forms;
 provided that if you redistribute the Apple Software in its entirety and
 without modifications, you must retain this notice and the following
 text and disclaimers in all such redistributions of the Apple Software.
 Neither the name, trademarks, service marks or logos of Apple Inc. may
 be used to endorse or promote products derived from the Apple Software
 without specific prior written permission from Apple.  Except as
 expressly stated in this notice, no other rights or licenses, express or
 implied, are granted by Apple herein, including but not limited to any
 patent rights that may be infringed by your derivative works or by other
 works in which the Apple Software may be incorporated.
 
 The Apple Software is provided by Apple on an "AS IS" basis.  APPLE
 MAKES NO WARRANTIES, EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
 THE IMPLIED WARRANTIES OF NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS
 FOR A PARTICULAR PURPOSE, REGARDING THE APPLE SOFTWARE OR ITS USE AND
 OPERATION ALONE OR IN COMBINATION WITH YOUR PRODUCTS.
 
 IN NO EVENT SHALL APPLE BE LIABLE FOR ANY SPECIAL, INDIRECT, INCIDENTAL
 OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) ARISING IN ANY WAY OUT OF THE USE, REPRODUCTION,
 MODIFICATION AND/OR DISTRIBUTION OF THE APPLE SOFTWARE, HOWEVER CAUSED
 AND WHETHER UNDER THEORY OF CONTRACT, TORT (INCLUDING NEGLIGENCE),
 STRICT LIABILITY OR OTHERWISE, EVEN IF APPLE HAS BEEN ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 
 Copyright (C) 2011 Apple Inc. All Rights Reserved.
 
 */

#import "HeartRateMonitorAppDelegate.h"
#import <QuartzCore/QuartzCore.h>
#import "JSONKit.h"

@implementation HeartRateMonitorAppDelegate

@synthesize window;
@synthesize heartRate;
@synthesize heartRateMonitors;
@synthesize arrayController;
@synthesize RRs;
@synthesize RRsToSend;
@synthesize startTime;
@synthesize create;

- (void)applicationDidFinishLaunching:(NSNotification *)aNotification
{
    self.heartRate = 0;
    create = 1;
    /* autoConnect = TRUE; */  /* uncomment this line if you want to automatically connect to previosly known peripheral */
    self.heartRateMonitors = [NSMutableArray array];
    
    self.RRs = [NSMutableArray array];
    self.RRsToSend = [NSMutableArray array];
    self.startTime = nil;
    self.currentlyConnectedPeripheral = nil;

    manager = [[CBCentralManager alloc] initWithDelegate:self queue:nil];
    [self startScan];
}

- (void) dealloc
{
    [self stopScan];
    
    [peripheral setDelegate:nil];
    
        
    
}

/* 
 Disconnect peripheral when application terminate 
*/
- (void) applicationWillTerminate:(NSNotification *)notification
{
    if(peripheral)
    {
        [manager cancelPeripheralConnection:peripheral];
    }
}

#pragma mark - Connect Button

/*
 This method is called when connect button pressed and it takes appropriate actions depending on device connection state
 */
- (IBAction)connectButtonPressed:(id)sender
{
    if(peripheral && ([peripheral isConnected]))
    { 
        /* Disconnect if it's already connected */
        [manager cancelPeripheralConnection:peripheral]; 
    }
    else if (peripheral)
    {
        /* Device is not connected, cancel pendig connection */
        [connectButton setTitle:@"Connect"];
        [manager cancelPeripheralConnection:peripheral];
    }
    else
    {   /* No outstanding connection, open scan sheet */
        NSIndexSet *indexes = [self.arrayController selectionIndexes];
        if ([indexes count] != 0)
        {
            NSUInteger anIndex = [indexes firstIndex];
            peripheral = (self.heartRateMonitors)[anIndex];
            
            [connectButton setTitle:@"Disconnect"];
            [manager connectPeripheral:peripheral options:nil];
        }
    }
}

- (IBAction) loginButtonPressed:(id)sender
{
    self.loginValue = [self.loginField stringValue];
    self.passwordValue = [self.passwordField stringValue];
    [self loginButton].hidden = true;
    [self loginField].hidden = true;
    [self passwordField].hidden = true;
    [self login].hidden = true;
    [self password].hidden = true;
    [self connectButton].hidden = false;
    [self heartRateLabel].hidden = false;
    [self bpmLabel].hidden = false;
    [self sensors].hidden = false;
    [self sensorsTable].hidden = false;
    NSRect frame = [self.window frame];
    frame.size.width = 535;
    frame.size.height = 211;
    frame.origin.x = 370;
    frame.origin.y = 294;
    [self.window setFrame:frame display:YES animate:YES];
    [self startScan];
}

#pragma mark - Heart Rate Data

/* 
 Update UI with heart rate data received from device
 */
- (void) updateWithHRMData:(NSData *)data
{
    const uint8_t *reportData = [data bytes];
    uint16_t bpm = 0;
    // n of byte containing rr
    int rrByte = 2;
    if ((reportData[0] & 0x01) == 0) 
    {
        /* uint8 bpm */
        bpm = reportData[1];
    } 
    else 
    {
        /* uint16 bpm */
        bpm = CFSwapInt16LittleToHost(*(uint16_t *)(&reportData[1]));
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
            self.startTime = [NSDate date];
            
        }
        
        
    }
        
    self.heartRate = bpm;
}

#pragma mark Json methods
// Send intervals to server
- (void) sendRRs:(NSArray *)rrs
{
    //NSData* jsonData = [self makeJSON:rrs];
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
    NSString* uuid = (NSString *)CFBridgingRelease(CFUUIDCreateString(NULL, [self.currentlyConnectedPeripheral UUID]));
    NSArray* objects = @[dateString, uuid, [self.currentlyConnectedPeripheral name], rrs, self.loginValue, self.passwordValue, self.create == 0 ? @"0" : @"1"];
    NSArray* keys = @[@"start", @"device_id", @"device_name", @"rates", @"id", @"password", @"create"];
    
    self.create = 0;
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

#pragma mark - Start/Stop Scan methods

/*
 Uses CBCentralManager to check whether the current platform/hardware supports Bluetooth LE. An alert is raised if Bluetooth LE is not enabled or is not supported.
 */
- (BOOL) isLECapableHardware
{
    NSString * state = nil;
    
    switch ([manager state]) 
    {
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
    
    NSAlert *alert = [[NSAlert alloc] init];
    [alert setMessageText:state];
    [alert addButtonWithTitle:@"OK"];
    [alert setIcon:[[NSImage alloc] initWithContentsOfFile:@"AppIcon"]];
    [alert beginSheetModalForWindow:[self window] modalDelegate:self didEndSelector:nil contextInfo:nil];
    return FALSE;
}

/*
 Request CBCentralManager to scan for heart rate peripherals using service UUID 0x180D
 */
- (void) startScan 
{
    [manager scanForPeripheralsWithServices:@[[CBUUID UUIDWithString:@"180D"]] options:nil];
}

/*
 Request CBCentralManager to stop scanning for heart rate peripherals
 */
- (void) stopScan 
{
    [manager stopScan];
}

#pragma mark - CBCentralManager delegate methods
/*
 Invoked whenever the central manager's state is updated.
 */
- (void) centralManagerDidUpdateState:(CBCentralManager *)central 
{
    [self isLECapableHardware];
}
    
/*
 Invoked when the central discovers heart rate peripheral while scanning.
 */
- (void) centralManager:(CBCentralManager *)central didDiscoverPeripheral:(CBPeripheral *)aPeripheral advertisementData:(NSDictionary *)advertisementData RSSI:(NSNumber *)RSSI 
{    
    NSMutableArray *peripherals = [self mutableArrayValueForKey:@"heartRateMonitors"];
    if( ![self.heartRateMonitors containsObject:aPeripheral] )
        [peripherals addObject:aPeripheral];
    
    /* Retreive already known devices */
    if(autoConnect)
    {
        [manager retrievePeripherals:@[(id)aPeripheral.UUID]];
    }
}

/*
 Invoked when the central manager retrieves the list of known peripherals.
 Automatically connect to first known peripheral
 */
- (void)centralManager:(CBCentralManager *)central didRetrievePeripherals:(NSArray *)peripherals
{
    NSLog(@"Retrieved peripheral: %lu - %@", [peripherals count], peripherals);
    
    [self stopScan];
    
    /* If there are any known devices, automatically connect to it.*/
    if([peripherals count] >=1)
    {
        peripheral = peripherals[0];
        [connectButton setTitle:@"Cancel"];
        [manager connectPeripheral:peripheral options:@{CBConnectPeripheralOptionNotifyOnDisconnectionKey: @YES}];
    }
}

/*
 Invoked whenever a connection is succesfully created with the peripheral. 
 Discover available services on the peripheral
 */
- (void) centralManager:(CBCentralManager *)central didConnectPeripheral:(CBPeripheral *)aPeripheral 
{    
    [aPeripheral setDelegate:self];
    [aPeripheral discoverServices:nil];
    [self.connectButton setTitle:@"Disconnect"];
    self.currentlyConnectedPeripheral = aPeripheral;
    self.startTime = [NSDate date];
}

/*
 Invoked whenever an existing connection with the peripheral is torn down. 
 Reset local variables
 */
- (void)centralManager:(CBCentralManager *)central didDisconnectPeripheral:(CBPeripheral *)aPeripheral error:(NSError *)error
{
    [self.connectButton setTitle:@"Connect"];
    self.heartRate = 0;
    self.currentlyConnectedPeripheral = nil;
    if( peripheral )
    {
        [peripheral setDelegate:nil];
        peripheral = nil;
    }
}

/*
 Invoked whenever the central manager fails to create a connection with the peripheral.
 */
- (void)centralManager:(CBCentralManager *)central didFailToConnectPeripheral:(CBPeripheral *)aPeripheral error:(NSError *)error
{
    NSLog(@"Fail to connect to peripheral: %@ with error = %@", aPeripheral, [error localizedDescription]);
    [connectButton setTitle:@"Connect"]; 
    if( peripheral )
    {
        [peripheral setDelegate:nil];
        peripheral = nil;
    }
}

#pragma mark - CBPeripheral delegate methods
/*
 Invoked upon completion of a -[discoverServices:] request.
 Discover available characteristics on interested services
 */
- (void) peripheral:(CBPeripheral *)aPeripheral didDiscoverServices:(NSError *)error 
{
    for (CBService *aService in aPeripheral.services) 
    {
        NSLog(@"Service found with UUID: %@", aService.UUID);
        
        /* Heart Rate Service */
        if ([aService.UUID isEqual:[CBUUID UUIDWithString:@"180D"]]) 
        {
            [aPeripheral discoverCharacteristics:nil forService:aService];
        }
        
        /* Device Information Service */
        if ([aService.UUID isEqual:[CBUUID UUIDWithString:@"180A"]]) 
        {
            [aPeripheral discoverCharacteristics:nil forService:aService];
        }
        
        /* GAP (Generic Access Profile) for Device Name */
        if ( [aService.UUID isEqual:[CBUUID UUIDWithString:CBUUIDGenericAccessProfileString]] )
        {
            [aPeripheral discoverCharacteristics:nil forService:aService];
        }
    }
}

/*
 Invoked upon completion of a -[discoverCharacteristics:forService:] request.
 Perform appropriate operations on interested characteristics
 */
- (void) peripheral:(CBPeripheral *)aPeripheral didDiscoverCharacteristicsForService:(CBService *)service error:(NSError *)error 
{    
    if ([service.UUID isEqual:[CBUUID UUIDWithString:@"180D"]]) 
    {
        for (CBCharacteristic *aChar in service.characteristics) 
        {
            /* Set notification on heart rate measurement */
            if ([aChar.UUID isEqual:[CBUUID UUIDWithString:@"2A37"]]) 
            {
                [peripheral setNotifyValue:YES forCharacteristic:aChar];
                NSLog(@"Found a Heart Rate Measurement Characteristic");
            }
            /* Read body sensor location */
            if ([aChar.UUID isEqual:[CBUUID UUIDWithString:@"2A38"]]) 
            {
                [aPeripheral readValueForCharacteristic:aChar];
                NSLog(@"Found a Body Sensor Location Characteristic");
            } 
            
            /* Write heart rate control point */
            if ([aChar.UUID isEqual:[CBUUID UUIDWithString:@"2A39"]])
            {
                uint8_t val = 1;
                NSData* valData = [NSData dataWithBytes:(void*)&val length:sizeof(val)];
                [aPeripheral writeValue:valData forCharacteristic:aChar type:CBCharacteristicWriteWithResponse];
            }
        }
    }
    
    if ( [service.UUID isEqual:[CBUUID UUIDWithString:CBUUIDGenericAccessProfileString]] )
    {
        for (CBCharacteristic *aChar in service.characteristics) 
        {
            /* Read device name */
            if ([aChar.UUID isEqual:[CBUUID UUIDWithString:CBUUIDDeviceNameString]]) 
            {
                [aPeripheral readValueForCharacteristic:aChar];
                NSLog(@"Found a Device Name Characteristic");
            }
        }
    }
    
    if ([service.UUID isEqual:[CBUUID UUIDWithString:@"180A"]]) 
    {
        for (CBCharacteristic *aChar in service.characteristics) 
        {
            /* Read manufacturer name */
            if ([aChar.UUID isEqual:[CBUUID UUIDWithString:@"2A29"]]) 
            {
                [aPeripheral readValueForCharacteristic:aChar];
                NSLog(@"Found a Device Manufacturer Name Characteristic");
            }
        }
    }
}

/*
 Invoked upon completion of a -[readValueForCharacteristic:] request or on the reception of a notification/indication.
 */
- (void) peripheral:(CBPeripheral *)aPeripheral didUpdateValueForCharacteristic:(CBCharacteristic *)characteristic error:(NSError *)error 
{
    /* Updated value for heart rate measurement received */
    if ([characteristic.UUID isEqual:[CBUUID UUIDWithString:@"2A37"]]) 
    {
        if( (characteristic.value)  || !error )
        {
            /* Update UI with heart rate data */
            [self updateWithHRMData:characteristic.value];
        }
    } 
    /* Value for body sensor location received */
    else  if ([characteristic.UUID isEqual:[CBUUID UUIDWithString:@"2A38"]]) 
    {
        NSData * updatedValue = characteristic.value;        
        uint8_t* dataPointer = (uint8_t*)[updatedValue bytes];
        if(dataPointer)
        {
            uint8_t location = dataPointer[0];
            NSString*  locationString;
            switch (location)
            {
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
    /* Value for device Name received */
    else if ([characteristic.UUID isEqual:[CBUUID UUIDWithString:CBUUIDDeviceNameString]])
    {
        NSString * deviceName = [[NSString alloc] initWithData:characteristic.value encoding:NSUTF8StringEncoding];
        NSLog(@"Device Name = %@", deviceName);
    } 
}

@end
