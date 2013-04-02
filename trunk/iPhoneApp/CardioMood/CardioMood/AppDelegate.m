//
//  AppDelegate.m
//  CardioMood
//
//  Created by Alexander O. Taraymovich on 07.10.12.
//  Copyright (c) 2012 Alexander O. Taraymovich. All rights reserved.
//
 
#import "AppDelegate.h"
#import "KeychainItemWrapper.h"

@implementation AppDelegate

@synthesize window = _window;

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    KeychainItemWrapper *keychainItem = [[KeychainItemWrapper alloc] initWithIdentifier:@"CardioMood" accessGroup:nil];
    NSString *password = [keychainItem objectForKey:CFBridgingRelease(kSecValueData)];
    NSString *username = [keychainItem objectForKey:CFBridgingRelease(kSecAttrAccount)];
    [self.window makeKeyAndVisible];
    
    if ((!username || !password) || ([username isEqual:@""] || [password isEqual:@""]))
    {
        [self.window.rootViewController performSegueWithIdentifier:@"signInStartSegue" sender:self];
    }
    else
    {
        [self.window.rootViewController performSegueWithIdentifier:@"tabStartSegue" sender:self];
    }
    return YES;
}

@end
