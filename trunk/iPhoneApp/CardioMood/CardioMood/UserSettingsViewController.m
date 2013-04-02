//
//  UserSettingsViewController.m
//  CardioMood
//
//  Created by Alexander O. Taraymovich on 02.04.13.
//  Copyright (c) 2013 Alexander O. Taraymovich. All rights reserved.
//

#import "UserSettingsViewController.h"
#import <ClientServerInteraction.h>

@interface UserSettingsViewController () <UITextFieldDelegate, UIPickerViewDelegate>

@end

@implementation UserSettingsViewController

- (id)initWithStyle:(UITableViewStyle)style
{
    self = [super initWithStyle:style];
    if (self) {
        // Custom initialization
    }
    return self;
}

@synthesize settings;
@synthesize datePicker;
@synthesize sexPicker;
@synthesize titleSex;
@synthesize imageSex;

- (NSArray*)currentSettings:(NSInteger)index {
    NSArray *keys = [settings allKeys];
    NSString *curentKey = [keys objectAtIndex:index];
    NSArray *currentSettings = [settings objectForKey:curentKey];
    return currentSettings;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    NSArray *authorizationData = [NSArray arrayWithObjects:@"taraymovich@gmail.com", @"a1l2e3x4", nil];
    NSArray *personalData = [NSArray arrayWithObjects:@"Alexander", @"Taraymovich", nil];
    NSArray *physicalParameters = [NSArray arrayWithObjects:@"185", @"90", @"08/24/1992", @"Male", nil];
    
    settings = [NSDictionary dictionaryWithObjectsAndKeys:physicalParameters, @"Physical parameters", authorizationData, @"Authorization data", personalData, @"Personal data", nil];
    titleSex = [[NSMutableArray alloc] init];
    [titleSex addObject:@"Male"];
    [titleSex addObject:@"Female"];
    imageSex = [[NSMutableArray alloc] init];
    [imageSex addObject:@"male"];
    [imageSex addObject:@"female"];
}

- (void)viewWillAppear:(BOOL)animated
{
    [super viewWillAppear:animated];
}

- (void)viewDidAppear:(BOOL)animated
{
    [super viewDidAppear:animated];
}

- (void)viewWillDisappear:(BOOL)animated
{
	[super viewWillDisappear:animated];
}

- (void)viewDidDisappear:(BOOL)animated
{
	[super viewDidDisappear:animated];
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
    
    return [[settings allKeys] objectAtIndex:section];
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
    
    UITextField *textField = [[UITextField alloc] initWithFrame:CGRectMake(80, 12, 215, 30)];
    //textField.backgroundColor = [UIColor whiteColor];
    textField.autocorrectionType = UITextAutocorrectionTypeNo; // no auto correction support
    textField.autocapitalizationType = UITextAutocapitalizationTypeNone; // no auto capitalization support
    //textField.tag = 0;
    
    textField.clearButtonMode = UITextFieldViewModeNever; // no clear 'x' button to the right
    [textField setEnabled: YES];
    
    NSArray *currentSettings = [self currentSettings:indexPath.section];
    //cell.textLabel.text = [currentSettings objectAtIndex:indexPath.row];
    textField.text = [currentSettings objectAtIndex:indexPath.row];
    [textField setFont:[UIFont fontWithName:@"Arial-BoldMT" size:18]];
    cell.textLabel.textColor = [UIColor colorWithRed:50.0 / 255.0 green:79.0 / 255.0 blue:133.0 / 255.0 alpha:1.0];
    
    [cell.textLabel setFont:[UIFont fontWithName:@"Arial-BoldMT" size:12]];
    
    if (indexPath.section == 0 && indexPath.row == 0)
    {
        cell.textLabel.text = @"username";
        [textField setEnabled:NO];
        textField.textColor = [UIColor grayColor];
        textField.keyboardType = UIKeyboardTypeEmailAddress;
        textField.returnKeyType = UIReturnKeyDone;
        textField.accessibilityIdentifier = @"username";
    }
    if (indexPath.section == 0 && indexPath.row == 1)
    {
        textField.keyboardType = UIKeyboardTypeDefault;
        textField.returnKeyType = UIReturnKeyDone;
        textField.secureTextEntry = YES;
        
        cell.textLabel.text = @"password";
        textField.accessibilityIdentifier = @"password";
    }
    
    if (indexPath.section == 1 && indexPath.row == 0)
    {
        textField.keyboardType = UIKeyboardTypeDefault;
        textField.returnKeyType = UIReturnKeyDone;
        
        cell.textLabel.text = @"first name";
        textField.accessibilityIdentifier = @"firstName";
    }
    if (indexPath.section == 1 && indexPath.row == 1)
    {
        textField.keyboardType = UIKeyboardTypeDefault;
        textField.returnKeyType = UIReturnKeyDone;
        
        cell.textLabel.text = @"last name";
        textField.accessibilityIdentifier = @"lastName";
    }
    
    if (indexPath.section == 2 && indexPath.row == 0)
    {
        textField.keyboardType = UIKeyboardTypeNumbersAndPunctuation;
        textField.returnKeyType = UIReturnKeyDone;
        
        cell.textLabel.text = @"height";
        textField.accessibilityIdentifier = @"height";
    }
    if (indexPath.section == 2 && indexPath.row == 1)
    {
        textField.keyboardType = UIKeyboardTypeNumbersAndPunctuation;
        textField.returnKeyType = UIReturnKeyDone;
        
        cell.textLabel.text = @"weight";
        textField.accessibilityIdentifier = @"weight";
    }
    if (indexPath.section == 2 && indexPath.row == 2)
    {
        cell.textLabel.text = @"birth date";
        textField.accessibilityIdentifier = @"birthDate";
        datePicker = [[UIDatePicker alloc] initWithFrame:CGRectMake(0, 44, 0, 0)];
        datePicker.datePickerMode = UIDatePickerModeDate;
        datePicker.maximumDate = [NSDate date];
        datePicker.hidden = NO;
        NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
        [formatter setDateFormat:@"MM/dd/yyyy"];
        datePicker.date = [formatter dateFromString:textField.text];
        [textField setInputView:datePicker];
    }
    if (indexPath.section == 2 && indexPath.row == 3)
    {
        cell.textLabel.text = @"sex";
        textField.accessibilityIdentifier = @"sex";
        sexPicker = [[UIPickerView alloc] initWithFrame:CGRectMake(0, 44, 0, 0)];
        sexPicker.hidden = NO;
        sexPicker.showsSelectionIndicator = YES;
        sexPicker.delegate = self;
        [textField setInputView:sexPicker];
    }
    
    textField.delegate = self;
    
    [cell.contentView addSubview:textField];
    [cell setSelectionStyle:UITableViewCellSelectionStyleNone];
    
    return cell;
}

- (BOOL)textFieldShouldReturn:(UITextField *)textField {
    [textField resignFirstResponder];
    return NO;
}

- (BOOL)textFieldShouldEndEditing:(UITextField *)textField
{
    if ([textField.accessibilityIdentifier isEqual:@"birthDate"])
    {
        NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
        [formatter setDateFormat:@"MM/dd/yyyy"];
        
        NSString *stringFromDate = [formatter stringFromDate:[datePicker date]];
        textField.text = stringFromDate;
    }
    if ([textField.accessibilityIdentifier isEqual:@"sex"])
    {
        
        textField.text = [titleSex objectAtIndex:[sexPicker selectedRowInComponent:0]];
    }
    [self.tableView reloadData];
    return YES;
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
    titleLabel.text = [titleSex objectAtIndex:row];
    [titleLabel setFont:[UIFont fontWithName:@"Arial-BoldMT" size:18]];
    titleLabel.backgroundColor = [UIColor clearColor];
    
    UIImage *img = [UIImage imageNamed:[NSString stringWithFormat:@"%@",[imageSex objectAtIndex:row]]];
    
    UIImageView *icon = [[UIImageView alloc] initWithImage:img];
    icon.frame = CGRectMake(30, 0, 30, 30);
    
    UIView *tmpView = [[UIView alloc] initWithFrame:CGRectMake(0, 0, 290, 32)];
    [tmpView insertSubview:icon atIndex:0];
    [tmpView insertSubview:titleLabel atIndex:0];
    [tmpView setUserInteractionEnabled:NO];
    [tmpView setTag:row];
    return tmpView;
}

@end
