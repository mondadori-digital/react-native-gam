package it.mondadori.rngam;

import android.util.Log;
import android.content.Context;
import androidx.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.os.Bundle;
import android.location.Location;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableNativeArray;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.PixelUtil;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.facebook.react.views.view.ReactViewGroup;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.admanager.*;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
// import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.MobileAds;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.amazon.device.ads.*;
import com.criteo.publisher.Bid;
import com.criteo.publisher.BidResponseListener;
import com.criteo.publisher.Criteo;
import com.criteo.publisher.model.BannerAdUnit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class ReactPublisherAdView extends ReactViewGroup implements AppEventListener {

    protected AdManagerAdView adView;

    String[] testDevices;
    AdSize[] validAdSizes;
    String adUnitID;
    String adType;
    String amazonSlotUUID;
    AdSize adSize;
    ReadableMap location;
    ReadableMap customTargeting;
    String ppid;

    public ReactPublisherAdView(final Context context) {
        super(context);
        this.createAdView();
    }

    private void createAdView() {
        if (this.adView != null) {
            this.removeViewAt(0);
            this.adView.destroy();
        }

        final Context context = getContext();
        this.adView = new AdManagerAdView(context);
        this.adView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        this.adView.setAppEventListener(this);
        this.adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                int width = adView.getAdSize().getWidthInPixels(context);
                int height = adView.getAdSize().getHeightInPixels(context);
                int left = adView.getLeft();
                int top = adView.getTop();
                adView.measure(width, height);
                adView.layout(left, top, left + width, top + height);
                sendOnSizeChangeEvent();
                sendEvent(RNGAMBannerViewManager.EVENT_AD_LOADED, null);
            }

            @Override
            public void onAdFailedToLoad(LoadAdError error) {
                String errorMessage = error.getMessage();
                int errorCode = error.getCode();
                // switch (errorCode) {
                //     case AdManagerAdRequest.ERROR_CODE_INTERNAL_ERROR:
                //         errorMessage = "Internal error, an invalid response was received from the ad server.";
                //         break;
                //     case AdManagerAdRequest.ERROR_CODE_INVALID_REQUEST:
                //         errorMessage = "Invalid ad request, possibly an incorrect ad unit ID was given.";
                //         break;
                //     case AdManagerAdRequest.ERROR_CODE_NETWORK_ERROR:
                //         errorMessage = "The ad request was unsuccessful due to network connectivity.";
                //         break;
                //     case AdManagerAdRequest.ERROR_CODE_NO_FILL:
                //         errorMessage = "The ad request was successful, but no ad was returned due to lack of ad inventory.";
                //         break;
                // }
                Log.d("Ads", error.toString());
                WritableMap event = Arguments.createMap();
                WritableMap errorMap = Arguments.createMap();
                errorMap.putString("message", errorMessage);
                event.putMap("error", errorMap);
                sendEvent(RNGAMBannerViewManager.EVENT_AD_FAILED_TO_LOAD, event);
            }

            @Override
            public void onAdOpened() {
                sendEvent(RNGAMBannerViewManager.EVENT_AD_OPENED, null);
            }

            @Override
            public void onAdClosed() {
                sendEvent(RNGAMBannerViewManager.EVENT_AD_CLOSED, null);
            }

        });
        this.addView(this.adView);
    }

    private void sendOnSizeChangeEvent() {
        int width;
        int height;
        ReactContext reactContext = (ReactContext) getContext();
        WritableMap event = Arguments.createMap();
        AdSize adSize = this.adView.getAdSize();
        if (adSize == AdSize.SMART_BANNER) {
            width = (int) PixelUtil.toDIPFromPixel(adSize.getWidthInPixels(reactContext));
            height = (int) PixelUtil.toDIPFromPixel(adSize.getHeightInPixels(reactContext));
        } else {
            width = adSize.getWidth();
            height = adSize.getHeight();
        }
        event.putDouble("width", width);
        event.putDouble("height", height);
        sendEvent(RNGAMBannerViewManager.EVENT_SIZE_CHANGE, event);
    }

    private void sendEvent(String name, @Nullable WritableMap event) {
        ReactContext reactContext = (ReactContext) getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                        getId(),
                        name,
                        event);
    }

    private void loadAdManagerBanner(@Nullable Bid bid) {
        AdManagerAdRequest.Builder adRequestBuilder = new AdManagerAdRequest.Builder();
        
        // if (testDevices != null) {
        //     RequestConfiguration configuration = new RequestConfiguration.Builder().setTestDeviceIds(testDevices).build();
        //     MobileAds.setRequestConfiguration(configuration);
        // }
        
        // if (location != null && location.hasKey("latitude") && !location.isNull("latitude") && location.hasKey("longitude") && !location.isNull("longitude")) {
        //     Location advLocation = new Location("");
        //     advLocation.setLatitude(location.getDouble("latitude"));
        //     advLocation.setLongitude(location.getDouble("longitude"));

        //     adRequestBuilder.setLocation(advLocation);
        // }

        // Criteo
        if (bid != null) {
            Log.d("loadBanner", "Criteo bid is not null");
            Criteo.getInstance().enrichAdObjectWithBid(adRequestBuilder, bid);
        }
        if (customTargeting != null) {
            ReadableMapKeySetIterator iterator = customTargeting.keySetIterator();
            while (iterator.hasNextKey()) {
                String key = iterator.nextKey();
                ReadableType type = customTargeting.getType(key);
                if (type == ReadableType.String){
                    adRequestBuilder.addCustomTargeting(key, customTargeting.getString(key));
                }
            }
        }

        if (ppid != null) {
            adRequestBuilder.setPublisherProvidedId(ppid);
        }

        AdManagerAdRequest adRequest = adRequestBuilder.build();
        this.adView.loadAd(adRequest);
    }

    public void loadBanner() {
        
        BannerAdUnit criteoBannerAdUnit = (BannerAdUnit) RNGAMConfig.getInstance().getGAM2Criteo().get(this.adType);
        Log.d("loadBanner", "criteoBanner: " + this.adType);

        ArrayList<AdSize> adSizes = new ArrayList<AdSize>();
        ReactContext reactContext = (ReactContext) getContext();
        if (this.adSize != null) {
            adSizes.add(this.adSize);
        }
        if (this.validAdSizes != null) {
            for (int i = 0; i < this.validAdSizes.length; i++) {
                adSizes.add(this.validAdSizes[i]);
            }
        }

        if (adSizes.size() == 0) {
            adSizes.add(AdSize.BANNER);
        }

        AdSize[] adSizesArray = adSizes.toArray(new AdSize[adSizes.size()]);
        this.adView.setAdSizes(adSizesArray);

        // load Criteo bids
        Criteo.getInstance().loadBid(criteoBannerAdUnit, new BidResponseListener() {
            @Override
            public void onResponse(final @Nullable Bid bid) {
                // AMAZON
                if (amazonSlotUUID != null) {
                    final DTBAdRequest loader = new DTBAdRequest();
                    loader.setSizes(new DTBAdSize(adView.getAdSize().getWidth(), adView.getAdSize().getHeight(), amazonSlotUUID));
                    loader.loadAd(new DTBAdCallback() {
                        @Override
                        public void onFailure(AdError adError) {
                            Log.e("AdError", "Oops banner ad load has failed: " + adError.getMessage());
                            loadAdManagerBanner(bid);
                        }

                        @Override
                        public void onSuccess(DTBAdResponse dtbAdResponse) {
                            Log.d("loadBanner", "success");
                            // Build Google Ad Manager request with APS keywords
                            AdManagerAdRequest.Builder adRequestBuilder = DTBAdUtil.INSTANCE.createAdManagerAdRequestBuilder(dtbAdResponse);
                            if (bid != null) {
                                Log.d("loadBanner", "Criteo bid is not null");
                                Criteo.getInstance().enrichAdObjectWithBid(adRequestBuilder, bid);
                            }
                            if (customTargeting != null) {
                                ReadableMapKeySetIterator iterator = customTargeting.keySetIterator();
                                while (iterator.hasNextKey()) {
                                    String key = iterator.nextKey();
                                    ReadableType type = customTargeting.getType(key);
                                    if (type == ReadableType.String){
                                        adRequestBuilder.addCustomTargeting(key, customTargeting.getString(key));
                                    }
                                }
                            }
                            if (ppid != null) {
                                adRequestBuilder.setPublisherProvidedId(ppid);
                            }
                            final AdManagerAdRequest adRequest = adRequestBuilder.build();
                            adView.loadAd(adRequest);
                        }
                    });
                } else {
                    Log.d("loadBanner", "amazonSlotUUID is null");
                    loadAdManagerBanner(bid);
                }

                // loadAdManagerBanner(bid);
            }
        });
    }

    public void setAdUnitID(String adUnitID) {
        if (this.adUnitID != null) {
            // We can only set adUnitID once, so when it was previously set we have
            // to recreate the view
            this.createAdView();
        }
        this.adUnitID = adUnitID;
        this.adView.setAdUnitId(adUnitID);
    }

    public void setAdType(String adType) {
        this.adType = adType;
    }

    public void setPropAmazonSlotUUID(String amazonSlotUUID) {
        this.amazonSlotUUID = amazonSlotUUID;
    }

    public void setTestDevices(String[] testDevices) {
        this.testDevices = testDevices;
    }

    public void setLocation(ReadableMap location) {
        this.location = location;
    }

    public void setCustomTargeting(ReadableMap customTargeting) {
        this.customTargeting = customTargeting;
    }

    public void setAdSize(AdSize adSize) {
        this.adSize = adSize;
    }

    public void setValidAdSizes(AdSize[] adSizes) {
        this.validAdSizes = adSizes;
    }

    @Override
    public void onAppEvent(String name, String info) {
        WritableMap event = Arguments.createMap();
        event.putString("name", name);
        event.putString("info", info);
        sendEvent(RNGAMBannerViewManager.EVENT_APP_EVENT, event);
    }
}

public class RNGAMBannerViewManager extends ViewGroupManager<ReactPublisherAdView> {

    public static final String REACT_CLASS = "RNGAMBannerView";

    public static final String PROP_AD_SIZE = "adSize";
    public static final String PROP_VALID_AD_SIZES = "validAdSizes";
    public static final String PROP_AD_UNIT_ID = "adUnitID";
    public static final String PROP_AD_TYPE = "adType";
    public static final String PROP_AMAZON_SLOT_UUID = "amazonSlotUUID";
    public static final String PROP_TEST_DEVICES = "testDevices";
    public static final String PROP_LOCATION = "location";
    public static final String PROP_CUSTOM_TARGETING = "customTargeting";

    public static final String EVENT_SIZE_CHANGE = "onSizeChange";
    public static final String EVENT_AD_LOADED = "onAdLoaded";
    public static final String EVENT_AD_FAILED_TO_LOAD = "onAdFailedToLoad";
    public static final String EVENT_AD_OPENED = "onAdOpened";
    public static final String EVENT_AD_CLOSED = "onAdClosed";
    public static final String EVENT_AD_LEFT_APPLICATION = "onAdLeftApplication";
    public static final String EVENT_APP_EVENT = "onAppEvent";

    public static final int COMMAND_LOAD_BANNER = 1;

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    protected ReactPublisherAdView createViewInstance(ThemedReactContext themedReactContext) {
        ReactPublisherAdView adView = new ReactPublisherAdView(themedReactContext);
        return adView;
    }

    @Override
    public void addView(ReactPublisherAdView parent, View child, int index) {
        throw new RuntimeException("RNPublisherBannerView cannot have subviews");
    }

    @Override
    @Nullable
    public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
        MapBuilder.Builder<String, Object> builder = MapBuilder.builder();
        String[] events = {
            EVENT_SIZE_CHANGE,
            EVENT_AD_LOADED,
            EVENT_AD_FAILED_TO_LOAD,
            EVENT_AD_OPENED,
            EVENT_AD_CLOSED,
            EVENT_AD_LEFT_APPLICATION,
            EVENT_APP_EVENT
        };
        for (int i = 0; i < events.length; i++) {
            builder.put(events[i], MapBuilder.of("registrationName", events[i]));
        }
        return builder.build();
    }

    @ReactProp(name = PROP_AD_SIZE)
    public void setPropAdSize(final ReactPublisherAdView view, final String sizeString) {
        AdSize adSize = getAdSizeFromString(sizeString);
        view.setAdSize(adSize);
    }

    @ReactProp(name = PROP_VALID_AD_SIZES)
    public void setPropValidAdSizes(final ReactPublisherAdView view, final ReadableArray adSizeStrings) {
        ReadableNativeArray nativeArray = (ReadableNativeArray)adSizeStrings;
        ArrayList<Object> list = nativeArray.toArrayList();
        String[] adSizeStringsArray = list.toArray(new String[list.size()]);
        AdSize[] adSizes = new AdSize[list.size()];

        for (int i = 0; i < adSizeStringsArray.length; i++) {
                String adSizeString = adSizeStringsArray[i];
                adSizes[i] = getAdSizeFromString(adSizeString);
        }
        view.setValidAdSizes(adSizes);
    }

    @ReactProp(name = PROP_AD_UNIT_ID)
    public void setPropAdUnitID(final ReactPublisherAdView view, final String adUnitID) {
        view.setAdUnitID(adUnitID);
    }

    @ReactProp(name = PROP_AD_TYPE)
    public void setPropAdType(final ReactPublisherAdView view, final String adType) {
        view.setAdType(adType);
    }

    @ReactProp(name = PROP_AMAZON_SLOT_UUID)
    public void setPropAmazonSlotUUID(final ReactPublisherAdView view, final String amazonSlotUUID) {
        view.setPropAmazonSlotUUID(amazonSlotUUID);
    }

    @ReactProp(name = PROP_TEST_DEVICES)
    public void setPropTestDevices(final ReactPublisherAdView view, final ReadableArray testDevices) {
        ReadableNativeArray nativeArray = (ReadableNativeArray)testDevices;
        ArrayList<Object> list = nativeArray.toArrayList();
        view.setTestDevices(list.toArray(new String[list.size()]));
    }

    @ReactProp(name = PROP_LOCATION)
    public void setPropLocation(final ReactPublisherAdView view, final ReadableMap location) {
        view.setLocation(location);
    }

     @ReactProp(name = PROP_CUSTOM_TARGETING)
    public void setCustomTargeting(final ReactPublisherAdView view, final ReadableMap customTargeting) {
        view.setCustomTargeting(customTargeting);
    }

    private AdSize getAdSizeFromString(String adSize) {
        switch (adSize) {
            case "banner":
                return AdSize.BANNER;
            case "largeBanner":
                return AdSize.LARGE_BANNER;
            case "mediumRectangle":
                return AdSize.MEDIUM_RECTANGLE;
            case "fullBanner":
                return AdSize.FULL_BANNER;
            case "leaderBoard":
                return AdSize.LEADERBOARD;
            case "smartBannerPortrait":
                return AdSize.SMART_BANNER;
            case "smartBannerLandscape":
                return AdSize.SMART_BANNER;
            case "smartBanner":
                return AdSize.SMART_BANNER;
            default:
                return AdSize.BANNER;
        }
    }

    @Nullable
    @Override
    public Map<String, Integer> getCommandsMap() {
        return MapBuilder.of("loadBanner", COMMAND_LOAD_BANNER);
    }

    @Override
    public void receiveCommand(ReactPublisherAdView root, int commandId, @javax.annotation.Nullable ReadableArray args) {
        switch (commandId) {
            case COMMAND_LOAD_BANNER:
                root.loadBanner();
                break;
        }
    }
}
