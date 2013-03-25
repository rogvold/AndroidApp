//
//  AppDelegate.h
//  TeamHRM
//
//  Created by Alexander O. Taraymovich on 16.12.12.
//  Copyright (c) 2012 Alexander O. Taraymovich. All rights reserved.
//

#import <Cocoa/Cocoa.h>
#import "MainViewController.h"

@interface AppDelegate : NSObject <NSApplicationDelegate, NSWindowDelegate>

@property (assign) IBOutlet NSWindow *window;
@property (nonatomic,strong) IBOutlet MainViewController *mainViewController;

@end
