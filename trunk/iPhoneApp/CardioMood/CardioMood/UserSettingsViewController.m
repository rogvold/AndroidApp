//
//  UserSettingsViewController.m
//  CardioMood
//
//  Created by Alexander O. Taraymovich on 02.04.13.
//  Copyright (c) 2013 Alexander O. Taraymovich. All rights reserved.
//

#import "UserSettingsViewController.h"
#import <ClientServerInteraction.h>
#import <User.h>
#import "KeychainItemWrapper.h"
#import "CMLabel.h"

@interface UserSettingsViewController () <UITextFieldDelegate, UIPickerViewDelegate>

@end

@implementation UserSettingsViewController

- (void)viewDidAppear:(BOOL)animated
{
    [super viewDidAppear:animated];
    [self.saveButton setTarget:self];
    [self.saveButton setAction:@selector(saveButtonPressed:)];
    [UIApplication sharedApplication].networkActivityIndicatorVisible = YES;
    UIActivityIndicatorView *activityIndicator =
    [[UIActivityIndicatorView alloc] initWithFrame:CGRectMake(0, 0, 20, 20)];
    [[self navigationItem] setTitleView:activityIndicator];
    [activityIndicator startAnimating];
    self.isUsernameEditable = NO;
    
    KeychainItemWrapper *keychainItem = [[KeychainItemWrapper alloc] initWithIdentifier:@"CardioMood" accessGroup:nil];
    NSString *password = [keychainItem objectForKey:CFBridgingRelease(kSecValueData)];
    NSString *username = [keychainItem objectForKey:CFBridgingRelease(kSecAttrAccount)];
    NSString *token = [keychainItem objectForKey:CFBridgingRelease(kSecAttrLabel)];
    [ClientServerInteraction getInfo:token completion:^(int code, User *response, NSError *error, ServerResponseError *serverError) {
        if (code == 1)
        {
            [self initUser:response];
            [activityIndicator stopAnimating];
        }
        else if (code == 3)
        {
            if (serverError.errorCode == InvalidToken)
            {
                [ClientServerInteraction authorizeWithEmail:username withPassword:password withDeviceId:[[[UIDevice currentDevice] identifierForVendor] UUIDString] completion:^(int code, AccessToken *response, NSError *error, ServerResponseError *serverError) {
                    [keychainItem setObject:[response token] forKey:CFBridgingRelease(kSecAttrLabel)];
                    [ClientServerInteraction getInfo:token completion:^(int code, User *response, NSError *error, ServerResponseError *serverError) {
                        if (code == 1)
                        {
                            [self initUser:response];
                            [UIApplication sharedApplication].networkActivityIndicatorVisible = NO;
                            [activityIndicator stopAnimating];
                            [[self navigationItem] setTitleView:nil];
                        }
                    }];
                }];
            }
        }
    }];
}

-(void)initUser:(User *)response
{
    self.user = response;
    titleSex = [[NSMutableArray alloc] init];
    [titleSex addObject:@"Male"];
    [titleSex addObject:@"Female"];
    imageSex = [[NSMutableArray alloc] init];
    [imageSex addObject:@"male"];
    [imageSex addObject:@"female"];
    NSMutableArray *authorizationData = [NSMutableArray arrayWithObjects:response.email, response.password, nil];
    NSMutableArray *personalData = [NSMutableArray arrayWithObjects:response.firstName, response.lastName, nil];
    NSMutableArray *physicalParameters = [NSMutableArray arrayWithObjects:[response.height stringValue], [response.weight stringValue], response.birthDate, [response.sex intValue] == 1 ? titleSex[0] : titleSex[1], nil];
    
    settings = [NSMutableDictionary dictionaryWithObjectsAndKeys:physicalParameters, @"Physical parameters", authorizationData, @"Authorization data", personalData, @"Personal data", nil];
    [UIApplication sharedApplication].networkActivityIndicatorVisible = NO;
    [[self navigationItem] setTitleView:nil];
    [self initializeData];
    [self.tableView reloadData];
}

- (IBAction)saveButtonPressed:(id)sender
{
    [UIApplication sharedApplication].networkActivityIndicatorVisible = YES;
    UIActivityIndicatorView *activityIndicator =
    [[UIActivityIndicatorView alloc] initWithFrame:CGRectMake(0, 0, 20, 20)];
    [[self navigationItem] setTitleView:activityIndicator];
    [activityIndicator startAnimating];
    KeychainItemWrapper *keychainItem = [[KeychainItemWrapper alloc] initWithIdentifier:@"CardioMood" accessGroup:nil];
    NSString *password = [keychainItem objectForKey:CFBridgingRelease(kSecValueData)];
    NSString *username = [keychainItem objectForKey:CFBridgingRelease(kSecAttrAccount)];
    NSString *token = [keychainItem objectForKey:CFBridgingRelease(kSecAttrLabel)];
    [ClientServerInteraction updateInfoForUser:self.user token:token completion:^(int code, NSNumber *response, NSError *error, ServerResponseError *serverError) {
        if (code == 1)
        {
            [UIApplication sharedApplication].networkActivityIndicatorVisible = NO;
            [[self navigationItem] setTitleView:nil];
            [activityIndicator stopAnimating];
            [self.navigationController popToRootViewControllerAnimated:YES];
        }
        else if (code == 3)
        {
            if (serverError.errorCode == InvalidToken)
            {
                [ClientServerInteraction authorizeWithEmail:username withPassword:password withDeviceId:[[[UIDevice currentDevice] identifierForVendor] UUIDString] completion:^(int code, AccessToken *response, NSError *error, ServerResponseError *serverError) {
                    [keychainItem setObject:[response token] forKey:CFBridgingRelease(kSecAttrLabel)];
                    [ClientServerInteraction updateInfoForUser:self.user token:[response token] completion:^(int code, NSNumber *response, NSError *error, ServerResponseError *serverError) {
                        if (code == 1)
                        {
                            [UIApplication sharedApplication].networkActivityIndicatorVisible = NO;
                            [[self navigationItem] setTitleView:nil];
                            [activityIndicator stopAnimating];
                            [self.navigationController popToRootViewControllerAnimated:YES];
                        }
                    }];
                }];
            }
        }
    }];
}

@end
