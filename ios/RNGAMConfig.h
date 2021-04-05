//
//  RNGAMConfig.h
//  GialloZafferano
//
//  Created by Mauro Di Lalla on 13/01/2021.
//

#import <Foundation/Foundation.h>
#import <CriteoPublisherSdk/CriteoPublisherSdk.h>

@interface RNGAMConfig : NSObject {
  NSDictionary *GAM2Criteo;
}

@property (nonatomic) NSDictionary *GAM2Criteo;

+(RNGAMConfig *)sharedInstance;

@end
