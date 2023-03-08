#if __has_include(<React/RCTComponent.h>)
#import <React/RCTView.h>
#else
#import "RCTView.h"
#endif

@import GoogleMobileAds;
#import <DTBiOSSDK/DTBiOSSDK.h>

@class RCTEventDispatcher;

@interface RNGAMBannerView : RCTView <GADBannerViewDelegate, GADAdSizeDelegate, GADAppEventDelegate>

@property (nonatomic, copy) NSArray *validAdSizes;
@property (nonatomic, copy) NSArray *testDevices;

@property (nonatomic) NSDictionary * location;
@property (nonatomic) NSString * amazonSlotUUID;
@property (nonatomic) NSString * adType;
@property (nonatomic) NSString * contentURL;
@property (nonatomic) NSDictionary * customTargeting;

@property (nonatomic, copy) RCTBubblingEventBlock onSizeChange;
@property (nonatomic, copy) RCTBubblingEventBlock onAppEvent;
@property (nonatomic, copy) RCTBubblingEventBlock onAdLoaded;
@property (nonatomic, copy) RCTBubblingEventBlock onAdFailedToLoad;
@property (nonatomic, copy) RCTBubblingEventBlock onAdOpened;
@property (nonatomic, copy) RCTBubblingEventBlock onAdClosed;
@property (nonatomic, copy) RCTBubblingEventBlock onAdLeftApplication;

- (void)loadBanner;

@end
