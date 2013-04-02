//
//  UserSettingsViewController.h
//  CardioMood
//
//  Created by Alexander O. Taraymovich on 02.04.13.
//  Copyright (c) 2013 Alexander O. Taraymovich. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface UserSettingsViewController : UITableViewController {
    NSDictionary *settings;
}

@property (nonatomic, retain) NSDictionary *settings;
@property (nonatomic, retain) NSMutableArray *titleSex;
@property (nonatomic, retain) NSMutableArray *imageSex;
@property (nonatomic, strong) UIDatePicker *datePicker;
@property (nonatomic, strong) UIPickerView *sexPicker;

- (NSArray*)currentSettings:(NSInteger)index;

@end
