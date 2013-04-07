//
//  LoginViewController.m
//  CardioMood
//
//  Created by Alexander O. Taraymovich on 16.12.12.
//  Copyright (c) 2012 Alexander O. Taraymovich. All rights reserved.
//

#import "LoginViewController.h"
#import "DeviceViewController.h"
#import "KeychainItemWrapper.h"
#import <ClientServerInteraction.h>

@interface LoginViewController () <UITextFieldDelegate>
@property (nonatomic, strong) IBOutlet UITextField *usernameField;
@property (nonatomic, strong) IBOutlet UITextField *passwordField;
@property (nonatomic, strong) IBOutlet UIBarButtonItem *signInButton;
@property (nonatomic, strong) IBOutlet UILabel *authStatusLabel;
@property (nonatomic, strong) KeychainItemWrapper *keychainItem;

@end

@implementation LoginViewController

- (id)initWithStyle:(UITableViewStyle)style
{
    self = [super initWithStyle:style];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    [self.signInButton setTarget:self];
    [self.signInButton setAction:@selector(signInButtonPressed:)];
    self.usernameField = [[UITextField alloc] initWithFrame:CGRectMake(80, 12, 215, 30)];
    self.passwordField = [[UITextField alloc] initWithFrame:CGRectMake(80, 12, 215, 30)];
    NSMutableArray *authorizationData = [NSMutableArray arrayWithObjects:@"", @"", nil];
    data = [NSMutableDictionary dictionaryWithObjectsAndKeys:authorizationData, @"Authorization data", nil];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
    return [data count];
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    return [data[@"Authorization data"] count];
}

- (NSString *)tableView:(UITableView *)tableView titleForHeaderInSection:(NSInteger)section {
    
    return [data allKeys][section];
}

- (void)initField:(UITextField *)textField section:(NSInteger)section row:(NSInteger)row
{
    textField.text = data[@"Authorization data"][row];
    textField.autocorrectionType = UITextAutocorrectionTypeNo; // no auto correction support
    textField.autocapitalizationType = UITextAutocapitalizationTypeNone; // no auto capitalization support
    
    textField.clearButtonMode = UITextFieldViewModeNever; // no clear 'x' button to the right
    [textField setEnabled: YES];
    [textField setFont:[UIFont fontWithName:@"Arial-BoldMT" size:18]];
}

// Customize the appearance of table view cells.
- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    NSString *CellIdentifier = [NSString stringWithFormat:@"Cell%d%d",indexPath.section, indexPath.row];
    
    UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:CellIdentifier];
    if (cell == nil) {
        cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:CellIdentifier];
    }
    cell.accessoryType = UITableViewCellAccessoryNone;
    
    cell.textLabel.textColor = [UIColor colorWithRed:50.0 / 255.0 green:79.0 / 255.0 blue:133.0 / 255.0 alpha:1.0];
    
    [cell.textLabel setFont:[UIFont fontWithName:@"Arial-BoldMT" size:12]];
    UITextField *textField;
    
    if (indexPath.section == 0 && indexPath.row == 0)
    {
        cell.textLabel.text = @"username";
        textField = self.usernameField;
        [self initField:textField section:indexPath.section row:indexPath.row];
        textField.keyboardType = UIKeyboardTypeEmailAddress;
        textField.returnKeyType = UIReturnKeyDone;
        textField.tag = 0;
        textField.accessibilityIdentifier = @"username";
    }
    if (indexPath.section == 0 && indexPath.row == 1)
    {
        textField = self.passwordField;
        [self initField:textField section:indexPath.section row:indexPath.row];
        textField.keyboardType = UIKeyboardTypeDefault;
        textField.returnKeyType = UIReturnKeyDone;
        textField.secureTextEntry = YES;
        textField.tag = 1;
        cell.textLabel.text = @"password";
    }
    
    UIToolbar *doneBar = [[UIToolbar alloc] initWithFrame:CGRectMake(0, 0, 320, 44)];
    [doneBar setBarStyle:UIBarStyleBlackTranslucent];
    UIBarButtonItem *spacer = [[UIBarButtonItem alloc]    initWithBarButtonSystemItem:UIBarButtonSystemItemFlexibleSpace target:nil action:nil];
    UIBarButtonItem *doneButton = [[UIBarButtonItem alloc] initWithTitle:@"Done" style:UIBarButtonItemStyleDone target:nil action:nil];
    
    [doneBar setItems:@[spacer,
                       doneButton] animated:YES];
    [doneButton setTarget:textField];
    [doneButton setAction:@selector(resignFirstResponder)];
    [textField setInputAccessoryView:doneBar];
    
    textField.delegate = self;
    
    [cell.contentView addSubview:textField];
    [cell setSelectionStyle:UITableViewCellSelectionStyleNone];
    
    return cell;
}

- (IBAction)signInButtonPressed:(id)sender
{
    [UIApplication sharedApplication].networkActivityIndicatorVisible = YES;
    self.keychainItem = [[KeychainItemWrapper alloc] initWithIdentifier:@"CardioMood" accessGroup:nil];
    NSString *password = [self.keychainItem objectForKey:CFBridgingRelease(kSecValueData)];
    NSString *username = [self.keychainItem objectForKey:CFBridgingRelease(kSecAttrAccount)];
    if ((self.usernameField.text != username || self.passwordField.text != password)
        && self.usernameField.text && self.passwordField.text)
    {
        [ClientServerInteraction validateEmail:self.usernameField.text completion:^(int code, NSNumber *response, NSError *error, ServerResponseError *serverError) {
            if (code == 1)
            {
                [ClientServerInteraction checkDataForEmail:self.usernameField.text forPassword:self.passwordField.text completion:^(int code, NSNumber *response, NSError *error, ServerResponseError *serverError) {
                    if (code == 1)
                    {
                        [ClientServerInteraction authorizeWithEmail:self.usernameField.text withPassword:self.passwordField.text withDeviceId:[[[UIDevice currentDevice] identifierForVendor] UUIDString] completion:^(int code, AccessToken *response, NSError *error, ServerResponseError *serverError) {
                            NSLog(@"%@", [response token]);
                            [UIApplication sharedApplication].networkActivityIndicatorVisible = NO;
                            [self.keychainItem resetKeychainItem];
                            [self.keychainItem setObject:self.passwordField.text forKey:CFBridgingRelease(kSecValueData)];
                            [self.keychainItem setObject:self.usernameField.text forKey:CFBridgingRelease(kSecAttrAccount)];
                            [self.keychainItem setObject:[response token] forKey:CFBridgingRelease(kSecAttrLabel)];
                            [self performSegueWithIdentifier:@"signInSegue" sender:self];
                        }];
                    }
                }];
            }
        }];
        /*[ClientServerInteraction validateEmail:self.usernameField.text completion:^(NSNumber *response, NSError *error) {
            if ([response intValue] == 1)
            {
                [ClientServerInteraction checkData:self.usernameField.text withPassword:self.passwordField.text completion:^(NSNumber *response, NSError *error) {
                    if ([response intValue] == 1)
                    {
                        [UIApplication sharedApplication].networkActivityIndicatorVisible = NO;
                        [self.keychainItem resetKeychainItem];
                        [self.keychainItem setObject:self.passwordField.text forKey:CFBridgingRelease(kSecValueData)];
                        [self.keychainItem setObject:self.usernameField.text forKey:CFBridgingRelease(kSecAttrAccount)];
                        [self performSegueWithIdentifier:@"signInSegue" sender:self];
                    }
                    else
                    {
                        [UIApplication sharedApplication].networkActivityIndicatorVisible = NO;
                        
                    }
                }];
            }
            else
            {
                [UIApplication sharedApplication].networkActivityIndicatorVisible = NO;
                
            }
        }];*/
    }
}

- (BOOL)textFieldShouldReturn:(UITextField *)textField {
    [textField resignFirstResponder];
    return NO;
}

- (void)textFieldDidEndEditing:(UITextField *)textField
{
    NSMutableArray *currentSettings;
    switch (textField.tag) {
        case 0:
            currentSettings = data[@"Authorization data"];
            currentSettings[0] = textField.text;
            break;
        case 1:
            currentSettings = data[@"Authorization data"];
            currentSettings[1] = textField.text;
            break;
        default:
            break;
    }
}

@end