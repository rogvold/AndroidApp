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

- (void)viewDidLoad
{
    [super viewDidLoad];
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
    [ClientServerInteraction getInfo:username withPassword:password completion:^(User *user, NSError *error){
        self.user = user;
        titleSex = [[NSMutableArray alloc] init];
        [titleSex addObject:@"Male"];
        [titleSex addObject:@"Female"];
        imageSex = [[NSMutableArray alloc] init];
        [imageSex addObject:@"male"];
        [imageSex addObject:@"female"];
        NSMutableArray *authorizationData = [NSMutableArray arrayWithObjects:username, password, nil];
        NSMutableArray *personalData = [NSMutableArray arrayWithObjects:user.firstName, user.lastName, nil];
        NSMutableArray *physicalParameters = [NSMutableArray arrayWithObjects:[NSString stringWithFormat:@"%d", (int)user.height], [NSString stringWithFormat:@"%d", (int)user.weight], @"08/24/1992", user.sex == 1 ? [titleSex objectAtIndex:0] : [titleSex objectAtIndex:1], nil];
        
        settings = [NSMutableDictionary dictionaryWithObjectsAndKeys:physicalParameters, @"Physical parameters", authorizationData, @"Authorization data", personalData, @"Personal data", nil];
        [UIApplication sharedApplication].networkActivityIndicatorVisible = NO;
        [[self navigationItem] setTitleView:nil];
        [activityIndicator stopAnimating];
        [self initializeData];
        [self.tableView reloadData];
    }];
}

- (IBAction)saveButtonPressed:(id)sender
{
    [UIApplication sharedApplication].networkActivityIndicatorVisible = YES;
    UIActivityIndicatorView *activityIndicator =
    [[UIActivityIndicatorView alloc] initWithFrame:CGRectMake(0, 0, 20, 20)];
    [[self navigationItem] setTitleView:activityIndicator];
    [activityIndicator startAnimating];
    [ClientServerInteraction updateInfo:self.user completion:^(NSNumber *response, NSError *error){
        [UIApplication sharedApplication].networkActivityIndicatorVisible = NO;
        [[self navigationItem] setTitleView:nil];
        [activityIndicator stopAnimating];
        [self.navigationController popToRootViewControllerAnimated:YES];
    }];
}

@end
