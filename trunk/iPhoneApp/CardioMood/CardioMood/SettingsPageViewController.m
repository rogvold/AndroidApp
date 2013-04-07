//
//  SettingsPageViewController.m
//  CardioMood
//
//  Created by Alexander O. Taraymovich on 05.04.13.
//  Copyright (c) 2013 Alexander O. Taraymovich. All rights reserved.
//

#import "SettingsPageViewController.h"
#import "CMLabel.h"
#import <User.h>
#import <ClientServerInteraction.h>

@interface SettingsPageViewController () <UITextFieldDelegate, UIPickerViewDelegate, UIPickerViewDataSource>

@end

@implementation SettingsPageViewController


- (id)initWithStyle:(UITableViewStyle)style
{
    self = [super initWithStyle:style];
    if (self) {
        // Custom initialization
    }
    return self;
}

-(void)viewDidLoad
{
    [super viewDidLoad];
}

- (NSArray*)currentSettings:(NSInteger)index {
    NSArray *keys = [settings allKeys];
    NSString *curentKey = keys[index];
    NSArray *currentSettings = settings[curentKey];
    return currentSettings;
}

@synthesize datePicker;
@synthesize sexPicker;

- (void)initializeData
{
    self.usernameField = [[UITextField alloc] initWithFrame:CGRectMake(80, 12, 215, 30)];
    self.passwordField = [[UITextField alloc] initWithFrame:CGRectMake(80, 12, 215, 30)];
    self.firstNameField = [[UITextField alloc] initWithFrame:CGRectMake(80, 12, 215, 30)];
    self.lastNameField = [[UITextField alloc] initWithFrame:CGRectMake(80, 12, 215, 30)];
    self.heightField = [[UITextField alloc] initWithFrame:CGRectMake(80, 12, 215, 30)];
    self.weightField = [[UITextField alloc] initWithFrame:CGRectMake(80, 12, 215, 30)];
    self.dateLabel = [[CMLabel alloc] initWithFrame:CGRectMake(80, 7, 215, 30)];
    self.sexLabel = [[CMLabel alloc] initWithFrame:CGRectMake(80, 7, 215, 30)];
}

- (void)initField:(UITextField *)textField section:(NSInteger)section row:(NSInteger)row
{
    NSArray *currentSettings = [self currentSettings:section];
    textField.text = currentSettings[row];
    textField.autocorrectionType = UITextAutocorrectionTypeNo; // no auto correction support
    textField.autocapitalizationType = UITextAutocapitalizationTypeNone; // no auto capitalization support
    
    textField.clearButtonMode = UITextFieldViewModeNever; // no clear 'x' button to the right
    [textField setEnabled: YES];
    [textField setFont:[UIFont fontWithName:@"Arial-BoldMT" size:18]];
}

/*
 // Override to allow orientations other than the default portrait orientation.
 - (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation {
 // Return YES for supported orientations.
 return (interfaceOrientation == UIInterfaceOrientationPortrait);
 }
 */

// Customize the number of sections in the table view.
- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
    return [settings count];
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    NSArray *currentSettings = [self currentSettings:section];
    return [currentSettings count];
}

- (NSString *)tableView:(UITableView *)tableView titleForHeaderInSection:(NSInteger)section {
    
    return [settings allKeys][section];
}

// Customize the appearance of table view cells.
- (IBAction)dateChanged:(id)sender
{
    CMLabel *textLabel = (CMLabel *)[self.tableView viewWithTag:6];
    NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
    [formatter setDateFormat:@"MM/dd/yyyy"];
    
    NSString *stringFromDate = [formatter stringFromDate:[datePicker date]];
    textLabel.text = stringFromDate;
    self.user.birthDate = stringFromDate;
    self.dateLabel = textLabel;
    NSMutableArray *currentSettings = settings[@"Physical parameters"];
    currentSettings[2] = textLabel.text;
    [textLabel endEditing:YES];
}

- (IBAction)sexChanged:(id)sender
{
    CMLabel *textLabel = (CMLabel *)[self.tableView viewWithTag:7];
    textLabel.text = titleSex[[sexPicker selectedRowInComponent:0]];
    self.user.sex = [NSNumber numberWithInt:[textLabel.text isEqual:@"Male"] ? 1 : 0];
    self.sexLabel = textLabel;
    NSMutableArray *currentSettings = settings[@"Physical parameters"];
    currentSettings[3] = textLabel.text;
    [textLabel endEditing:YES];
}

- (BOOL)textFieldShouldReturn:(UITextField *)textField {
    [textField resignFirstResponder];
    return NO;
}

- (NSInteger)numberOfComponentsInPickerView:(UIPickerView *)thePickerView {
    return 1;
}

- (NSInteger)pickerView:(UIPickerView *)thePickerView numberOfRowsInComponent:(NSInteger)component {
    
    return [titleSex count];
}

- (UIView *)pickerView:(UIPickerView *)pickerView viewForRow:(NSInteger)row forComponent:(NSInteger)component reusingView:(UIView *)view
{
    UILabel *titleLabel = [[UILabel alloc] initWithFrame:CGRectMake(100, 0, 100, 32)];
    titleLabel.text = titleSex[row];
    [titleLabel setFont:[UIFont fontWithName:@"Arial-BoldMT" size:18]];
    titleLabel.backgroundColor = [UIColor clearColor];
    
    UIImage *img = [UIImage imageNamed:[NSString stringWithFormat:@"%@",imageSex[row]]];
    
    UIImageView *icon = [[UIImageView alloc] initWithImage:img];
    icon.frame = CGRectMake(30, 0, 30, 30);
    
    UIView *tmpView = [[UIView alloc] initWithFrame:CGRectMake(0, 0, 290, 32)];
    [tmpView insertSubview:icon atIndex:0];
    [tmpView insertSubview:titleLabel atIndex:0];
    [tmpView setUserInteractionEnabled:NO];
    [tmpView setTag:row];
    return tmpView;
}

// Customize the appearance of table view cells.
- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    NSString *CellIdentifier = [NSString stringWithFormat:@"Cell%d%d",indexPath.section, indexPath.row];
    
    UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:CellIdentifier];
    if (cell == nil) {
        cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:CellIdentifier];
    }
    cell.accessoryType = UITableViewCellAccessoryNone;
    
    cell.textLabel.textColor = [UIColor colorWithRed:50.0 / 255.0 green:79.0 / 255.0 blue:133.0 / 255.0 alpha:1.0];
    
    [cell.textLabel setFont:[UIFont fontWithName:@"Arial-BoldMT" size:12]];
    UITextField *textField;
    CMLabel *textLabel;
    
    if (indexPath.section == 0 && indexPath.row == 0)
    {
        cell.textLabel.text = @"username";
        textField = self.usernameField;
        [self initField:textField section:indexPath.section row:indexPath.row];
        if (!self.isUsernameEditable)
        {
            [textField setEnabled:NO];
            textField.textColor = [UIColor grayColor];
        }
        textField.keyboardType = UIKeyboardTypeEmailAddress;
        textField.returnKeyType = UIReturnKeyDone;
        textField.tag = 0;
        textField.accessibilityIdentifier = @"username";
    }
    if (indexPath.section == 0 && indexPath.row == 1)
    {
        textField = self.passwordField;
        [self initField:textField section:indexPath.section row:indexPath.row];
        textField.keyboardType = UIKeyboardTypeDefault;
        textField.returnKeyType = UIReturnKeyDone;
        textField.secureTextEntry = YES;
        textField.tag = 1;
        cell.textLabel.text = @"password";
    }
    
    if (indexPath.section == 1 && indexPath.row == 0)
    {
        textField = self.firstNameField;
        [self initField:textField section:indexPath.section row:indexPath.row];
        textField.keyboardType = UIKeyboardTypeDefault;
        textField.returnKeyType = UIReturnKeyDone;
        
        cell.textLabel.text = @"first name";
        textField.tag = 2;
    }
    if (indexPath.section == 1 && indexPath.row == 1)
    {
        textField = self.lastNameField;
        [self initField:textField section:indexPath.section row:indexPath.row];
        textField.keyboardType = UIKeyboardTypeDefault;
        textField.returnKeyType = UIReturnKeyDone;
        
        cell.textLabel.text = @"last name";
        textField.tag = 3;
    }
    
    if (indexPath.section == 2 && indexPath.row == 0)
    {
        textField = self.heightField;
        [self initField:textField section:indexPath.section row:indexPath.row];
        textField.keyboardType = UIKeyboardTypeNumbersAndPunctuation;
        textField.returnKeyType = UIReturnKeyDone;
        
        cell.textLabel.text = @"height";
        textField.tag = 4;
    }
    if (indexPath.section == 2 && indexPath.row == 1)
    {
        textField = self.weightField;
        [self initField:textField section:indexPath.section row:indexPath.row];
        textField.keyboardType = UIKeyboardTypeNumbersAndPunctuation;
        textField.returnKeyType = UIReturnKeyDone;
        
        cell.textLabel.text = @"weight";
        textField.tag = 5;
    }
    if (indexPath.section == 2 && indexPath.row == 2)
    {
        cell.textLabel.text = @"birth date";
        textLabel = self.dateLabel;
        textLabel.tag = 6;
        [textLabel setBackgroundColor:[UIColor clearColor]];
        NSArray *currentSettings = [self currentSettings:indexPath.section];
        textLabel.text = currentSettings[indexPath.row];
        [textLabel setFont:[UIFont fontWithName:@"Arial-BoldMT" size:18]];
        datePicker = [[UIDatePicker alloc] initWithFrame:CGRectMake(0, 40, 0, 0)];
        datePicker.datePickerMode = UIDatePickerModeDate;
        datePicker.maximumDate = [NSDate date];
        datePicker.hidden = NO;
        NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
        [formatter setDateFormat:@"MM/dd/yyyy"];
        if (![textLabel.text isEqual:@""])
        {
            datePicker.date = [formatter dateFromString:textLabel.text];
        }
        else
        {
            datePicker.date = [NSDate date];
        }
        [textLabel setInputView:datePicker];
    }
    if (indexPath.section == 2 && indexPath.row == 3)
    {
        cell.textLabel.text = @"sex";
        textLabel = self.sexLabel;
        textLabel.tag = 7;
        [textLabel setBackgroundColor:[UIColor clearColor]];
        NSArray *currentSettings = [self currentSettings:indexPath.section];
        textLabel.text = currentSettings[indexPath.row];
        [textLabel setFont:[UIFont fontWithName:@"Arial-BoldMT" size:18]];
        sexPicker = [[UIPickerView alloc] initWithFrame:CGRectMake(0, 44, 0, 0)];
        sexPicker.hidden = NO;
        sexPicker.showsSelectionIndicator = YES;
        sexPicker.delegate = self;
        sexPicker.dataSource = self;
        if (![textLabel.text isEqual:@""])
        {
            [sexPicker selectRow:[textLabel.text isEqual:@"Male"] ? 0 : 1 inComponent:0 animated:YES];
        }
        else
        {
            [sexPicker selectRow:0 inComponent:0 animated:YES];
        }
        [textLabel setInputView:sexPicker];
    }
    
    UIToolbar *doneBar = [[UIToolbar alloc] initWithFrame:CGRectMake(0, 0, 320, 44)];
    [doneBar setBarStyle:UIBarStyleBlackTranslucent];
    UIBarButtonItem *spacer = [[UIBarButtonItem alloc]    initWithBarButtonSystemItem:UIBarButtonSystemItemFlexibleSpace target:nil action:nil];
    UIBarButtonItem *doneButton = [[UIBarButtonItem alloc] initWithTitle:@"Done" style:UIBarButtonItemStyleDone target:nil action:nil];
    
    if (indexPath.section != 2 || (indexPath.section == 2 && indexPath.row != 2 && indexPath.row != 3))
    {
        [doneBar setItems:@[spacer,
                           doneButton] animated:YES];
        [doneButton setTarget:textField];
        [doneButton setAction:@selector(resignFirstResponder)];
        [textField setInputAccessoryView:doneBar];
        
        textField.delegate = self;
        
        [cell.contentView addSubview:textField];
    }
    else
    {
        [doneBar setItems:@[spacer,
                           doneButton] animated:YES];
        [doneButton setTarget:self];
        if (indexPath.row == 2)
        {
            [doneButton setAction:@selector(dateChanged:)];
        }
        if (indexPath.row == 3)
        {
            [doneButton setAction:@selector(sexChanged:)];
        }
        [textLabel setInputAccessoryView:doneBar];
        
        [cell.contentView addSubview:textLabel];
    }
    [cell setSelectionStyle:UITableViewCellSelectionStyleNone];
    
    return cell;
}

- (void)textFieldDidEndEditing:(UITextField *)textField
{
    NSMutableArray *currentSettings;
    switch (textField.tag) {
        case 0:
            self.user.email = textField.text;
            currentSettings = settings[@"Authorization data"];
            currentSettings[0] = textField.text;
            break;
        case 1:
            self.user.password = textField.text;
            currentSettings = settings[@"Authorization data"];
            currentSettings[1] = textField.text;
            break;
        case 2:
            self.user.firstName = textField.text;
            currentSettings = settings[@"Personal data"];
            currentSettings[0] = textField.text;
            break;
        case 3:
            self.user.lastName = textField.text;
            currentSettings = settings[@"Personal data"];
            currentSettings[1] = textField.text;
            break;
        case 4:
            self.user.height = [NSNumber numberWithInt:[textField.text intValue]];
            currentSettings = settings[@"Physical parameters"];
            currentSettings[0] = textField.text;
            break;
        case 5:
            self.user.weight = [NSNumber numberWithInt:[textField.text intValue]];
            currentSettings = settings[@"Physical parameters"];
            currentSettings[1] = textField.text;
            break;
        default:
            break;
    }
}

@end
