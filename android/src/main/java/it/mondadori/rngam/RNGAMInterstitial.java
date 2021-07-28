package it.mondadori.rngam;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import android.os.Bundle;
import android.location.Location;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableNativeArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.admanager.AdManagerInterstitialAd;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.android.gms.ads.admanager.AdManagerInterstitialAdLoadCallback;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.LoadAdError;
import com.google.ads.mediation.admob.AdMobAdapter;

import com.amazon.device.ads.*;

import com.criteo.publisher.Bid;
import com.criteo.publisher.BidResponseListener;
import com.criteo.publisher.Criteo;
import com.criteo.publisher.model.InterstitialAdUnit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RNGAMInterstitial extends ReactContextBaseJavaModule {

    public static final String REACT_CLASS = "RNGAMInterstitial";

    public static final String EVENT_AD_LOADED = "interstitialAdLoaded";
    public static final String EVENT_AD_FAILED_TO_LOAD = "interstitialAdFailedToLoad";
    public static final String EVENT_AD_OPENED = "interstitialAdOpened";
    public static final String EVENT_AD_CLOSED = "interstitialAdClosed";
    public static final String EVENT_AD_LEFT_APPLICATION = "interstitialAdLeftApplication";

    String[] testDevices;
    ReadableMap location;
    String amazonSlotUUID;
    String adUnitID;

    private Promise mRequestAdPromise;
    private final ReactApplicationContext mReactApplicationContext;
    private AdManagerInterstitialAd mAdManagerInterstitialAd;

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    public RNGAMInterstitial(ReactApplicationContext reactContext) {
        super(reactContext);
        mReactApplicationContext = reactContext;     
    }
    private void sendEvent(String eventName, @Nullable WritableMap params) {
        getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);
    }

    @ReactMethod
    public void setAdUnitID(String adUnitID, String amazonSlotUUID) {
        
        this.amazonSlotUUID = amazonSlotUUID;
        this.adUnitID = adUnitID;

        // if (mInterstitialAd.getAdUnitId() == null) {
        //     mInterstitialAd.setAdUnitId(adUnitID);
        //     mInterstitialAds.put(adUnitID, mInterstitialAd);
        //     return;
        // }

        // // already current
        // if( mInterstitialAd.getAdUnitId() == adUnitID ){
        //     return;
        // }

        // // check for existing interstitial matching adUnitID, 
        // final AdManagerInterstitialAd interstitialAd = mInterstitialAds.get(adUnitID);

        // // existing found, make current
        // if(interstitialAd != null ){
        //     mInterstitialAd = interstitialAd;
        //     return;
        // }

        // // create new interstitial, store and make current
        // final AdManagerInterstitialAd newInterstitialAd = new AdManagerInterstitialAd(mReactApplicationContext);
        // new Handler(Looper.getMainLooper()).post(new Runnable() {
        //     @Override
        //     public void run() {
        //         newInterstitialAd.setAdListener( adListener );
        //     }
        // });
        // newInterstitialAd.setAdUnitId(adUnitID);
        // mInterstitialAds.put(adUnitID, newInterstitialAd);
        // mInterstitialAd = newInterstitialAd;
    }

    @ReactMethod
    public void setTestDevices(ReadableArray testDevices) {
        ReadableNativeArray nativeArray = (ReadableNativeArray)testDevices;
        ArrayList<Object> list = nativeArray.toArrayList();
        this.testDevices = list.toArray(new String[list.size()]);
    }

    @ReactMethod
    public void setLocation(ReadableMap location) {
        this.location = location;
    }

    private void loadAdManagerBanner(@Nullable Bid bid) {
        AdManagerAdRequest.Builder adRequestBuilder = new AdManagerAdRequest.Builder();
        // if (testDevices != null) {
        //     for (int i = 0; i < testDevices.length; i++) {
        //         String testDevice = testDevices[i];
        //         if (testDevice == "SIMULATOR") {
        //             testDevice = AdRequest.DEVICE_ID_EMULATOR;
        //         }
        //         adRequestBuilder.addTestDevice(testDevice);
        //     }
        // }

        if (location != null && location.hasKey("latitude") && !location.isNull("latitude") && location.hasKey("longitude") && !location.isNull("longitude")) {
            Location advLocation = new Location("");
            advLocation.setLatitude(location.getDouble("latitude"));
            advLocation.setLongitude(location.getDouble("longitude"));

            adRequestBuilder.setLocation(advLocation);
        }

        if (bid != null) {
            Criteo.getInstance().enrichAdObjectWithBid(adRequestBuilder, bid);
        }

        AdManagerAdRequest adRequest = adRequestBuilder.build();
        AdManagerInterstitialAd.load(mReactApplicationContext,this.adUnitID, adRequest,
            new AdManagerInterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull AdManagerInterstitialAd interstitialAd) {
                    // The mAdManagerInterstitialAd reference will be null until
                    // an ad is loaded.
                    mAdManagerInterstitialAd = interstitialAd;
                    Log.i("RNGAMInterstitial", "onAdLoaded");

                    mAdManagerInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            // Called when fullscreen content is dismissed.
                            Log.d("RNGAMInterstitial", "The ad was dismissed.");
                            sendEvent(EVENT_AD_CLOSED, null);
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(AdError adError) {
                            // Called when fullscreen content failed to show.
                            String errorMessage = adError.getMessage();
                            
                            Log.d("RNGAMInterstitial", adError.toString());
                            
                            WritableMap event = Arguments.createMap();
                            event.putString("message", errorMessage);
                            sendEvent(EVENT_AD_FAILED_TO_LOAD, event);
                        }

                        @Override
                        public void onAdShowedFullScreenContent() {
                            // Called when fullscreen content is shown.
                            // Make sure to set your reference to null so you don't
                            // show it a second time.
                            mAdManagerInterstitialAd = null;
                            Log.d("RNGAMInterstitial", "The ad was shown.");
                            sendEvent(EVENT_AD_OPENED, null);
                        }
                    });
                    sendEvent(EVENT_AD_LOADED, null);
                    mRequestAdPromise.resolve(null);
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    // Handle the error
                    String errorMessage = loadAdError.getMessage();
                    Log.i("RNGAMInterstitial", errorMessage);
                    mAdManagerInterstitialAd = null;
                    mRequestAdPromise.reject("" + loadAdError.getCode(), errorMessage);
                }
            });
    }

    @ReactMethod
    public void requestAd(final Promise promise) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run () {
                
                mRequestAdPromise = promise;
                // load Criteo bids
                InterstitialAdUnit criteoBannerAdUnit = (InterstitialAdUnit) RNGAMConfig.getInstance().getGAM2Criteo().get("interstitial");
                
                Criteo.getInstance().loadBid(criteoBannerAdUnit, new BidResponseListener() {
                    @Override
                    public void onResponse(final @Nullable Bid bid) {
                        // AMAZON
                        if(amazonSlotUUID != null) {
                            final DTBAdRequest loader = new DTBAdRequest();
                            loader.setSizes(new DTBAdSize.DTBInterstitialAdSize(amazonSlotUUID));
                            loader.loadAd(new DTBAdCallback() {


                                @Override
                                public void onFailure(com.amazon.device.ads.AdError adError) {
                                    Log.e("APP", "Failed to get the interstitial ad from Amazon" + adError.getMessage());
                                    loadAdManagerBanner(bid);
                                }

                                @Override
                                public void onSuccess(DTBAdResponse dtbAdResponse) {
                                    AdManagerAdRequest.Builder adRequestBuilder = DTBAdUtil.INSTANCE.createAdManagerAdRequestBuilder(dtbAdResponse);
                                    if (bid != null) {
                                        Log.d("loadInterstitialBanner", "Criteo bid is not null");
                                        Criteo.getInstance().enrichAdObjectWithBid(adRequestBuilder, bid);
                                    }
                                    AdManagerAdRequest adRequest = adRequestBuilder.build();
        
                                    AdManagerInterstitialAd.load(mReactApplicationContext, adUnitID, adRequest, new AdManagerInterstitialAdLoadCallback() {
                                        @Override
                                        public void onAdLoaded(@NonNull AdManagerInterstitialAd adManagerInterstitialAd) {
                                            super.onAdLoaded(adManagerInterstitialAd);
                                            mAdManagerInterstitialAd = adManagerInterstitialAd;
                                            mAdManagerInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                                                @Override
                                                public void onAdDismissedFullScreenContent() {
                                                    super.onAdDismissedFullScreenContent();
                                                    Log.d("RNGAMInterstitial", "The ad was dismissed.");
                                                    sendEvent(EVENT_AD_CLOSED, null);
                                                }

                                                @Override
                                                public void onAdShowedFullScreenContent() {
                                                    super.onAdShowedFullScreenContent();
                                                    mAdManagerInterstitialAd = null;
                                                    Log.d("RNGAMInterstitial", "The ad was shown.");
                                                    sendEvent(EVENT_AD_OPENED, null);
                                                }

                                                @Override
                                                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                                                    super.onAdFailedToShowFullScreenContent(adError);
                                                    String errorMessage = adError.getMessage();
                            
                                                    Log.d("RNGAMInterstitial", adError.toString());
                                                    
                                                    WritableMap event = Arguments.createMap();
                                                    event.putString("message", errorMessage);
                                                    sendEvent(EVENT_AD_FAILED_TO_LOAD, event);
                                                }
                                            });
                                        }

                                        @Override
                                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                                            super.onAdFailedToLoad(loadAdError);
                                        }
                                    });
                                }
                            });
                        }  else {
                            Log.d("loadInterstitialBanner", "amazonSlotUUID is null");
                            loadAdManagerBanner(bid);
                        }
                    }
                });
            }
        });
    }

    @ReactMethod
    public void showAd(final Promise promise) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run () {
                if (mAdManagerInterstitialAd != null) {
                    mAdManagerInterstitialAd.show(getCurrentActivity());
                    promise.resolve(null);
                } else {
                    Log.d("RNGAMInterstitial", "The interstitial ad wasn't ready yet.");
                    promise.reject("E_AD_NOT_READY", "Ad is not ready.");
                }
            }
        });
    }
}
