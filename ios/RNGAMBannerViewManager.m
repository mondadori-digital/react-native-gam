#import "RNGAMBannerViewManager.h"
#import "RNGAMBannerView.h"

#import <React/RCTBridge.h>
#import <React/RCTUIManager.h>
#import <React/RCTEventDispatcher.h>

@implementation RNGAMBannerViewManager

RCT_EXPORT_MODULE();

- (UIView *)view
{
  return [RNGAMBannerView new];
}

RCT_EXPORT_METHOD(loadBanner:(nonnull NSNumber *)reactTag)
{
    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, RNGAMBannerView *> *viewRegistry) {
        RNGAMBannerView *view = viewRegistry[reactTag];
        if (![view isKindOfClass:[RNGAMBannerView class]]) {
            RCTLogError(@"Invalid view returned from registry, expecting RNGAMBannerView, got: %@", view);
        } else {
            [view loadBanner];
        }
    }];
}

RCT_REMAP_VIEW_PROPERTY(adSize, _bannerView.adSize, GADAdSize)
RCT_REMAP_VIEW_PROPERTY(adUnitID, _bannerView.adUnitID, NSString)
RCT_EXPORT_VIEW_PROPERTY(validAdSizes, NSArray)
RCT_EXPORT_VIEW_PROPERTY(testDevices, NSArray)
RCT_EXPORT_VIEW_PROPERTY(amazonSlotUUID, NSString)
RCT_EXPORT_VIEW_PROPERTY(adType, NSString)
RCT_EXPORT_VIEW_PROPERTY(location, NSDictionary)
RCT_EXPORT_VIEW_PROPERTY(onSizeChange, RCTBubblingEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onAppEvent, RCTBubblingEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onAdLoaded, RCTBubblingEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onAdFailedToLoad, RCTBubblingEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onAdOpened, RCTBubblingEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onAdClosed, RCTBubblingEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onAdLeftApplication, RCTBubblingEventBlock)

@end
