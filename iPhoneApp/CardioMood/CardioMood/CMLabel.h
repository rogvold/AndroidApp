//
//  CMLabel.h
//  CardioMood
//
//  Created by Alexander O. Taraymovich on 03.04.13.
//  Copyright (c) 2013 Alexander O. Taraymovich. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface CMLabel : UILabel

@property (strong, nonatomic, readwrite) UIView* inputView;
@property (strong, nonatomic, readwrite) UIView* inputAccessoryView;

@end
