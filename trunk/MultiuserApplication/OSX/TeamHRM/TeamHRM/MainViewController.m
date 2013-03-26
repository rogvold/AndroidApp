//
//  MainViewController.m
//  TeamHRM
//
//  Created by Alexander O. Taraymovich on 17.12.12.
//  Copyright (c) 2012 Alexander O. Taraymovich. All rights reserved.
//

#import "MainViewController.h"
#import "ScanWindowController.h"
#import <IOBluetooth/IOBluetooth.h>
#import <sys/socket.h>
#import <netinet/in.h>
#import <SystemConfiguration/SystemConfiguration.h>
#import "User.h"
#import "sqlite3.h"

@interface MainViewController ()

@property (weak) IBOutlet NSTableView *userTableView;

@end

@implementation MainViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        self.heartRateMonitors = [NSMutableArray array];
        self.users = [NSMutableArray array];
        self.baseUrl = @"http://www.cardiomood.com/BaseProjectWeb/";
        self.secret = @"h7a7RaRtvAVwnMGq5BV6";
        manager = [[CBCentralManager alloc] initWithDelegate:self queue:nil];
        [self isLECapableHardware];
        NSString *applicationSupportPath = [NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, YES) objectAtIndex:0];
        NSString *applicationPath = [applicationSupportPath stringByAppendingPathComponent:@"TeamHRM"];
        BOOL isDirectory = NO;
        BOOL exists = [[NSFileManager defaultManager] fileExistsAtPath:applicationPath isDirectory:&isDirectory];
        NSError *error;
        if (!exists)
        {
            BOOL create =[[NSFileManager defaultManager] createDirectoryAtPath:applicationPath withIntermediateDirectories:YES attributes:nil error:&error];
            if (!create)
            {
                NSLog(@"%@", error);
            }
        }
        NSString *databasePath = [applicationPath stringByAppendingPathComponent:@"local.db"];
        self.dataBase = [[DataBaseInteraction alloc] initWithPath:databasePath];
        NSArray *result = [self.dataBase performQuery:@"select * from users"];
        if ([result count] == 0)
        {
            [self.dataBase performQuery:@"create table users(user_id integer primary key, username text, password text, first_name text, last_name text, height integer, weight integer, age integer, sex integer)"];
            [self.dataBase performQuery:@"create table intervals(intervals_id integer primary key, session_id numeric, value text)"];
            [self.dataBase performQuery:@"create table sessions(session_id integer primary key, user_id numeric, start_time text, device_name text, device_id text)"];
        }
        //[self resetDetailInfo];
    }
    
    return self;
}

+ (BOOL)hasConnectivity {
    struct sockaddr_in zeroAddress;
    bzero(&zeroAddress, sizeof(zeroAddress));
    zeroAddress.sin_len = sizeof(zeroAddress);
    zeroAddress.sin_family = AF_INET;
    
    SCNetworkReachabilityRef reachability = SCNetworkReachabilityCreateWithAddress(kCFAllocatorDefault, (const struct sockaddr*)&zeroAddress);
    if(reachability != NULL) {
        //NetworkStatus retVal = NotReachable;
        SCNetworkReachabilityFlags flags;
        if (SCNetworkReachabilityGetFlags(reachability, &flags)) {
            if ((flags & kSCNetworkReachabilityFlagsReachable) == 0)
            {
                // if target host is not reachable
                return NO;
            }
            
            if ((flags & kSCNetworkReachabilityFlagsConnectionRequired) == 0)
            {
                // if target host is reachable and no connection is required
                //  then we'll assume (for now) that your on Wi-Fi
                return YES;
            }
            
            
            if ((((flags & kSCNetworkReachabilityFlagsConnectionOnDemand ) != 0) ||
                 (flags & kSCNetworkReachabilityFlagsConnectionOnTraffic) != 0))
            {
                // ... and the connection is on-demand (or on-traffic) if the
                //     calling application is using the CFSocketStream or higher APIs
                
                if ((flags & kSCNetworkReachabilityFlagsInterventionRequired) == 0)
                {
                    // ... and no [user] intervention is needed
                    return YES;
                }
            }
            
            if ((flags & kSCNetworkReachabilityFlagsIsDirect) == kSCNetworkReachabilityFlagsIsDirect)
            {
                // ... but WWAN connections are OK if the calling application
                //     is using the CFNetwork (CFSocketStream?) APIs.
                return YES;
            }
        }
    }
    
    return NO;
}

- (void)controlTextDidChange:(NSNotification *)notification
{
    if (notification.object == self.username || notification.object == self.password)
    {
        if ([self.username.stringValue length] != 0 && [self.password.stringValue length])
        {
            [self.addButton setEnabled:YES];
        }
        else
        {
            [self.addButton setEnabled:NO];
        }
    }
    else if (notification.object == self.registrationUsername || notification.object == self.registrationPassword)
    {
        if ([self.registrationUsername.stringValue length] != 0 && [self.registrationPassword.stringValue length])
        {
            [self.signupButton setEnabled:YES];
        }
        else
        {
            [self.signupButton setEnabled:NO];
        }
    }
}

-(int) userExists:(NSString *)username
{
    NSArray* objects = @[@"CheckUserExistence", username, @"h7a7RaRtvAVwnMGq5BV6"];
    NSArray* keys = @[@"purpose", @"email", @"secret"];
    
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
    
    NSURL* url = [NSURL URLWithString:@"http://reshaka.ru:8080/BaseProjectWeb/mobileauth"];
    
    
    NSString* stringToSend = [@"json=" stringByAppendingString:json];
    NSData* dataToSend = [stringToSend dataUsingEncoding:NSUTF8StringEncoding];
    
    
    NSMutableURLRequest* request = [[NSMutableURLRequest alloc] initWithURL:url];
    [request setHTTPMethod:@"POST"];
    [request setValue:@"application/x-www-form-urlencoded" forHTTPHeaderField:@"Content-Type"];
    [request setHTTPBody:dataToSend];
    
    return [[[NSJSONSerialization
              JSONObjectWithData:[NSURLConnection sendSynchronousRequest:request returningResponse:nil error:nil]
              options:kNilOptions
              error:&error] objectForKey:@"response"] intValue];
}

- (int) checkUser:(NSString *)username withPassword:(NSString *)password
{
    NSArray* objects = @[@"CheckAuthorisationData", username, password, @"h7a7RaRtvAVwnMGq5BV6"];
    NSArray* keys = @[@"purpose", @"email", @"password", @"secret"];
    
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
    
    NSURL* url = [NSURL URLWithString:@"http://reshaka.ru:8080/BaseProjectWeb/mobileauth"];
    
    
    NSString* stringToSend = [@"json=" stringByAppendingString:json];
    NSData* dataToSend = [stringToSend dataUsingEncoding:NSUTF8StringEncoding];
    
    
    NSMutableURLRequest* request = [[NSMutableURLRequest alloc] initWithURL:url];
    [request setHTTPMethod:@"POST"];
    [request setValue:@"application/x-www-form-urlencoded" forHTTPHeaderField:@"Content-Type"];
    [request setHTTPBody:dataToSend];
    
    return [[[NSJSONSerialization
              JSONObjectWithData:[NSURLConnection sendSynchronousRequest:request returningResponse:nil error:nil]
              options:kNilOptions
              error:&error] objectForKey:@"response"] intValue];
}

- (int) checkUserRegistration:(NSString *)username withPassword:(NSString *)password
{
    NSArray* objects = @[@"Register", username, password, @"h7a7RaRtvAVwnMGq5BV6"];
    NSArray* keys = @[@"purpose", @"email", @"password", @"secret"];
    
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
    
    NSURL* url = [NSURL URLWithString:@"http://reshaka.ru:8080/BaseProjectWeb/mobileauth"];
    
    
    NSString* stringToSend = [@"json=" stringByAppendingString:json];
    NSData* dataToSend = [stringToSend dataUsingEncoding:NSUTF8StringEncoding];
    
    
    NSMutableURLRequest* request = [[NSMutableURLRequest alloc] initWithURL:url];
    [request setHTTPMethod:@"POST"];
    [request setValue:@"application/x-www-form-urlencoded" forHTTPHeaderField:@"Content-Type"];
    [request setHTTPBody:dataToSend];
    
    return [[[NSJSONSerialization
              JSONObjectWithData:[NSURLConnection sendSynchronousRequest:request returningResponse:nil error:nil]
              options:kNilOptions
              error:&error] objectForKey:@"response"] intValue];
}

#pragma mark User Table selection methods
-(User*)selectedUser
{
    NSInteger selectedRow = [self.userTableView selectedRow];
    if( selectedRow >=0 && self.users.count > selectedRow )
    {
        User *selectedUser = [self.users objectAtIndex:selectedRow];
        return selectedUser;
    }
    return nil;
    
}

- (void)tableViewSelectionDidChange:(NSNotification *)notification
{
    if (notification.object == self.userTableView)
    {
        [self resetDetailInfo];
        if ([self.userTableView selectedRow] != -1)
        {
            User *selectedUser = [self selectedUser];
            [self getLocalUserData:selectedUser];
            [self setDetailInfo:selectedUser];
        }
    }
}

#pragma mark Detail info methods
-(void)setDetailInfo:(User *)user
{
    self.nameFieldValue.hidden = false;
    self.surnameFieldValue.hidden = false;
    self.weightFieldValue.hidden = false;
    self.heightFieldValue.hidden = false;
    self.ageFieldValue.hidden = false;
    self.sensorFieldValue.hidden = false;
    self.heartRateFieldValue.hidden = false;
    
    self.nameField.hidden = false;
    self.surnameField.hidden = false;
    self.weightField.hidden = false;
    self.heightField.hidden = false;
    self.ageField.hidden = false;
    self.bpmField.hidden = false;
    self.sensorField.hidden = false;
    
    self.sensorsView.hidden = false;
    self.connectButton.hidden = false;
    self.sensorsScrollView.hidden = false;
    
    if (user)
        [self startScan];
    
    if (user.name)
        self.nameFieldValue.stringValue = user.name;
    if (user.surname)
        self.surnameFieldValue.stringValue = user.surname;
    if (user.weight)
        self.weightFieldValue.stringValue = user.weight;
    if (user.height)
        self.heightFieldValue.stringValue = user.height;
    if (user.age)
        self.ageFieldValue.stringValue = user.age;
    if (user.connectedPeripheral)
        self.sensorFieldValue.stringValue = user.connectedPeripheral.name;
    if (user.heartRate)
        self.heartRateFieldValue.stringValue = user.heartRate;
    self.connectButton.title = user.isConnected ? @"Disconnect" : @"Connect";
    self.sensorsView.enabled = !user.isConnected;
}

- (IBAction)addUserButtonPressed:(id)sender
{
    //[self openAddUserSheet:nil];
    [self openScanWindow:nil];
}

- (IBAction)removeUserButtonPressed:(id)sender
{
    [self openRemoveUserSheet:nil];
}

-(void)resetDetailInfo
{
    self.nameFieldValue.hidden = true;
    self.surnameFieldValue.hidden = true;
    self.weightFieldValue.hidden = true;
    self.heightFieldValue.hidden = true;
    self.ageFieldValue.hidden = true;
    self.sensorFieldValue.hidden = true;
    self.heartRateFieldValue.hidden = true;
    
    self.nameField.hidden = true;
    self.surnameField.hidden = true;
    self.weightField.hidden = true;
    self.heightField.hidden = true;
    self.ageField.hidden = true;
    self.bpmField.hidden = true;
    self.sensorField.hidden = true;
    
    self.sensorsView.hidden = true;
    self.connectButton.hidden = true;
    self.sensorsScrollView.hidden = true;
    
    self.nameFieldValue.stringValue = @"";
    self.surnameFieldValue.stringValue = @"";
    self.weightFieldValue.stringValue = @"";
    self.heightFieldValue.stringValue = @"";
    self.ageFieldValue.stringValue = @"";
    self.sensorFieldValue.stringValue = @"";
    self.heartRateFieldValue.stringValue = @"";
}

#pragma mark - Add sheet methods

/*
 Open scan sheet to discover heart rate peripherals if it is LE capable hardware
 */
- (IBAction)openAddUserSheet:(id)sender
{
    [NSApp beginSheet:self.addSheet modalForWindow:self.view.window modalDelegate:self didEndSelector:@selector(sheetDidEnd:returnCode:contextInfo:) contextInfo:nil];
}

/*
 Close scan sheet once device is selected
 */
- (IBAction)closeAddUserSheet:(id)sender
{
    /*int exists = [self userExists:self.username.stringValue];
     int rightPass = [self checkUser:self.username.stringValue withPassword:self.password.stringValue];
     if (exists && rightPass)
     {*/
    [NSApp endSheet:self.addSheet returnCode:NSAlertDefaultReturn];
    [self.addSheet orderOut:self];
    /*}
     else if (!exists)
     {
     self.authError.stringValue = @"User with entered e-mail not found.";
     }
     else if (exists && !rightPass)
     {
     self.authError.stringValue = @"Password is incorrect. Try again.";
     }
     else if (exists == -1 && rightPass == -1)
     {
     self.authError.stringValue = @"An error occured. Try again later.";
     }*/
}

/*
 Close scan sheet without choosing any device
 */
- (IBAction)cancelAddUserSheet:(id)sender
{
    [NSApp endSheet:self.addSheet returnCode:NSAlertAlternateReturn];
    [self.addSheet orderOut:self];
}

/*
 This method is called when Scan sheet is closed. Initiate connection to selected heart rate peripheral
 */
- (void)sheetDidEnd:(NSWindow *)sheet returnCode:(NSInteger)returnCode contextInfo:(void *)contextInfo
{
    if( returnCode == NSAlertDefaultReturn )
    {
        User *newUser = [User alloc];
        newUser.username = self.username.stringValue;
        newUser.password = self.password.stringValue;
        newUser.create = 1;
        newUser.intervals = [NSMutableArray array];
        newUser.heartRate = @"0";
        NSArray *result = [self.dataBase performQuery:[NSString stringWithFormat:@"select user_id from users where username = \"%@\"", newUser.username]];
        if ([result count] == 0)
        {
            [self.dataBase performQuery:[NSString stringWithFormat:@"insert into users(username, password) values(\"%@\", \"%@\")", newUser.username, newUser.password]];
            
            result = [self.dataBase performQuery:[NSString stringWithFormat:@"select user_id from users where username = \"%@\"", newUser.username]];
        }
        newUser.userId = [[[result objectAtIndex:0] objectAtIndex:0] intValue];
        NSMutableArray *user = [self mutableArrayValueForKey:@"users"];
        if( ![self.users containsObject:newUser] )
            [user addObject:newUser];
        self.username.stringValue = @"";
        self.password.stringValue = @"";
        self.authError.stringValue = @"";
        [self.removeUserButton setEnabled:YES];
    }
}

- (IBAction)connectButtonPressed:(id)sender
{
    User *user = [self selectedUser];
    if (!user.isConnected)
    {
        [self stopScan];
        NSIndexSet *indexes = [self.monitorsArrayController selectionIndexes];
        if ([indexes count] != 0)
        {
            NSUInteger anIndex = [indexes firstIndex];
            CBPeripheral *peripheral = [self.heartRateMonitors objectAtIndex:anIndex];
            user.connectedPeripheral = peripheral;
            [manager connectPeripheral:peripheral options:nil];
            [self.heartRateMonitors removeObject:peripheral];
            self.sensorFieldValue.stringValue = user.connectedPeripheral.name;
            user.isConnected = true;
            self.connectButton.title = @"Disconnect";
            self.sensorsView.enabled = false;
        }
    }
    else
    {
        [manager cancelPeripheralConnection:user.connectedPeripheral];
    }
}

#pragma mark - Register sheet methods
- (IBAction)registerUserButtonPressed:(id)sender
{
    [self openRegisterUserSheet:nil];
}

- (IBAction)openRegisterUserSheet:(id)sender
{
    [self cancelAddUserSheet:nil];
    [NSApp beginSheet:self.registerSheet modalForWindow:self.view.window modalDelegate:self didEndSelector:@selector(registerSheetDidEnd:returnCode:contextInfo:) contextInfo:nil];
}

- (IBAction)closeRegisterUserSheet:(id)sender
{
    int exists = [self userExists:self.registrationUsername.stringValue];
    int registrationStatus = -1;
    if(!exists)
        registrationStatus = [self checkUserRegistration:self.registrationUsername.stringValue withPassword:self.registrationPassword.stringValue];
    if (!exists && registrationStatus == 1)
    {
        [NSApp endSheet:self.registerSheet returnCode:NSAlertDefaultReturn];
        [self.registerSheet orderOut:self];
        [self openAddUserSheet:nil];
    }
    else if (exists)
    {
        self.registrationError.stringValue = @"User with entered e-mail already exists";
    }
    else if (!exists && registrationStatus == -1)
    {
        self.registrationError.stringValue = @"A problem occured. Try again later.";
    }
}

- (IBAction)cancelRegisterUserSheet:(id)sender
{
    [NSApp endSheet:self.registerSheet returnCode:NSAlertAlternateReturn];
    [self.registerSheet orderOut:self];
    [self openAddUserSheet:nil];
}

- (void)registerSheetDidEnd:(NSWindow *)sheet returnCode:(NSInteger)returnCode contextInfo:(void *)contextInfo
{
    if( returnCode == NSAlertDefaultReturn )
    {
        self.username.stringValue = self.registrationUsername.stringValue;
        self.password.stringValue = self.registrationPassword.stringValue;
        [self.addButton setEnabled:YES];
        self.registrationError.stringValue = @"";
    }
}

#pragma mark - Remove confirmation sheet methods

/*
 Open scan sheet to discover heart rate peripherals if it is LE capable hardware
 */
- (IBAction)openRemoveUserSheet:(id)sender
{
    [NSApp beginSheet:self.removeConfirmationWindow modalForWindow:self.view.window modalDelegate:self didEndSelector:@selector(removeSheetDidEnd:returnCode:contextInfo:) contextInfo:nil];
}

/*
 Close scan sheet once device is selected
 */
- (IBAction)closeRemoveUserSheet:(id)sender
{
    [NSApp endSheet:self.removeConfirmationWindow returnCode:NSAlertDefaultReturn];
    
    [self.removeConfirmationWindow orderOut:self];
}

/*
 Close scan sheet without choosing any device
 */
- (IBAction)cancelRemoveUserSheet:(id)sender
{
    [NSApp endSheet:self.removeConfirmationWindow returnCode:NSAlertAlternateReturn];
    [self.removeConfirmationWindow orderOut:self];
}

/*
 This method is called when Scan sheet is closed. Initiate connection to selected heart rate peripheral
 */
- (void)removeSheetDidEnd:(NSWindow *)sheet returnCode:(NSInteger)returnCode contextInfo:(void *)contextInfo
{
    if( returnCode == NSAlertDefaultReturn )
    {
        NSIndexSet *indexes = [self.usersArrayController selectionIndexes];
        if ([indexes count] != 0)
        {
            NSUInteger anIndex = [indexes firstIndex];
            User *user = [self.users objectAtIndex:anIndex];
            @synchronized (self.users)
            {
                if (user.isConnected)
                    [manager cancelPeripheralConnection:user.connectedPeripheral];
                [self.users removeObject:user];
            }
            [self resetDetailInfo];
            [self.userTableView reloadData];
        }
        if (self.users.count == 0)
        {
            [self.removeUserButton setEnabled:YES];
        }
    }
}

#pragma mark - Heart Rate Data

/*
 Update UI with heart rate data received from device
 */
- (void) updateHRMData:(CBPeripheral *)peripheral withData:(NSData *)data
{
    NSPredicate *predicate = [NSPredicate predicateWithFormat:@"self.connectedPeripheral == %@", peripheral ];
    NSArray *filtered;
    filtered  = [self.users filteredArrayUsingPredicate:predicate];
    if ([filtered count] != 0)
    {
        User *updatingUser = [filtered objectAtIndex:0];
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
        }
        else
        {
            NSLog(@"RR intervals are present");
            NSMutableArray* rrs = [NSMutableArray array];
            uint16_t rr = (CFSwapInt16LittleToHost(*(uint16_t *)(&reportData[rrByte])) * 1000) / 1024;
            while (rr != 0)
            {
                [rrs addObject:[NSNumber numberWithInt:rr]];
                rrByte += 2;
                rr = (CFSwapInt16LittleToHost(*(uint16_t *)(&reportData[rrByte])) * 1000) / 1024;
            }
            [updatingUser.intervals addObjectsFromArray:rrs];
            [updatingUser.intervalsToSave addObjectsFromArray:rrs];
            
            if ([updatingUser.intervalsToSave count] >= 50) {
                [self saveIntervals:updatingUser];
                [updatingUser.intervalsToSave removeAllObjects];
                updatingUser.startTime = [NSDate date];
            }
        }
        
        updatingUser.heartRate = [NSString stringWithFormat:@"%d", bpm];
        if (updatingUser == [self selectedUser])
        {
            self.heartRate = updatingUser.heartRate;
            //NSLog(@"Data recieved from: %@", updatingUser.connectedPeripheral);
        }
    }
}

#pragma mark - Start/Stop Scan methods

/*
 Uses CBCentralManager to check whether the current platform/hardware supports Bluetooth LE. An alert is raised if Bluetooth LE is not enabled or is not supported.
 */
- (BOOL) isLECapableHardware
{
    NSString * state = nil;
    [self.addUserButton setEnabled:YES];
    
    
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
            [self.addUserButton setEnabled:YES];
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
    [alert beginSheetModalForWindow:[[self view] window] modalDelegate:self didEndSelector:nil contextInfo:nil];
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
}

/*
 Invoked whenever a connection is succesfully created with the peripheral.
 Discover available services on the peripheral
 */
- (void) centralManager:(CBCentralManager *)central didConnectPeripheral:(CBPeripheral *)aPeripheral
{
    [aPeripheral setDelegate:self];
    [aPeripheral discoverServices:nil];
    NSPredicate *predicate = [NSPredicate predicateWithFormat:@"self.connectedPeripheral == %@", aPeripheral ];
    NSArray *filtered  = [self.users filteredArrayUsingPredicate:predicate];
    User *updatingUser = [filtered objectAtIndex:0];
    updatingUser.startTime = [NSDate date];
    updatingUser.connectedPeripheral = aPeripheral;
    updatingUser.deviceId = (NSString *)CFBridgingRelease(CFUUIDCreateString(NULL, [aPeripheral UUID]));
    updatingUser.deviceName = [aPeripheral name];
    NSDateFormatter* dateFormatter = [[NSDateFormatter alloc] init];
    [dateFormatter setDateFormat:@"yyyy-MM-dd HH:mm:ss.SSS"];
    [self.dataBase performQuery:[NSString stringWithFormat:@"insert into sessions(start_time, user_id, device_name, device_id) values(\"%@\", %ld, \"%@\", \"%@\")", [dateFormatter stringFromDate:updatingUser.startTime], (long)updatingUser.userId, updatingUser.deviceName, updatingUser.deviceId]];
    NSArray *result = [self.dataBase performQuery:[NSString stringWithFormat:@"select session_id from sessions where start_time = \"%@\"", [dateFormatter stringFromDate:updatingUser.startTime]]];
    updatingUser.currentSessionId = [[[result objectAtIndex:0] objectAtIndex:0] intValue];
}

/*
 Invoked whenever an existing connection with the peripheral is torn down.
 Reset local variables
 */
- (void)centralManager:(CBCentralManager *)central didDisconnectPeripheral:(CBPeripheral *)aPeripheral error:(NSError *)error
{
    [self.connectButton setTitle:@"Connect"];
    self.heartRate = @"0";
    self.sensorFieldValue.stringValue = @"";
    self.sensorsView.enabled = true;
    [self.heartRateMonitors addObject:aPeripheral];
    NSPredicate *predicate = [NSPredicate predicateWithFormat:@"self.connectedPeripheral == %@", aPeripheral ];
    NSArray *filtered;
    filtered  = [self.users filteredArrayUsingPredicate:predicate];
    User *user = [User alloc];
    if (filtered)
    {
        user = filtered[0];
        user.connectedPeripheral = nil;
        user.heartRate = @"0";
        user.isConnected = false;
        if ([user.intervalsToSave count] > 0)
        {
            [self saveIntervals:user];
        }
    }
}

/*
 Invoked whenever the central manager fails to create a connection with the peripheral.
 */
- (void)centralManager:(CBCentralManager *)central didFailToConnectPeripheral:(CBPeripheral *)aPeripheral error:(NSError *)error
{
    NSLog(@"Fail to connect to peripheral: %@ with error = %@", aPeripheral, [error localizedDescription]);
    [self.connectButton setTitle:@"Connect"];
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
                [aPeripheral setNotifyValue:YES forCharacteristic:aChar];
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
            [self updateHRMData:aPeripheral withData:characteristic.value];
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

#pragma mark Json methods
// Send intervals to server
- (NSString *) sendRRs:(User *)user
{
    NSError *error;
    NSString* jsonString = [self makeJSON:user];
    NSString* stringToSend = [@"json=" stringByAppendingString:jsonString];
    NSData* dataToSend = [stringToSend dataUsingEncoding:NSUTF8StringEncoding];
    
    NSString *urlString = [NSString stringWithFormat:@"%@resources/rates/sync", self.baseUrl];
    NSURL* url = [NSURL URLWithString:urlString];
    
    NSMutableURLRequest* request = [[NSMutableURLRequest alloc] initWithURL:url];
    [request setHTTPMethod:@"POST"];
    [request setValue:@"application/x-www-form-urlencoded" forHTTPHeaderField:@"Content-Type"];
    [request setHTTPBody:dataToSend];
    
    NSDictionary *response = [NSJSONSerialization
              JSONObjectWithData:[NSURLConnection sendSynchronousRequest:request returningResponse:nil error:nil]
              options:kNilOptions
              error:&error];
    if ([response objectForKey:@"response"])
        return [response objectForKey:@"response"];
    else
    {
        NSLog(@"%@", [response objectForKey:@"error"]);
        return @"";
    }
}

// Make json based on array of rr intervals
-(NSString *) makeJSON:(User *)user
{
    NSArray* objects = @[[NSString stringWithFormat:@"%ld", (NSInteger)([user.startTime timeIntervalSince1970] * 1000)], user.intervals, user.username, user.password, user.create == 0 ? @"0" : @"1"];
    NSArray* keys = @[@"start", @"rates", @"email", @"password", @"create"];
    
    user.create = 0;
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

-(void) saveIntervals:(User *)user
{
    [self.dataBase performQuery:[NSString stringWithFormat:@"insert into intervals(session_id, value) values(%ld, \"%@\")", (long)user.currentSessionId, [user.intervalsToSave componentsJoinedByString:@","]]];
    [user.intervalsToSave removeAllObjects];

}

- (IBAction) syncData:(id)sender
{
    [self.syncButton setEnabled:NO];
    if ([MainViewController hasConnectivity])
    {
        [self performSelectorInBackground:@selector(sync) withObject:nil];
    }
    else
    {
        NSAlert *alert = [[NSAlert alloc] init];
        [alert setMessageText:@"No internet connection"];
        [alert addButtonWithTitle:@"OK"];
        [alert setIcon:[[NSImage alloc] initWithContentsOfFile:@"AppIcon"]];
        [alert beginSheetModalForWindow:[[self view] window] modalDelegate:self didEndSelector:nil contextInfo:nil];
    }
}

-(void) sync
{
    NSArray *users = [self.dataBase performQuery:@"select * from users"];
    for (NSArray *user in users)
    {
        User *newUser = [User alloc];
        newUser.username = [user objectAtIndex:1];
        newUser.password = [user objectAtIndex:2];
        [self getUserData:newUser];
        NSArray *sessions = [self.dataBase performQuery:[NSString stringWithFormat:@"select * from sessions where user_id = %ld", (long)[[user objectAtIndex:0] intValue]]];
        for (NSArray *session in sessions)
        {
            NSArray *intervalValues = [self.dataBase performQuery:[NSString stringWithFormat:@"select value from intervals where session_id = %ld", (long)[[session objectAtIndex:0] intValue]]];
            NSMutableArray *intervals = [NSMutableArray array];
            for (NSArray *value in intervalValues)
            {
                [intervals addObjectsFromArray:[[value objectAtIndex:0] componentsSeparatedByString:@","]];
            }
            newUser.create = 1;
            newUser.intervals = intervals;
            newUser.heartRate = @"0";
            NSDateFormatter* dateFormatter = [[NSDateFormatter alloc] init];
            [dateFormatter setDateFormat:@"yyyy-MM-dd HH:mm:ss.SSS"];
            newUser.startTime = [dateFormatter dateFromString:[session objectAtIndex:2]];
            newUser.deviceName = [session objectAtIndex:3];
            newUser.deviceId = [session objectAtIndex:4];
            if ([[self sendRRs:newUser] isEqual:@"1"])
            {
                [self.dataBase performQuery:[NSString stringWithFormat:@"delete from intervals where session_id = %ld", (long)[[session objectAtIndex:0] intValue]]];
                [self.dataBase performQuery:[NSString stringWithFormat:@"delete from sessions where session_id = %ld", (long)[[session objectAtIndex:0] intValue]]];
                [self.dataBase performQuery:@"vacuum"];
            }
        }
    }
    User *selectedUser = [self selectedUser];
    if (selectedUser)
    {
        [self getLocalUserData:selectedUser];
        [self setDetailInfo:selectedUser];
    }
    NSArray *result = [self.dataBase performQuery:@"select * from sessions"];
    if ([result count] == 0)
    {
        NSAlert *alert = [[NSAlert alloc] init];
        [alert setMessageText:@"Synchronization successfully finished."];
        [alert addButtonWithTitle:@"OK"];
        [alert setIcon:[[NSImage alloc] initWithContentsOfFile:@"AppIcon"]];
        [alert beginSheetModalForWindow:[[self view] window] modalDelegate:self didEndSelector:nil contextInfo:nil];
        [self.syncButton setEnabled:NO];
    }
}

- (IBAction)openScanWindow:(id)sender
{
    self.scanSheet = [[ScanWindowController alloc] initWithWindowNibName:@"ScanWindowController"];
    [NSApp beginSheet:self.scanSheet.window modalForWindow:self.view.window modalDelegate:self didEndSelector:@selector(scanSheetDidEnd:returnCode:contextInfo:) contextInfo:nil];
}

-(void)scanSheetDidEnd:(NSWindow *)sheet returnCode:(NSInteger)returnCode contextInfo:(void *)contextInfo
{
    if( returnCode == NSAlertDefaultReturn )
    {
        NSString *username = self.scanSheet.username;
        BOOL retry = false;
        for (User *user in self.users)
        {
            if ([user.username isEqualToString:username])
            {
                retry = true;
            }
        }
        if (retry)
        {
            [NSApp endSheet:self.scanSheet.window returnCode:NSAlertDefaultReturn];
            [self.scanSheet.window orderOut:self];
            NSAlert *alert = [[NSAlert alloc] init];
            [alert setMessageText:@"User with this username currently signed in."];
            [alert addButtonWithTitle:@"OK"];
            [alert setIcon:[[NSImage alloc] initWithContentsOfFile:@"AppIcon"]];
            [alert beginSheetModalForWindow:[[self view] window] modalDelegate:self didEndSelector:nil contextInfo:nil];
        }
        else
        {
            User *newUser = [User alloc];
            newUser.username = self.scanSheet.username;
            newUser.password = self.scanSheet.password;
            newUser.create = 1;
            newUser.intervals = [NSMutableArray array];
            newUser.intervalsToSave = [NSMutableArray array];
            newUser.heartRate = @"0";
            NSArray *result = [self.dataBase performQuery:[NSString stringWithFormat:@"select user_id from users where username = \"%@\"", newUser.username]];
            if ([result count] == 0)
            {
                [self.dataBase performQuery:[NSString stringWithFormat:@"insert into users(username, password) values(\"%@\", \"%@\")", newUser.username, newUser.password]];
                
                result = [self.dataBase performQuery:[NSString stringWithFormat:@"select user_id from users where username = \"%@\"", newUser.username]];
            }
            if ([MainViewController hasConnectivity])
            {
                [self getUserData:newUser];
            }
            newUser = [self getLocalUserData:newUser];
            newUser.userId = [[[result objectAtIndex:0] objectAtIndex:0] intValue];
            NSMutableArray *user = [self mutableArrayValueForKey:@"users"];
            if( ![self.users containsObject:newUser] )
                [user addObject:newUser];
            [self.removeUserButton setEnabled:YES];
        }
    }
    
}

-(void)getUserData:(User*)user
{
    NSError* error = nil;
    NSString *urlString = [NSString stringWithFormat:@"%@resources/auth/info?secret=%@&email=%@&password=%@", self.baseUrl, self.secret, user.username, user.password];
    
    NSURL* url = [NSURL URLWithString:[NSString stringWithFormat:@"%@", urlString]];
    
    NSMutableURLRequest* request = [[NSMutableURLRequest alloc] initWithURL:url];
    [request setHTTPMethod:@"POST"];
    [request setValue:@"application/x-www-form-urlencoded" forHTTPHeaderField:@"Content-Type"];
    //[request setTimeoutInterval:5];
    
    NSDictionary *response = [NSJSONSerialization
                              JSONObjectWithData:[NSURLConnection sendSynchronousRequest:request returningResponse:nil error:nil]
                              options:kNilOptions
                              error:&error];
    
    user.name = [response objectForKey:@"firstName"];
    user.surname = [response objectForKey:@"lastName"];
    user.weight = [response objectForKey:@"weight"];
    user.height = [response objectForKey:@"height"];
    user.age = [response objectForKey:@"age"];
    user.sex = [response objectForKey:@"sex"];
    [self.dataBase performQuery:[NSString stringWithFormat:@"update users set first_name = \"%@\", last_name = \"%@\", weight = \"%@\", height = \"%@\", age = \"%@\", sex = \"%@\" where username = \"%@\"", user.name, user.surname, user.weight, user.height, user.age, user.sex, user.username]];
}

-(User*)getLocalUserData:(User*)user
{
    NSArray *result = [self.dataBase performQuery:[NSString stringWithFormat:@"select * from users where username = \"%@\"", user.username]];
    
    user.name = [[[result objectAtIndex:0]objectAtIndex:3] isEqual:[NSNull  null]] ? @"" : [[result objectAtIndex:0]objectAtIndex:3];
    user.surname = [[[result objectAtIndex:0]objectAtIndex:4] isEqual:[NSNull  null]] ? @"" : [[result objectAtIndex:0]objectAtIndex:4];
    user.weight = [[[result objectAtIndex:0]objectAtIndex:5] isEqual:[NSNull  null]] ? @"" : [[result objectAtIndex:0]objectAtIndex:5];
    user.height = [[[result objectAtIndex:0]objectAtIndex:6] isEqual:[NSNull  null]] ? @"" : [[result objectAtIndex:0]objectAtIndex:6];
    user.age = [[[result objectAtIndex:0]objectAtIndex:7] isEqual:[NSNull  null]] ? @"" : [[result objectAtIndex:0]objectAtIndex:7];
    user.sex = [[[result objectAtIndex:0]objectAtIndex:8] isEqual:[NSNull  null]] ? @"" : [[result objectAtIndex:0]objectAtIndex:8];
    
    return user;
}

@end
