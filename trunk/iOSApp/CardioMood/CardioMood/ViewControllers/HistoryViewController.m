//
//  HistoryViewController.m
//  CardioMood
//
//  Created by Yuriy Pogrebnyak on 20.04.13.
//  Copyright (c) 2013 Alexander O. Taraymovich. All rights reserved.
//

#import "HistoryViewController.h"

@interface HistoryViewController ()

@end

@implementation HistoryViewController

@synthesize mainWebView;

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    [mainWebView loadHTMLString:@"<html><body><h1>History body</h1></body></html>" baseURL:nil];
	// Do any additional setup after loading the view.
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

@end
