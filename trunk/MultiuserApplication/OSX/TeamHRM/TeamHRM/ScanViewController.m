//
//  ScanViewController.m
//  TeamHRM
//
//  Created by Alexander O. Taraymovich on 13.03.13.
//  Copyright (c) 2013 Alexander O. Taraymovich. All rights reserved.
//

#import "ScanViewController.h"

@interface ScanViewController ()

@end

@implementation ScanViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Initialization code here.
    }
    
    return self;
}

-(IBAction)closeSheet:(id)sender
{
    [NSApp endSheet:self.view.window returnCode:NSAlertDefaultReturn];
    [self.view.window orderOut:self];
}

@end
