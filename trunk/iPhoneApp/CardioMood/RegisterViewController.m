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
    [ClientServerInteraction registerUser:self.user.email withPassword:self.user.password completion:^(NSNumber *response, NSError *error) {
        [ClientServerInteraction getInfo:self.user.email withPassword:self.user.password completion:^(User *user, NSError *error){
            user.firstName = self.user.firstName;
            user.lastName = self.user.lastName;
            user.height = self.user.height;
            user.weight = self.user.weight;
            user.sex = self.user.sex;
            //user.age = self.user.firstName;
            [ClientServerInteraction updateInfo:user completion:^(NSNumber *response, NSError *error){
                KeychainItemWrapper *keychainItem = [[KeychainItemWrapper alloc] initWithIdentifier:@"CardioMood" accessGroup:nil];
                [keychainItem resetKeychainItem];
                [keychainItem setObject:self.user.email forKey:CFBridgingRelease(kSecValueData)];
                [keychainItem setObject:self.user.password forKey:CFBridgingRelease(kSecAttrAccount)];
                [UIApplication sharedApplication].networkActivityIndicatorVisible = NO;
                [[self navigationItem] setTitleView:nil];
                [activityIndicator stopAnimating];
                [self performSegueWithIdentifier:@"registerSegue" sender:self];
            }];
        }];
    }];
}

@end
