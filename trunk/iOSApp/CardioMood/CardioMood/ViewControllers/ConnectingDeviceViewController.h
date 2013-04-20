//
//  ConnectingDeviceViewController.h
//  CardioMood
//
//  Created by Yuriy Pogrebnyak on 20.04.13.
//  Copyright (c) 2013 Alexander O. Taraymovich. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface ConnectingDeviceViewController : UIViewController<UITableViewDelegate, UITableViewDataSource>
@property (strong, nonatomic) IBOutlet UITableView* devicesTableView;

@end
