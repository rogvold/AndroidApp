//
//  ScanWindowController.m
//  TeamHRM
//
//  Created by Yuriy Pogrebnyak on 18.03.13.
//  Copyright (c) 2013 Alexander O. Taraymovich. All rights reserved.
//

#import "ScanWindowController.h"
#import <ZXing/ZXResult.h>
#import "Base64.h"

//@interface ScanWindowController ()
//
//@end

@implementation ScanWindowController

@synthesize mainWindow;
@synthesize mainView;
@synthesize binaryView;
@synthesize luminanceView;
@synthesize previewBox;
@synthesize binaryBox;
@synthesize luminanceBox;

@synthesize previewView;
@synthesize userdefaults;
@synthesize zxingEngine;
@synthesize captureLayer;
@synthesize closeButton;
@synthesize captureDevice;
@synthesize mirrorVideoMode;
@synthesize resultsText;
@synthesize retryButton;

@synthesize sourceSelectPopupMenu;
@synthesize mirrorVideoCheckbox;
@synthesize soundsCheckbox;
@synthesize signInButton;
@synthesize currentImageBuffer;
@synthesize allVideoDevices;
@synthesize currentVideoSourceName;
@synthesize resultsSound;
@synthesize username;
@synthesize password;

- (id)initWithWindow:(NSWindow *)window
{
    self = [super initWithWindow:window];
    return self;
}

- (void)windowDidLoad
{
    [super windowDidLoad];
    
    // Implement this method to handle any initialization after your window controller's window has been loaded from its nib file.
}

- (void) awakeFromNib
{
#ifdef __DEBUG_LOGGING__
    NSLog(@"AppDelegate::awakeFromNib - ENTER");
#endif
	
    allVideoDevices		= [[NSMutableArray alloc] init];
    [self setUserdefaults:[NSUserDefaults standardUserDefaults]];
    
    [userdefaults setBool:[soundsCheckbox state] forKey:KZXING_SOUND_ON_RESULT];
    
#ifdef __DEBUG_LOGGING__
    NSLog(@"AppDelegate::applicationDidFinishLaunching - ENTER");
#endif
	
    BOOL forcePrefsReset = NO;
	
    [self setupPreferences:forcePrefsReset];
    [self setupSound];
	
    zxingEngine = [self createZXcapture];
	
    NSNotificationCenter* nc = [NSNotificationCenter defaultCenter];
    if(nil != nc)
    {
        [nc addObserver: self
               selector: @selector(windowWillClose:)
                   name: @"NSWindowWillCloseNotification"
                 object: nil];
    }
	
	
    // NSRect NSRectFromCGRect(CGRect cgrect);
    // CGRect NSRectToCGRect(NSrect nsrect);
	
    if(nil != zxingEngine)
    {
        CALayer* layertemp = nil; // this is used for debugging
        NSRect	 nsRect;
        CGRect	 cgRect;
        
        zxingEngine.binary = YES;
        zxingEngine.luminance = YES;
        
        // create a layer where the raw video will appear
        captureLayer = [zxingEngine layer];
		
        if(nil != captureLayer)
        {
            // CALayer CGRect for capturePayer is going to be all ZEROES
            nsRect   = [[previewBox contentView] frame];
            cgRect	 = [self shrinkContentRect:nsRect];
			
            [captureLayer	setFrame:cgRect];
            [captureLayer	setBackgroundColor:kBACKGROUNDCOLOR];
            [previewView	setLayer:captureLayer];
            [previewView	setWantsLayer:YES];
            
            layertemp = zxingEngine.luminance;
            if((nil != layertemp) && (nil != luminanceView))
            {
                nsRect  = [[luminanceBox contentView] frame];
                cgRect	= [self shrinkContentRect:nsRect];
                [layertemp	setFrame:cgRect];
                [luminanceView	setLayer:layertemp];
            }
            
            layertemp = zxingEngine.binary;
            if((nil != layertemp) & (nil != binaryView))
            {
                nsRect   = [[binaryBox contentView] frame];
                cgRect	= [self shrinkContentRect:nsRect];
                [layertemp	setFrame:cgRect];
                [binaryView setLayer:layertemp];
            }
            
            [self performVideoSourceScan];
			
            if((nil == captureDevice) && (nil != allVideoDevices) && (0 < [allVideoDevices count]))
            {
                [self setCaptureDevice:(QTCaptureDevice*)[allVideoDevices objectAtIndex:0]];
            }
			
            
            zxingEngine.captureDevice = captureDevice;
            zxingEngine.delegate = [[NSApplication sharedApplication]delegate];
            zxingEngine.mirror = mirrorVideoMode;
            [zxingEngine start];
        }
    }
}


- (IBAction) captureButtonPressed:(id) sender
{
#pragma unused(sender)
	
#ifdef __DEBUG_LOGGING__
    NSLog(@"AppDelegate::captureButtonPressed - ENTER");
#endif
	
    if(!zxingEngine.running)
    {
        // Remove the RESULTS layer if it's there...
        //if(nil != resultsLayer)
        //	{
        //	[resultsLayer removeFromSuperlayer];
        //	[resultsLayer release];
        //	resultsLayer = nil;
        //	}
        
#ifdef __DEBUG_LOGGING__
        NSLog(@"AppDelegate::captureButtonPressed - zxingEngine was not running");
#endif
        
		
        [captureButton			setTitle:kCANCELTITLE];
        [resultsText			setStringValue:kBLANKSTR];	// NSTextField
		
        zxingEngine.captureDevice = captureDevice ;
        zxingEngine.delegate = self;
        zxingEngine.mirror = mirrorVideoMode;
        
        [zxingEngine start];
    }
    else	// isRunning 
    {			
        [zxingEngine stop];
        [captureButton			setTitle:kCAPTURETITLE];
    }
}

// ------------------------------------------------------------------------------------------
// Find all video input devices for DISPLAY IN POPUP MENU - This does not initialize anything

- (void) performVideoSourceScan
{
#ifdef __DEBUG_LOGGING__
    NSLog(@"ZXSourceSelect::performVideoSourceScan - ENTER");
#endif
	
    NSUInteger		dex				= 0;
    OSErr			err				= noErr;
    NSString*		theitemtitle	= nil;
    NSMenuItem*		themenuitem		= nil;
    NSArray*		theQTarray		= nil;
	
    if(nil != sourceSelectPopupMenu)
    {
        NSUInteger count;
        [sourceSelectPopupMenu	removeAllItems];		// wipe popup menu clean
		
        [sourceSelectPopupMenu  addItemWithTitle:kIMAGESOURCESELECTIONPOPUPTITLE];	// always the top item
		
        [allVideoDevices		removeAllObjects];		// wipe array of video devices clean
		
        // acquire unmuxed devices
        theQTarray = [NSArray arrayWithArray:[QTCaptureDevice inputDevicesWithMediaType:QTMediaTypeVideo]];
        if((nil != theQTarray) && (0 < [theQTarray count]))
            [allVideoDevices addObjectsFromArray:theQTarray];
		
        // acquire muxed devices
        theQTarray = [NSArray arrayWithArray:[QTCaptureDevice inputDevicesWithMediaType:QTMediaTypeMuxed]];
        if((nil != theQTarray) && (0 < [theQTarray count]))
            [allVideoDevices addObjectsFromArray:theQTarray];
		
        // did anything show up?
        count = [allVideoDevices count];
        if(0 < count)
        {
            for(dex = 0; dex < count; dex++)
            {
                QTCaptureDevice* aVideoDevice = (QTCaptureDevice*)[allVideoDevices objectAtIndex:dex];
                if(nil != aVideoDevice)
                {
                    theitemtitle  = [aVideoDevice localizedDisplayName];
                    [sourceSelectPopupMenu addItemWithTitle:theitemtitle]; // NSPopUpButton
					
                    themenuitem = [sourceSelectPopupMenu itemWithTitle:theitemtitle];
                    [themenuitem setTag:(dex+kVIDEOSOURCEOFFSET)];
                }
            }
        }
        else	// reset the menu
        {
            err = fnfErr;
            [sourceSelectPopupMenu removeAllItems];
            [sourceSelectPopupMenu addItemWithTitle:kIMAGESOURCESELECTIONPOPUPTITLE];
            [sourceSelectPopupMenu addItemWithTitle:kIMAGESOURCESELECTIONRESCANTITLE];
            [self setCurrentVideoSourceName:kBLANKSTR];
            [userdefaults setObject:(id)kBLANKSTR forKey:kVIDEOSOURCETITLE];
        }
		
        if(noErr == err)
        {
            [sourceSelectPopupMenu addItemWithTitle:kIMAGESOURCESELECTIONRESCANTITLE];
            [sourceSelectPopupMenu addItemWithTitle:kIMAGESOURCESELECTIONDISCONNECT];	// Disconnect video Source
        }
    }
}
// called when user clicks in or selects an item in the popup menu

- (IBAction) selectedVideoSourceChange:(id)sender
{
#ifdef __DEBUG_LOGGING__
    NSLog(@"ZXSourceSelect::selectedVideoSourceChange - ENTER");
#endif
	
    [self performSelectorOnMainThread:@selector(configureForVideoSource:) withObject:sender waitUntilDone:NO];
}

- (IBAction) configureForVideoSource:(id) sender
{
    NSPopUpButton* videoselections = (id) sender;
    
    if(nil != videoselections)
    {
        NSInteger numberOfItems = [videoselections numberOfItems];
        if(1 < numberOfItems)
        {
            NSMenuItem* selectedItem = [videoselections selectedItem];
            if(nil != selectedItem)
            {
                NSInteger tag = [selectedItem tag];
				
                // See if it's a source or command that the user has selected
				
                if(kVIDEOSOURCEOFFSET > tag)	// in this case, the user wants to rescan or disable
                {
                    NSString* selectedItemTitle = [selectedItem title];
                    if(nil != selectedItemTitle)
                    {
                        BOOL isDisableVideoRequest = (NSOrderedSame == [selectedItemTitle compare:kIMAGESOURCESELECTIONDISCONNECT]);
                        if(YES == isDisableVideoRequest)
                        {
                            [sourceSelectPopupMenu removeAllItems];
                            [sourceSelectPopupMenu addItemWithTitle:kIMAGESOURCESELECTIONPOPUPTITLE];
                            [sourceSelectPopupMenu addItemWithTitle:kIMAGESOURCESELECTIONRESCANTITLE];
                        }
                        else
                        {
                            BOOL isRescanRequest = (NSOrderedSame == [selectedItemTitle compare:kIMAGESOURCESELECTIONRESCANTITLE]);
                            if(YES == isRescanRequest)
                            {
                                [self performVideoSourceScan];
                            }
                            else
                            {
                                ; // nothing...
                            }
                        }
                    }
                }
                else	// changing the selected video to a source that is (or was) known to exist
                {
                    NSInteger devicecount = [allVideoDevices count];
                    NSInteger tagindex = (tag - kVIDEOSOURCEOFFSET);
					
                    if((0 <= tagindex) && (tagindex < devicecount))
                    {
                        QTCaptureDevice* aCaptureDevice = (QTCaptureDevice*)[allVideoDevices objectAtIndex:tagindex];
						
                        if(nil != aCaptureDevice)
                        {
                            currentVideoSourceName = [aCaptureDevice localizedDisplayName];
                            if(nil != currentVideoSourceName)
                            {
                                if(0 < [currentVideoSourceName length])
                                {
                                    [userdefaults setObject:kZXING_VIDEOSOURCENAME forKey:currentVideoSourceName];
                                    [userdefaults synchronize];
									
                                    [zxingEngine stop];
                                    zxingEngine.captureDevice = aCaptureDevice;
                                    zxingEngine.delegate = self;
                                    zxingEngine.mirror = mirrorVideoMode;
                                    [zxingEngine start];
                                }
                            }
                            [self setCaptureDevice:aCaptureDevice]; // releases existing and retains the new device
                        }
                    }
                    else
                    {
#ifdef __DEBUG_LOGGING__
                        NSLog(@"ERROR ZXSourceSelect::presentSourceSelectSheet - tag [%d] for [%@] outside bounds of allVideoDevices count [%d]",
                              (int)tag, [selectedItem title], (int)devicecount);
#endif
                    }
                }
            }
        }
    }
}

- (CGRect) shrinkContentRect:(NSRect) inRect
{
    CGRect result;
	
    result.origin.x		= (inRect.origin.x		+ kLEFTVIDEOEASE);
    result.origin.y		= (inRect.origin.y		+ kTOPVIDEOEASE);
    result.size.width	= (inRect.size.width	- kWIDTHVIDEOEASE);
    result.size.height	= (inRect.size.height	- kHEIGHTVIDEOEASE);
	
    return(result);
}

- (void) setupPreferences:(BOOL) forceReset
{
#ifdef __DEBUG_LOGGING__
    NSLog(@"AppDelegate::setupPerferences - ENTER - forceReset [%@]", ((YES == forceReset)?@"YES":@"NO"));
#endif
	
    // If there was nothing there, init - else leave previous settings in place
    NSString* zxinglibsupport = (NSString*)[userdefaults objectForKey:kZXING_LIBSUPPORT];
	
    if((nil == zxinglibsupport) || (YES == forceReset))
    {
        [userdefaults setObject:kZXING_LIBSUPPORT forKey:kZXING_LIBSUPPORT];
        [userdefaults setBool:NO				  forKey:KZXING_MIRROR_VIDEO];
        [userdefaults setBool:YES				  forKey:KZXING_SOUND_ON_RESULT];
		
        [userdefaults synchronize];
    }
    
    [self setMirrorVideoMode:(BOOL)[userdefaults boolForKey:KZXING_MIRROR_VIDEO]];
}

- (void) setMirrorVideoMode:(BOOL)mirror {
    mirrorVideoMode = mirror;
    zxingEngine.mirror = mirror;
}

- (ZXCapture*) createZXcapture
{
#ifdef __DEBUG_LOGGING__
    NSLog(@"AppDelegate::createZXcapture - ENTER");
#endif
	
    ZXCapture* thecaptureobject = [[ZXCapture alloc] init];
	
    return(thecaptureobject);
}

// -----------------------------------------------------------------------------------------

- (void) setupSound
{
    BOOL playsounds = (BOOL)[userdefaults boolForKey:KZXING_SOUND_ON_RESULT];
	
    NSSound* thesound = [NSSound soundNamed:@"shutter"];
    if(nil != thesound)
    {
        [self setResultsSound:thesound];
    }
}

// ==========================================================================================
//
//		ZXCaptureDelegate functions:

- (void)captureResult:(ZXCapture*)zxCapture
               result:(ZXResult*) inResult
{
#ifdef __DEBUG_LOGGING_CAPTURE__
    NSLog(@"AppDelegate::captureResult - ENTER");
#endif
	
    if(nil != inResult)
    {
        NSString* resultString = [inResult text];
		
        if(nil != resultString)
        {
            NSString *decodedString = [resultString base64DecodedString];
            NSArray *resultArray = [decodedString componentsSeparatedByString:@" "];
            username = resultArray[0];
            password = resultArray[1];
            [retryButton setEnabled:YES];
            [signInButton setEnabled:YES];
            [resultsText setStringValue:[NSString stringWithFormat:@"Detected user with username: %@", username]];
            [self manageOverlay:inResult];
            
            [zxingEngine stop];							// stop and wait for user to want to "Capture" again
			
#ifdef __DEBUG_LOGGING__
            NSLog(@"AppDelegate::captureResult - inResult text[%@]", resultText);
#endif
			
            if(nil != resultsSound)
            {
                [resultsSound play];
            }
        }
    }
}

// ------------------------------------------------------------------------------------------

- (void) presentOverlayForPoints:(CGPoint)point0
                             pt1:(CGPoint)point1
                             pt2:(CGPoint)point2
{
#ifdef __DEBUG_LOGGING__
    NSLog(@"AppDelegate::presentOverlayForPoints - ENTER pt0X[%d] pt0Y[%d] pt1X[%d] pt1Y[%d] pt2X[%d] pt2Y[%d]",
          (int)point0.x, (int)point0.y, (int)point1.x, (int)point1.y, (int)point2.x, (int)point2.y);
#endif
	
	
}

// ------------------------------------------------------------------------------------------

- (void) manageOverlay:(ZXResult*) inResult
{
#ifdef __DEBUG_LOGGING_CAPTURE__
    NSLog(@"AppDelegate::manageOverlay - ENTER");
#endif
    /*
     #ifdef __SHOW_OVERLAY_LAYER__
     if(nil != resultsLayer)
     {
     [resultsLayer removeFromSuperlayer];
     [resultsLayer release];
     resultsLayer = nil;
     }
     
     resultsLayer = [[ZXOverlay alloc] init];
     
     // NSRect NSRectFromCGRect(CGRect cgrect);	<- handy reference
     // CGRect NSRectToCGRect(NSrect nsrect);
     
     NSRect resultsRect   = [[previewBox contentView] frame];  // [self shrinkContentRect:
     [resultsLayer setFrame:NSRectToCGRect(resultsRect)];
     [resultsLayer plotPointsOnLayer:inResult];
     [captureLayer addSublayer:resultsLayer];
     #endif
     */
	
}

// ------------------------------------------------------------------------------------------
// This interface doesn't do anything with this...

- (void)captureSize:(ZXCapture*) inCapture
              width:(NSNumber*)  inWidth
             height:(NSNumber*)  inHeight
{
#pragma unused (inCapture, inWidth, inHeight)
	
#ifdef __DEBUG_LOGGING__
    if((0 != [inWidth intValue]) && (0 != [inHeight intValue]))
    {
        NSLog(@"AppDelegate::captureSize - ENTER - inWidth [%d] inHeight [%d]",
              [inWidth intValue], [inHeight intValue]);
    }
#endif
}


//		finish: ZXCaptureDelegate functions:
//

- (IBAction)closeSheet:(id)sender
{
    [zxingEngine stop];
    [allVideoDevices	removeAllObjects];
    
    [[NSNotificationCenter defaultCenter] removeObserver:self];
    
    [NSApp endSheet:self.window returnCode:NSAlertDefaultReturn];
    [self.window orderOut:self];
}

- (IBAction)cancelSheet:(id)sender
{
    [zxingEngine stop];
    [allVideoDevices	removeAllObjects];
    
    [[NSNotificationCenter defaultCenter] removeObserver:self];
    
    [NSApp endSheet:self.window returnCode:NSAlertAlternateReturn];
    [self.window orderOut:self];
}

@end
