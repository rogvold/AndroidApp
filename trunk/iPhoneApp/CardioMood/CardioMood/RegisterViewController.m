//
//  RegisterViewController.m
//  CardioMood
//
//  Created by Alexander O. Taraymovich on 05.04.13.
//  Copyright (c) 2013 Alexander O. Taraymovich. All rights reserved.
//

#import "RegisterViewController.h"
#import "CMLabel.h"
#import "KeychainItemWrapper.h"
#import <User.h>
#import <ClientServerInteraction.h>

@interface RegisterViewController () <UITextFieldDelegate, UIPickerViewDelegate>

@end

@implementation RegisterViewController

@synthesize datePicker;
@synthesize sexPicker;

- (void)viewDidLoad
{
    [super viewDidLoad];
    [self.saveButton setTarget:self];
    [self.saveButton setAction:@selector(saveButtonPressed:)];
    self.user = [[User alloc] init];
    titleSex = [[NSMutableArray alloc] init];
    [titleSex addObject:@"Male"];
    [titleSex addObject:@"Female"];
    imageSex = [[NSMutableArray alloc] init];
    [imageSex addObject:@"male"];
    [imageSex addObject:@"female"];
    self.isUsernameEditable = YES;
    NSMutableArray *authorizationData = [NSMutableArray arrayWithObjects:@"", @"", nil];
    NSMutableArray *personalData = [NSMutableArray arrayWithObjects:@"", @"", nil];
    NSMutableArray *physicalParameters = [NSMutableArray arrayWithObjects:@"", @"", @"", @"", nil];
    
    settings = [NSMutableDictionary dictionaryWithObjectsAndKeys:physicalParameters, @"Physical parameters", authorizationData, @"Authorization data", personalData, @"Personal data", nil];
    [UIApplication sharedApplication].networkActivityIndicatorVisible = NO;
    [[self navigationItem] setTitleView:nil];
    [self initializeData];
}

- (IBAction)saveButtonPressed:(id)sender
{
    [UIApplication sharedApplication].networkActivityIndicatorVisible = YES;
    UIActivityIndicatorView *activityIndicator =
    [[UIActivityIndicatorView alloc] initWithFrame:CGRectMake(0, 0, 20, 20)];
    [[self navigationItem] setTitleView:activityIndicator];
    [activityIndicator startAnimating];
    [ClientServerInteraction registerUserWithEmal:self.user.email withPassword:self.user.password completion:^(int code, NSNumber* response, NSError* error, ServerResponseError* serverError) {
        if (code == 1)
        {
            KeychainItemWrapper *keychainItem = [[KeychainItemWrapper alloc] initWithIdentifier:@"CardioMood" accessGroup:nil];
            [ClientServerInteraction authorizeWithEmail:self.usernameField.text withPassword:self.passwordField.text withDeviceId:[[[UIDevice currentDevice] identifierForVendor] UUIDString] completion:^(int code, AccessToken *response, NSError *error, ServerResponseError *serverError) {
                NSString *responseToken = [response token];
                [keychainItem resetKeychainItem];
                [keychainItem setObject:self.passwordField.text forKey:CFBridgingRelease(kSecValueData)];
                [keychainItem setObject:self.usernameField.text forKey:CFBridgingRelease(kSecAttrAccount)];
                [keychainItem setObject:responseToken forKey:CFBridgingRelease(kSecAttrLabel)];
                [ClientServerInteraction getInfo:responseToken completion:^(int code, User* response, NSError* error, ServerResponseError* serverError){
                    response.firstName = self.user.firstName;
                    response.lastName = self.user.lastName;
                    response.height = self.user.height;
                    response.weight = self.user.weight;
                    response.sex = self.user.sex;
                    response.birthDate = self.user.birthDate;
                    [ClientServerInteraction updateInfoForUser:response token:responseToken completion:^(int code, NSNumber* response, NSError* error, ServerResponseError* serverError){
                        [UIApplication sharedApplication].networkActivityIndicatorVisible = NO;
                        [[self navigationItem] setTitleView:nil];
                        [activityIndicator stopAnimating];
                        [self performSegueWithIdentifier:@"registerSegue" sender:self];
                    }];
                }];
            }];
        }
    }];
}

@end
