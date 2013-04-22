/*
     File: HeartRateMonitorAppDelegate.h
 Abstract: Interface file for Heart Rate Monitor app using Bluetooth Low Energy (LE) Heart Rate Service. This app demonstrats the use of CoreBluetooth APIs for LE devices.
  Version: 1.0
 
 Disclaimer: IMPORTANT:  This Apple software is supplied to you by Apple
 Inc. ("Apple") in consideration of your agreement to the following
 terms, and your use, installation, modification or redistribution of
 this Apple software constitutes acceptance of these terms.  If you do
 not agree with these terms, please do not use, install, modify or
 redistribute this Apple software.
 
 In consideration of your agreement to abide by the following terms, and
 subject to these terms, Apple grants you a personal, non-exclusive
 license, under Apple's copyrights in this original Apple software (the
 "Apple Software"), to use, reproduce, modify and redistribute the Apple
 Software, with or without modifications, in source and/or binary forms;
 provided that if you redistribute the Apple Software in its entirety and
 without modifications, you must retain this notice and the following
 text and disclaimers in all such redistributions of the Apple Software.
 Neither the name, trademarks, service marks or logos of Apple Inc. may
 be used to endorse or promote products derived from the Apple Software
 without specific prior written permission from Apple.  Except as
 expressly stated in this notice, no other rights or licenses, express or
 implied, are granted by Apple herein, including but not limited to any
 patent rights that may be infringed by your derivative works or by other
 works in which the Apple Software may be incorporated.
 
 The Apple Software is provided by Apple on an "AS IS" basis.  APPLE
 MAKES NO WARRANTIES, EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
 THE IMPLIED WARRANTIES OF NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS
 FOR A PARTICULAR PURPOSE, REGARDING THE APPLE SOFTWARE OR ITS USE AND
 OPERATION ALONE OR IN COMBINATION WITH YOUR PRODUCTS.
 
 IN NO EVENT SHALL APPLE BE LIABLE FOR ANY SPECIAL, INDIRECT, INCIDENTAL
 OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) ARISING IN ANY WAY OUT OF THE USE, REPRODUCTION,
 MODIFICATION AND/OR DISTRIBUTION OF THE APPLE SOFTWARE, HOWEVER CAUSED
 AND WHETHER UNDER THEORY OF CONTRACT, TORT (INCLUDING NEGLIGENCE),
 STRICT LIABILITY OR OTHERWISE, EVEN IF APPLE HAS BEEN ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 
 Copyright (C) 2011 Apple Inc. All Rights Reserved.
 
 */

#import <Cocoa/Cocoa.h>
#import <IOBluetooth/IOBluetooth.h>
#import "EMKeychain.h"

@interface HeartRateMonitorAppDelegate : NSObject <NSApplicationDelegate, CBCentralManagerDelegate, CBPeripheralDelegate> 
{
    NSWindow *window;
    NSArrayController *arrayController;
    
    CBCentralManager *manager;
    CBPeripheral *peripheral;
    
    NSMutableArray *heartRateMonitors;
    
    uint16_t heartRate;
    
    IBOutlet NSButton* connectButton;
    BOOL autoConnect; 
}

@property (strong) IBOutlet NSWindow *window;
@property (strong) IBOutlet NSArrayController *arrayController;
@property (strong) IBOutlet NSTextField *loginField;
@property (strong) IBOutlet NSSecureTextField *passwordField;
@property (strong) IBOutlet NSButton *loginButton;
@property (strong) IBOutlet NSButton *connectButton;

@property (strong) IBOutlet NSTextField *heartRateLabel;
@property (strong) IBOutlet NSTextField *bpmLabel;
@property (strong) IBOutlet NSScrollView *sensors;

@property (strong) IBOutlet NSTableView *sensorsTable;

@property (assign) uint16_t heartRate;
@property (strong) NSMutableArray *heartRateMonitors;
@property (nonatomic, strong) CBPeripheral *currentlyConnectedPeripheral;
// RR intervals
@property (strong) NSMutableArray *RRs;
// Queue for intervals waiting to be sent
@property (strong) NSMutableArray *RRsToSend;
// Start time for each portion of intervals sending to server
@property (strong) NSDate *startTime;
@property (assign) int create;

@property (strong) NSString *loginValue;
@property (strong) NSString *passwordValue;
@property (strong) NSString *serviceName;
@property (strong) IBOutlet NSButton *saveLogin;
@property (strong) EMKeychainItem *keychain;

- (IBAction) connectButtonPressed:(id)sender;
- (IBAction) loginButtonPressed:(id)sender;

- (void) startScan;
- (void) stopScan;
- (BOOL) isLECapableHardware;

- (void) updateWithHRMData:(NSData *)data;

- (void) sendRRs:(NSArray *)rrs;


@end
