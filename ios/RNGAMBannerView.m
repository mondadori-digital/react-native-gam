#import "RNGAMBannerView.h"
#import "RNGAMUtils.h"

#if __has_include(<React/RCTBridgeModule.h>)
#import <React/RCTBridgeModule.h>
#import <React/UIView+React.h>
#import <React/RCTLog.h>
#else
#import "RCTBridgeModule.h"
#import "UIView+React.h"
#import "RCTLog.h"
#endif

#include "RCTConvert+GADAdSize.h"
#import <CriteoPublisherSdk/CriteoPublisherSdk.h>
#import "RNGAMConfig.h"


@implementation RNGAMBannerView
{
    GAMBannerView  *_bannerView;
}

- (void)dealloc
{
    _bannerView.delegate = nil;
    _bannerView.adSizeDelegate = nil;
    _bannerView.appEventDelegate = nil;
}

- (instancetype)initWithFrame:(CGRect)frame
{
    if ((self = [super initWithFrame:frame])) {
        super.backgroundColor = [UIColor clearColor];

        UIWindow *keyWindow = [[UIApplication sharedApplication] keyWindow];
        UIViewController *rootViewController = [keyWindow rootViewController];

        _bannerView = [[GAMBannerView alloc] initWithAdSize:GADAdSizeBanner];
        _bannerView.delegate = self;
        _bannerView.adSizeDelegate = self;
        _bannerView.appEventDelegate = self;
        _bannerView.rootViewController = rootViewController;
        [self addSubview:_bannerView];
    }

    return self;
}

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wobjc-missing-super-calls"
- (void)insertReactSubview:(UIView *)subview atIndex:(NSInteger)atIndex
{
    RCTLogError(@"RNGAMBannerView cannot have subviews");
}
#pragma clang diagnostic pop

- (void)loadBanner {
    NSLog(@"DTB - andSlotUUID:%@", self.amazonSlotUUID);
    if (self.amazonSlotUUID) {
        // AMAZON request
        if(self.contentURL) {
            [DTBAds.sharedInstance addCustomAttribute:@"contentURL" value:self.contentURL];
        }
        DTBAdSize *size = [[DTBAdSize alloc] initBannerAdSizeWithWidth:_bannerView.frame.size.width height:_bannerView.frame.size.height andSlotUUID:self.amazonSlotUUID];
        DTBAdLoader *adLoader = [DTBAdLoader new];
        [adLoader setSizes:size, nil];
        [adLoader loadAd:self];
    } else {
        [self requestBanner];
    }

}

- (void)requestBanner {
    [[Criteo sharedCriteo] loadBidForAdUnit:[RNGAMConfig sharedInstance].GAM2Criteo[self.adType] responseHandler:^(CRBid *bid) {
        NSLog(@"Criteo - adType %@", self.adType);
        NSLog(@"Criteo - unit %@", [RNGAMConfig sharedInstance].GAM2Criteo[self.adType]);
        
        GAMRequest *request = [GAMRequest request];
        if (self.customTargeting) request.customTargeting = self.customTargeting;
        
        // contentUrl
        if(self.contentURL) {
            request.contentURL = self.contentURL; 
        }

        // localizzazione
        // if (self.location) {
        //     [request setLocationWithLatitude:[self.location[@"latitude"] doubleValue]
        //     longitude:[self.location[@"longitude"] doubleValue]
        //     accuracy:[self.location[@"accuracy"] doubleValue]];
        // }
        
        // add Criteo bids into Ad Manager request
        if (bid != nil) {
            [[Criteo sharedCriteo] enrichAdObject:request withBid:bid];
        }
        
        // request.testDevices = _testDevices;
        [_bannerView loadRequest:request];
    }];
}

#pragma mark - <DTBAdCallback>
- (void)onFailure: (DTBAdError)error {
    NSLog(@"DTB - Failed to load ad :(");
    [self requestBanner];
}

- (void)onSuccess: (DTBAdResponse *)adResponse {
    // Add APS Keywords.
    NSLog(@"DTB - Loaded :)");
    [[Criteo sharedCriteo] loadBidForAdUnit:[RNGAMConfig sharedInstance].GAM2Criteo[self.adType] responseHandler:^(CRBid *bid) {
        NSLog(@"Criteo - adType %@", self.adType);
        NSLog(@"Criteo - unit %@", [RNGAMConfig sharedInstance].GAM2Criteo[self.adType]);
        GAMRequest *request = [GAMRequest request];

        NSMutableDictionary *custTarg = [[NSMutableDictionary alloc] init];
        [custTarg addEntriesFromDictionary: adResponse.customTargeting];
        if (self.customTargeting) [custTarg addEntriesFromDictionary: self.customTargeting];
        NSLog(@"customTargeting %@", custTarg);

        request.customTargeting = custTarg;

        if (bid != nil) {
            // add Criteo bids into Ad Manager request
            [[Criteo sharedCriteo] enrichAdObject:request withBid:bid];
        }
        // add Criteo bids into Ad Manager request
        if (bid != nil) {
            [[Criteo sharedCriteo] enrichAdObject:request withBid:bid];
        }
        [_bannerView loadRequest:request];
    }];
}

- (void)setValidAdSizes:(NSArray *)adSizes
{
    __block NSMutableArray *validAdSizes = [[NSMutableArray alloc] initWithCapacity:adSizes.count];
    [adSizes enumerateObjectsUsingBlock:^(id jsonValue, NSUInteger idx, __unused BOOL *stop) {
        GADAdSize adSize = [RCTConvert GADAdSize:jsonValue];
        if (GADAdSizeEqualToSize(adSize, GADAdSizeInvalid)) {
            RCTLogWarn(@"Invalid adSize %@", jsonValue);
        } else {
            [validAdSizes addObject:NSValueFromGADAdSize(adSize)];
        }
    }];
    _bannerView.validAdSizes = validAdSizes;
}

// - (void)setTestDevices:(NSArray *)testDevices
// {
//     _testDevices = RNGAMMobProcessTestDevices(testDevices, kDFPSimulatorID);
// }

-(void)layoutSubviews
{
    [super layoutSubviews];
    _bannerView.frame = self.bounds;
}

# pragma mark GADBannerViewDelegate

/// Tells the delegate an ad request loaded an ad.
- (void)adViewDidReceiveAd:(GAMBannerView *)adView
{
    if (self.onSizeChange) {
        self.onSizeChange(@{
                            @"width": @(adView.frame.size.width),
                            @"height": @(adView.frame.size.height) });
    }
    if (self.onAdLoaded) {
        self.onAdLoaded(@{});
    }
}

/// Tells the delegate an ad request failed.
- (void)adView:(GAMBannerView *)adView
didFailToReceiveAdWithError:(NSError *)error
{
    if (self.onAdFailedToLoad) {
        self.onAdFailedToLoad(@{ @"error": @{ @"message": [error localizedDescription] } });
    }
}

/// Tells the delegate that a full screen view will be presented in response
/// to the user clicking on an ad.
- (void)adViewWillPresentScreen:(GAMBannerView *)adView
{
    if (self.onAdOpened) {
        self.onAdOpened(@{});
    }
}

/// Tells the delegate that the full screen view will be dismissed.
- (void)adViewWillDismissScreen:(__unused GAMBannerView *)adView
{
    if (self.onAdClosed) {
        self.onAdClosed(@{});
    }
}

/// Tells the delegate that a user click will open another app (such as
/// the App Store), backgrounding the current app.
- (void)adViewWillLeaveApplication:(GAMBannerView *)adView
{
    if (self.onAdLeftApplication) {
        self.onAdLeftApplication(@{});
    }
}

# pragma mark GADAdSizeDelegate

- (void)adView:(GADBannerView *)bannerView willChangeAdSizeTo:(GADAdSize)size
{
    CGSize adSize = CGSizeFromGADAdSize(size);
    self.onSizeChange(@{
                        @"width": @(adSize.width),
                        @"height": @(adSize.height) });
}

# pragma mark GADAppEventDelegate

- (void)adView:(GADBannerView *)banner didReceiveAppEvent:(NSString *)name withInfo:(NSString *)info
{
    if (self.onAppEvent) {
        self.onAppEvent(@{ @"name": name, @"info": info });
    }
    [DTBAdHelper skadnHelper:name withInfo:info];
}

@end
