#import "RNGAMInterstitial.h"
#import "RNGAMUtils.h"

#if __has_include(<React/RCTUtils.h>)
#import <React/RCTUtils.h>
#else
#import "RCTUtils.h"
#endif

#import <CriteoPublisherSdk/CriteoPublisherSdk.h>
#import "RNGAMConfig.h"

static NSString *const kEventAdLoaded = @"interstitialAdLoaded";
static NSString *const kEventAdFailedToLoad = @"interstitialAdFailedToLoad";
static NSString *const kEventAdOpened = @"interstitialAdOpened";
static NSString *const kEventAdFailedToOpen = @"interstitialAdFailedToOpen";
static NSString *const kEventAdClosed = @"interstitialAdClosed";
static NSString *const kEventAdLeftApplication = @"interstitialAdLeftApplication";

@implementation RNGAMInterstitial
{
    GAMInterstitialAd  *_interstitial;
    NSString *_adUnitID;
    NSString *_amazonSlotUUID;
    NSArray *_testDevices;
    RCTPromiseResolveBlock _requestAdResolve;
    RCTPromiseRejectBlock _requestAdReject;
    BOOL hasListeners;
    NSDictionary *_location;
    NSDictionary *_customTargeting;
}

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

+ (BOOL)requiresMainQueueSetup
{
    return NO;
}

RCT_EXPORT_MODULE();

- (NSArray<NSString *> *)supportedEvents
{
    return @[
             kEventAdLoaded,
             kEventAdFailedToLoad,
             kEventAdOpened,
             kEventAdFailedToOpen,
             kEventAdClosed,
             kEventAdLeftApplication ];
}

#pragma mark exported methods

RCT_EXPORT_METHOD(setAdUnitID:(NSString *)adUnitID amazonSlotUUID:(NSString *)amazonSlotUUID)
{
    _adUnitID = adUnitID;
    _amazonSlotUUID = amazonSlotUUID;
}

RCT_EXPORT_METHOD(setTestDevices:(NSArray *)testDevices)
{
    _testDevices = RNGAMProcessTestDevices(testDevices, GADSimulatorID);
}

RCT_EXPORT_METHOD(setLocation:(NSDictionary *)location)
{
    _location = location;
}

RCT_EXPORT_METHOD(setCustomTargeting:(NSDictionary *)customTargeting)
{
    _customTargeting = customTargeting;
}

RCT_EXPORT_METHOD(requestAd:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
    _requestAdResolve = nil;
    _requestAdReject = nil;

    if (_interstitial == nil) {
        _requestAdResolve = resolve;
        _requestAdReject = reject;

        //NSLog(@"DTB - andSlotUUID:%@", self.amazonSlotUUID);
        // AMAZON request
        if (_amazonSlotUUID) {
            DTBAdSize *size = [[DTBAdSize alloc] initInterstitialAdSizeWithSlotUUID:_amazonSlotUUID];
            DTBAdLoader *adLoader = [DTBAdLoader new];
            [adLoader setSizes:size, nil];
            [adLoader loadAd:self];
        } else {
            [self requestInterstitial];
        }
       
    } else {
        reject(@"E_AD_ALREADY_LOADED", @"Ad is already loaded.", nil);
    }
}

RCT_EXPORT_METHOD(showAd:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
    NSLog(@"RNGAMInterstitial - showAd");
    if (_interstitial) {
        NSLog(@"RNGAMInterstitial - Ad is ready");
        [_interstitial presentFromRootViewController:[UIApplication sharedApplication].delegate.window.rootViewController];
        resolve(nil);
    }
    else {
        NSLog(@"RNGAMInterstitial - Ad is not ready");
        reject(@"E_AD_NOT_READY", @"Ad is not ready.", nil);
    }
}

- (void)startObserving
{
    hasListeners = YES;
}

- (void)stopObserving
{
    hasListeners = NO;
}

- (void)requestInterstitial {
    
    [[Criteo sharedCriteo] loadBidForAdUnit:[RNGAMConfig sharedInstance].GAM2Criteo[@"interstitial"] responseHandler:^(CRBid *bid) {

        NSLog(@"Criteo - unit %@", [RNGAMConfig sharedInstance].GAM2Criteo[@"interstitial"]);

        GAMRequest *request = [GAMRequest request];
        
        // localizzazione
        // if (_location) {
        //     [request setLocationWithLatitude:[_location[@"latitude"] doubleValue]
        //     longitude:[_location[@"longitude"] doubleValue]
        //     accuracy:[_location[@"accuracy"] doubleValue]];
        // }
        
        if (bid != nil) {
            // add Criteo bids into Ad Manager request
            [[Criteo sharedCriteo] enrichAdObject:request withBid:bid];
        }

        [GAMInterstitialAd loadWithAdManagerAdUnitID:_adUnitID
                              request:request
                    completionHandler:^(GAMInterstitialAd *ad, NSError *error) {
        if (error) {
            NSLog(@"Failed to load interstitial ad with error: %@", [error localizedDescription]);
            _requestAdReject(@"E_AD_REQUEST_FAILED", error.localizedDescription, error);
        } else {
            _interstitial = ad;
            _interstitial.fullScreenContentDelegate = self;
            _requestAdResolve(nil);
        }    
        }];
    }];
}

#pragma mark - <DTBAdCallback>
- (void)onFailure: (DTBAdError)error {
    NSLog(@"DBT - Interstitial Failed to load ad :(");
    [self requestInterstitial];
}
 
- (void)onSuccess: (DTBAdResponse *)adResponse {
    NSLog(@"DTB - Interstitial Loaded :)");
    // Code from Google Ad Manager to set up Google Ad Manager's ad view.
    [[Criteo sharedCriteo] loadBidForAdUnit:[RNGAMConfig sharedInstance].GAM2Criteo[@"interstitial"] responseHandler:^(CRBid *bid) {
        
        NSLog(@"Criteo - unit %@", [RNGAMConfig sharedInstance].GAM2Criteo[@"interstitial"]);

        GAMRequest *request = [GAMRequest request];
        // Add APS Keywords.
        // request.customTargeting = adResponse.customTargeting;
        if (_customTargeting) request.customTargeting = _customTargeting;
        NSLog(@"loadWithAdManagerAdUnitID");
        if (bid != nil) {
            // add Criteo bids into Ad Manager request
            [[Criteo sharedCriteo] enrichAdObject:request withBid:bid];
        }
        NSLog(@"loadWithAdManagerAdUnitID");
        [GAMInterstitialAd loadWithAdManagerAdUnitID:_adUnitID
                              request:request
                    completionHandler:^(GAMInterstitialAd *ad, NSError *error) {
            
        if (error) {
            NSLog(@"Failed to load interstitial ad with error: %@", [error localizedDescription]);
            _requestAdReject(@"E_AD_REQUEST_FAILED", error.localizedDescription, error);
        } else {
            NSLog(@"Interstitial Loaded");
        
            _interstitial = ad;
            _interstitial.fullScreenContentDelegate = self;
            _requestAdResolve(nil);
            }
        } ];
    }];
}

// Tells the delegate that the ad failed to present full screen content.
- (void)ad:(nonnull id<GADFullScreenPresentingAd>)ad
didFailToPresentFullScreenContentWithError:(nonnull NSError *)error {
    NSLog(@"Ad did fail to present full screen content.");
    if (hasListeners) {
        NSDictionary *jsError = RCTJSErrorFromCodeMessageAndNSError(@"E_AD_REQUEST_FAILED", error.localizedDescription, error);
        [self sendEventWithName:kEventAdFailedToLoad body:jsError];
    }
    _interstitial = nil;
    _requestAdReject(@"E_AD_REQUEST_FAILED", error.localizedDescription, error);
}

// Tells the delegate that the ad presented full screen content.
- (void)adDidPresentFullScreenContent:(nonnull id<GADFullScreenPresentingAd>)ad {
    NSLog(@"Ad did present full screen content.");
    if (hasListeners){
        [self sendEventWithName:kEventAdOpened body:nil];
    }
}

// Tells the delegate that the ad will present full screen content.
- (void)adWillPresentFullScreenContent:(nonnull id<GADFullScreenPresentingAd>)ad {
    NSLog(@"Ad will present full screen content.");
}

// Tells the delegate that the ad dismissed full screen content.
- (void)adDidDismissFullScreenContent:(nonnull id<GADFullScreenPresentingAd>)ad {
   NSLog(@"Ad did dismiss full screen content.");
   if (hasListeners) {
        [self sendEventWithName:kEventAdClosed body:nil];
    }
}

@end
