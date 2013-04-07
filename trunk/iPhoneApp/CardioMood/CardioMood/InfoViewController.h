//
//  InfoViewController.h
//  CardioMood
//
//  Created by Alexander O. Taraymovich on 06.04.13.
//  Copyright (c) 2013 Alexander O. Taraymovich. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface InfoViewController : UIViewController

@property (nonatomic, strong) IBOutlet UIButton *logOutButton;

-(IBAction)logOutButtonPressed:(id)sender;

@end
