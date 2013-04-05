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

@interface LoginViewController ()
@property (nonatomic, strong) IBOutlet UITextField *loginField;
@property (nonatomic, strong) IBOutlet UITextField *passwordField;
@property (nonatomic, strong) IBOutlet UIBarButtonItem *signInButton;
@property (nonatomic, strong) IBOutlet UILabel *authStatusLabel;
@property (nonatomic, strong) KeychainItemWrapper *keychainItem;

@end

@implementation LoginViewController

- (void)viewDidLoad
{
    [super viewDidLoad];
    [self.signInButton setTarget:self];
    [self.signInButton setAction:@selector(signInButtonPressed:)];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (IBAction)signInButtonPressed:(id)sender
{
    [UIApplication sharedApplication].networkActivityIndicatorVisible = YES;
    self.keychainItem = [[KeychainItemWrapper alloc] initWithIdentifier:@"CardioMood" accessGroup:nil];
    NSString *password = [self.keychainItem objectForKey:CFBridgingRelease(kSecValueData)];
    NSString *username = [self.keychainItem objectForKey:CFBridgingRelease(kSecAttrAccount)];
    if ((self.loginField.text != username || self.passwordField.text != password)
        && self.loginField.text && self.passwordField.text)
    {
        [ClientServerInteraction validateEmail:self.loginField.text completion:^(NSNumber *response, NSError *error) {
            if ([response intValue] == 1)
            {
                [ClientServerInteraction checkData:self.loginField.text withPassword:self.passwordField.text completion:^(NSNumber *response, NSError *error) {
                    if ([response intValue] == 1)
                    {
                        [UIApplication sharedApplication].networkActivityIndicatorVisible = NO;
                        [self.keychainItem resetKeychainItem];
                        [self.keychainItem setObject:self.passwordField.text forKey:CFBridgingRelease(kSecValueData)];
                        [self.keychainItem setObject:self.loginField.text forKey:CFBridgingRelease(kSecAttrAccount)];
                        [self performSegueWithIdentifier:@"signInSegue" sender:self];
                    }
                    else
                    {
                        [UIApplication sharedApplication].networkActivityIndicatorVisible = NO;
                        self.authStatusLabel.text = @"Incorrect password";
                    }
                }];
            }
            else
            {
                [UIApplication sharedApplication].networkActivityIndicatorVisible = NO;
                self.authStatusLabel.text = @"Incorrect username";
            }
        }];
    }
}

@end