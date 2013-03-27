//
//  MainViewController.h
//  TeamHRM
//
//  Created by Alexander O. Taraymovich on 17.12.12.
//  Copyright (c) 2012 Alexander O. Taraymovich. All rights reserved.
//

#import <Cocoa/Cocoa.h>
#import <IOBluetooth/IOBluetooth.h>
#import "ScanWindowController.h"
#import "DataBaseInteraction.h"

@interface MainViewController : NSViewController <CBPeripheralDelegate, CBCentralManagerDelegate, NSTableViewDelegate, NSTextFieldDelegate, NSWindowDelegate>
{
    CBCentralManager *manager;
}

@property (strong) NSMutableArray *users;
@property (strong) NSMutableArray *heartRateMonitors;
@property (assign) IBOutlet NSArrayController *monitorsArrayController;
@property (assign) IBOutlet NSArrayController *usersArrayController;

@property (strong) IBOutlet NSTextField *nameFieldValue;
@property (strong) IBOutlet NSTextField *surnameFieldValue;
@property (strong) IBOutlet NSTextField *weightFieldValue;
@property (strong) IBOutlet NSTextField *heightFieldValue;
@property (strong) IBOutlet NSTextField *ageFieldValue;
@property (strong) IBOutlet NSTextField *sensorFieldValue;
@property (strong) IBOutlet NSTextField *heartRateFieldValue;
@property (strong) NSString *heartRate;

@property (strong) DataBaseInteraction *dataBase;
@property (strong) ScanWindowController *scanSheet;

@property (strong) IBOutlet NSTextField *nameField;
@property (strong) IBOutlet NSTextField *surnameField;
@property (strong) IBOutlet NSTextField *weightField;
@property (strong) IBOutlet NSTextField *heightField;
@property (strong) IBOutlet NSTextField *ageField;
@property (strong) IBOutlet NSTextField *bpmField;
@property (strong) IBOutlet NSTextField *sensorField;
@property (strong) IBOutlet NSWindow *removeConfirmationWindow;

@property (strong) IBOutlet NSButton *addUserButton;
@property (strong) IBOutlet NSButton *removeUserButton;
@property (strong) IBOutlet NSButton *syncButton;
@property (strong) IBOutlet NSTableView *sensorsView;
@property (strong) IBOutlet NSButton *connectButton;
@property (strong) IBOutlet NSScrollView *sensorsScrollView;

@property (strong) NSDictionary *appSettings;

- (IBAction)addUserButtonPressed:(id)sender;
- (IBAction)removeUserButtonPressed:(id)sender;

- (IBAction)cancelRemoveUserSheet:(id)sender;
- (IBAction)closeRemoveUserSheet:(id)sender;

- (IBAction)connectButtonPressed:(id)sender;

- (IBAction)syncData:(id)sender;

+ (BOOL)hasConnectivity;

@end
