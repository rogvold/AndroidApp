//
//  StatViewController.h
//  CardioMood
//
//  Created by Alexander O. Taraymovich on 07.04.13.
//  Copyright (c) 2013 Alexander O. Taraymovich. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "LocalSession.h"
#import "LocalUser.h"
#import "KeychainItemWrapper.h"

@interface StatViewController : UITableViewController {
    NSArray *sessions;
    NSManagedObjectContext *managedObjectContext;
    NSArray *localUserSessions;
}

@property (nonatomic, strong) NSManagedObjectContext *managedObjectContext;
@property (nonatomic, strong) NSArray *localUserSessions;
@property (nonatomic, strong) LocalUser *user;
@property (nonatomic, strong) KeychainItemWrapper *keychainItem;
@property (nonatomic, strong) NSString *username;
@property (nonatomic, strong) NSString *password;
@property (nonatomic, strong) NSString *token;

@end
