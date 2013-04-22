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
#import "AppDelegate.h"

@interface StatViewController ()

@end

@implementation StatViewController

@synthesize keychainItem;
@synthesize username;
@synthesize password;
@synthesize token;

- (id)initWithStyle:(UITableViewStyle)style
{
    self = [super initWithStyle:style];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)viewDidAppear:(BOOL)animated
{
    [super viewDidAppear:animated];
    AppDelegate *appDelegate = (AppDelegate *)[[UIApplication sharedApplication] delegate];
    managedObjectContext = appDelegate.managedObjectContext;
    self.user = (LocalUser *)[NSEntityDescription insertNewObjectForEntityForName:@"LocalUser" inManagedObjectContext:managedObjectContext];
    keychainItem = [[KeychainItemWrapper alloc] initWithIdentifier:@"CardioMood" accessGroup:nil];
    password = [keychainItem objectForKey:CFBridgingRelease(kSecValueData)];
    username = [keychainItem objectForKey:CFBridgingRelease(kSecAttrAccount)];
    token = [keychainItem objectForKey:CFBridgingRelease(kSecAttrLabel)];
    [self updateUI];
    [ClientServerInteraction checkIfServerIsReachable:^(bool response) {
        if (response)
        {
            [ClientServerInteraction getInfo:token completion:^(int code, User *response, NSError *error, ServerResponseError *serverError) {
                if (code == 1)
                {
                    [self.user setUserId:response.userId];
                    if ([self fetchSessions])
                    {
                        [self performSelectorInBackground:@selector(syncLocalData) withObject:nil];
                        
                    }
                }
                else if (code == 3)
                {
                    if (serverError.errorCode == InvalidToken)
                    {
                        [ClientServerInteraction authorizeWithEmail:username withPassword:password withDeviceId:[[[UIDevice currentDevice] identifierForVendor] UUIDString] completion:^(int code, AccessToken *response, NSError *error, ServerResponseError *serverError) {
                            [keychainItem setObject:[response token] forKey:CFBridgingRelease(kSecAttrLabel)];
                            [ClientServerInteraction getInfo:token completion:^(int code, User *response, NSError *error, ServerResponseError *serverError) {
                                if (code == 1)
                                {
                                    [self.user setUserId:response.userId];
                                    if ([self fetchSessions])
                                    {
                                        [self performSelectorInBackground:@selector(syncLocalData) withObject:nil];
                                    }
                                }
                            }];
                        }];
                    }
                }
            }];
        }
    }];
}

-(void)updateUI
{
    [UIApplication sharedApplication].networkActivityIndicatorVisible = YES;
    UIActivityIndicatorView *activityIndicator =
    [[UIActivityIndicatorView alloc] initWithFrame:CGRectMake(0, 0, 20, 20)];
    [[self navigationItem] setTitleView:activityIndicator];
    [activityIndicator startAnimating];
    token = [keychainItem objectForKey:CFBridgingRelease(kSecAttrLabel)];
    [ClientServerInteraction getAllSessions:token completion:^(int code, NSArray *response, NSError *error, ServerResponseError *serverError) {
        if (code == 1)
        {
            sessions = response;
            [UIApplication sharedApplication].networkActivityIndicatorVisible = NO;
            [activityIndicator stopAnimating];
            [[self navigationItem] setTitleView:nil];
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
                            [UIApplication sharedApplication].networkActivityIndicatorVisible = NO;
                            [activityIndicator stopAnimating];
                            [[self navigationItem] setTitleView:nil];
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

- (BOOL)fetchSessions
{
    // Define our table/entity to use
    NSEntityDescription *entity = [NSEntityDescription entityForName:@"LocalSession" inManagedObjectContext:managedObjectContext];
    // Setup the fetch request
    NSFetchRequest *request = [[NSFetchRequest alloc] init];
    [request setEntity:entity];
    // Fetch the records and handle an error
    NSError *error;
    NSMutableArray *mutableFetchResults = [[managedObjectContext executeFetchRequest:request error:&error] mutableCopy];
    if (!mutableFetchResults) {
        // Handle the error.
        // This is a serious error and should advise the user to restart the application
    }
    NSPredicate *predicate = [NSPredicate predicateWithFormat:@"self.user.userId == %@", self.user.userId];
    NSArray *fetchResults = [mutableFetchResults filteredArrayUsingPredicate:predicate];
    // Save our fetched data to an array
    [self setLocalUserSessions: fetchResults];
    if ([fetchResults count] > 0)
    {
        return YES;
    }
    else
    {
        return NO;
    }
}

-(void)syncLocalData
{
    for (LocalSession *session in self.localUserSessions)
    {
        token = [keychainItem objectForKey:CFBridgingRelease(kSecAttrLabel)];
        [ClientServerInteraction syncRates:[session.rates componentsSeparatedByString:@","] start:[NSNumber numberWithLongLong:(long long)[session.startTime timeIntervalSince1970] * 1000] create:[NSNumber numberWithInt:1] token:token completion:^(int code, NSNumber *response, NSError *error, ServerResponseError *serverError) {
            if (code == 1)
            {
                [managedObjectContext deleteObject:session];
                NSError *error;
                @synchronized(self.managedObjectContext)
                {
                    if(![self.managedObjectContext save:&error]){
                        //This is a serious error saying the record
                        //could not be saved. Advise the user to
                        //try again or restart the application.
                    }
                }
                [self updateUI];
            }
            else if (code == 3)
            {
                if (serverError.errorCode == InvalidToken)
                {
                    [ClientServerInteraction authorizeWithEmail:username withPassword:password withDeviceId:[[[UIDevice currentDevice] identifierForVendor] UUIDString] completion:^(int code, AccessToken *response, NSError *error, ServerResponseError *serverError) {
                        [keychainItem setObject:[response token] forKey:CFBridgingRelease(kSecAttrLabel)];
                        [ClientServerInteraction syncRates:[session.rates componentsSeparatedByString:@","] start:[NSNumber numberWithLongLong:(long long)[session.startTime timeIntervalSince1970] * 1000] create:[NSNumber numberWithInt:1] token:token completion:^(int code, NSNumber *response, NSError *error, ServerResponseError *serverError) {
                            if (code == 1)
                            {
                                [managedObjectContext deleteObject:session];
                                NSError *error;
                                @synchronized(self.managedObjectContext)
                                {
                                    if(![self.managedObjectContext save:&error]){
                                        //This is a serious error saying the record
                                        //could not be saved. Advise the user to
                                        //try again or restart the application.
                                    }
                                }
                                [self updateUI];
                            }
                        }];
                    }];
                }
            }
        }];
    }
}

@end
