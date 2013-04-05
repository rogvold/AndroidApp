//
//  SettingsPageViewController.h
//  CardioMood
//
//  Created by Alexander O. Taraymovich on 05.04.13.
//  Copyright (c) 2013 Alexander O. Taraymovich. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <User.h>
#import "CMLabel.h"

@interface SettingsPageViewController : UITableViewController {
    NSMutableDictionary *settings;
    NSMutableArray *titleSex;
    NSMutableArray *imageSex;
}

@property (nonatomic, strong) User *user;
@property (nonatomic, strong) IBOutlet UIBarButtonItem *saveButton;
@property (nonatomic, strong) UIDatePicker *datePicker;
@property (nonatomic, strong) UIPickerView *sexPicker;
@property (nonatomic, strong) UITextField *usernameField;
@property (nonatomic, strong) UITextField *passwordField;
@property (nonatomic, strong) UITextField *firstNameField;
@property (nonatomic, strong) UITextField *lastNameField;
@property (nonatomic, strong) UITextField *heightField;
@property (nonatomic, strong) UITextField *weightField;
@property (nonatomic, strong) CMLabel *dateLabel;
@property (nonatomic, strong) CMLabel *sexLabel;
@property (nonatomic) BOOL isUsernameEditable;

- (NSArray*)currentSettings:(NSInteger)index;
- (void)initializeData;
- (void)initField:(UITextField *)textField section:(NSInteger)section row:(NSInteger)row;
- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView;
- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section;
- (NSString *)tableView:(UITableView *)tableView titleForHeaderInSection:(NSInteger)section;
- (IBAction)dateChanged:(id)sender;
- (IBAction)sexChanged:(id)sender;
- (BOOL)textFieldShouldReturn:(UITextField *)textField;
- (NSInteger)numberOfComponentsInPickerView:(UIPickerView *)thePickerView;
- (NSInteger)pickerView:(UIPickerView *)thePickerView numberOfRowsInComponent:(NSInteger)component;
- (UIView *)pickerView:(UIPickerView *)pickerView viewForRow:(NSInteger)row forComponent:(NSInteger)component reusingView:(UIView *)view;
- (void)textFieldDidEndEditing:(UITextField *)textField;
- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath;

@end
