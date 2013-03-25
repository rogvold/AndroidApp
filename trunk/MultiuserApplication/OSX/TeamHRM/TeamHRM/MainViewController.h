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
@property (strong) IBOutlet NSWindow *addSheet;
@property (strong) NSMutableArray *heartRateMonitors;
@property (assign) IBOutlet NSArrayController *monitorsArrayController;
@property (assign) IBOutlet NSArrayController *usersArrayController;
@property (strong) IBOutlet NSSecureTextField *password;
@property (strong) IBOutlet NSTextField *username;

@property (strong) IBOutlet NSSecureTextField *registrationPassword;
@property (strong) IBOutlet NSTextField *registrationUsername;
@property (strong) IBOutlet NSButton *signupButton;
@property (strong) IBOutlet NSWindow *registerSheet;
@property (strong) IBOutlet NSTextField *registrationError;

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

@property (strong) IBOutlet NSButton *addButton;
@property (strong) IBOutlet NSButton *addUserButton;
@property (strong) IBOutlet NSButton *removeUserButton;
@property (strong) IBOutlet NSTableView *sensorsView;
@property (strong) IBOutlet NSButton *connectButton;
@property (strong) IBOutlet NSScrollView *sensorsScrollView;

@property (strong) IBOutlet NSTextField *authError;

@property (strong) NSString *baseUrl;
@property (strong) NSString *secret;

- (IBAction)addUserButtonPressed:(id)sender;
- (IBAction)removeUserButtonPressed:(id)sender;
- (IBAction)openAddUserSheet:(id)sender;
- (IBAction)closeAddUserSheet:(id)sender;
- (IBAction)cancelAddUserSheet:(id)sender;

- (IBAction)cancelRemoveUserSheet:(id)sender;
- (IBAction)closeRemoveUserSheet:(id)sender;

- (IBAction)connectButtonPressed:(id)sender;

- (IBAction)registerUserButtonPressed:(id)sender;
- (IBAction)openRegisterUserSheet:(id)sender;
- (IBAction)closeRegisterUserSheet:(id)sender;
- (IBAction)cancelRegisterUserSheet:(id)sender;

- (IBAction)syncData:(id)sender;

- (IBAction)terminateApp:(id)sender;

@end
