//
//  StatViewController.m
//  CardioMood
//
//  Created by Alexander O. Taraymovich on 07.04.13.
//  Copyright (c) 2013 Alexander O. Taraymovich. All rights reserved.
//

#import "StatViewController.h"
#import "KeychainItemWrapper.h"
#import <ClientServerInteraction.h>
#import "SessionDetailViewController.h"

@interface StatViewController ()

@end

@implementation StatViewController

- (id)initWithStyle:(UITableViewStyle)style
{
    self = [super initWithStyle:style];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    [UIApplication sharedApplication].networkActivityIndicatorVisible = YES;
    UIActivityIndicatorView *activityIndicator =
    [[UIActivityIndicatorView alloc] initWithFrame:CGRectMake(0, 0, 20, 20)];
    [[self navigationItem] setTitleView:activityIndicator];
    [activityIndicator startAnimating];
    KeychainItemWrapper *keychainItem = [[KeychainItemWrapper alloc] initWithIdentifier:@"CardioMood" accessGroup:nil];
    NSString *password = [keychainItem objectForKey:CFBridgingRelease(kSecValueData)];
    NSString *username = [keychainItem objectForKey:CFBridgingRelease(kSecAttrAccount)];
    NSString *token = [keychainItem objectForKey:CFBridgingRelease(kSecAttrLabel)];
    [ClientServerInteraction getAllSessions:token completion:^(int code, NSArray *response, NSError *error, ServerResponseError *serverError) {
        if (code == 1)
        {
            sessions = response;
            [activityIndicator stopAnimating];
            [self.tableView reloadData];
        }
        else if (code == 3)
        {
            if (serverError.errorCode == InvalidToken)
            {
                [ClientServerInteraction authorizeWithEmail:username withPassword:password withDeviceId:[[[UIDevice currentDevice] identifierForVendor] UUIDString] completion:^(int code, AccessToken *response, NSError *error, ServerResponseError *serverError) {
                    [keychainItem setObject:[response token] forKey:CFBridgingRelease(kSecAttrLabel)];
                    [ClientServerInteraction getAllSessions:token completion:^(int code, NSArray *response, NSError *error, ServerResponseError *serverError) {
                        if (code == 1)
                        {
                            sessions = response;
                            [activityIndicator stopAnimating];
                            [self.tableView reloadData];
                        }
                    }];
                }];
            }
        }
    }];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

#pragma mark - Table view data source

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
    return 1;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    return [sessions count];
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    NSString *CellIdentifier = [NSString stringWithFormat:@"Cell%d", indexPath.row];
    UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:CellIdentifier];
    
    if (cell == nil) {
        
        cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleSubtitle  reuseIdentifier:CellIdentifier];
        
    }
    
    Session *currentSession = sessions[indexPath.row];
    
    NSDate *sessionDate = [NSDate dateWithTimeIntervalSince1970:[currentSession.start doubleValue] / 1000];
    NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
    [formatter setDateFormat:@"MM/dd/yyyy HH:mm:ss"];
    
    cell.textLabel.text = [formatter stringFromDate:sessionDate];
    
    return cell;
}

/*
 // Override to support conditional editing of the table view.
 - (BOOL)tableView:(UITableView *)tableView canEditRowAtIndexPath:(NSIndexPath *)indexPath
 {
 // Return NO if you do not want the specified item to be editable.
 return YES;
 }
 */

/*
 // Override to support editing the table view.
 - (void)tableView:(UITableView *)tableView commitEditingStyle:(UITableViewCellEditingStyle)editingStyle forRowAtIndexPath:(NSIndexPath *)indexPath
 {
 if (editingStyle == UITableViewCellEditingStyleDelete) {
 // Delete the row from the data source
 [tableView deleteRowsAtIndexPaths:@[indexPath] withRowAnimation:UITableViewRowAnimationFade];
 }
 else if (editingStyle == UITableViewCellEditingStyleInsert) {
 // Create a new instance of the appropriate class, insert it into the array, and add a new row to the table view
 }
 }
 */

/*
 // Override to support rearranging the table view.
 - (void)tableView:(UITableView *)tableView moveRowAtIndexPath:(NSIndexPath *)fromIndexPath toIndexPath:(NSIndexPath *)toIndexPath
 {
 }
 */

/*
 // Override to support conditional rearranging of the table view.
 - (BOOL)tableView:(UITableView *)tableView canMoveRowAtIndexPath:(NSIndexPath *)indexPath
 {
 // Return NO if you do not want the item to be re-orderable.
 return YES;
 }
 */

#pragma mark - Table view delegate

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    // Navigation logic may go here. Create and push another view controller.
    
    SessionDetailViewController *detailViewController = [[SessionDetailViewController alloc] initWithStyle:UITableViewStyleGrouped];
    detailViewController.session = sessions[indexPath.row];
    [self.navigationController pushViewController:detailViewController animated:YES];
}

@end
