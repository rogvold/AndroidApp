//
//  main.m
//  TeamHRM
//
//  Created by Alexander O. Taraymovich on 16.12.12.
//  Copyright (c) 2012 Alexander O. Taraymovich. All rights reserved.
//

#import <Cocoa/Cocoa.h>
#include <stdio.h>

int main(int argc, char *argv[])
{
    NSString *homeDirectory = [NSSearchPathForDirectoriesInDomains(NSLibraryDirectory, NSUserDomainMask, YES) objectAtIndex:0];
    NSString *logPath = [homeDirectory stringByAppendingPathComponent:@"Logs/TeamHRM.log"];
    const char *path = [logPath cStringUsingEncoding:NSASCIIStringEncoding];
    freopen([logPath cStringUsingEncoding:NSASCIIStringEncoding], "a" ,stderr);
    return NSApplicationMain(argc, (const char **)argv);
}
