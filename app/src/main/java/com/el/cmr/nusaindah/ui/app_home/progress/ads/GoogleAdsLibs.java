package com.el.cmr.nusaindah.ui.app_home.progress.ads;


import static com.el.cmr.nusaindah.ui.app_dashboard.utils.GLibs.CONTENT_ADS;
import static com.el.cmr.nusaindah.ui.app_dashboard.utils.GLibs.INTERS_FIRST;
import static com.el.cmr.nusaindah.ui.app_dashboard.utils.GLibs.INTERS_SECOND;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.util.ArrayList;
import java.util.List;

public class GoogleAdsLibs {
    public static InterstitialAd _interstitialAd;

    private Context mContext;

    public GoogleAdsLibs (Context context){
        this.mContext = context;
    }

    public void admobInit(){
        MobileAds.initialize(mContext, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {}
        });
    }

    public void interstitialCreated(Boolean firstOpen){
        AdRequest adRequest = new AdRequest.Builder().build();

        String intersAd = INTERS_FIRST;
        if(!firstOpen){intersAd = INTERS_SECOND;}

        InterstitialAd.load(mContext, intersAd,
                adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@androidx.annotation.NonNull InterstitialAd interstitialAd) {
                        _interstitialAd = interstitialAd;
                    }

                    @Override
                    public void onAdFailedToLoad(@androidx.annotation.NonNull LoadAdError loadAdError) {
                        _interstitialAd = null;
                    }
                });
    }

    public InterstitialAd interstitialLoaded(){
        return _interstitialAd;
    }

    private List<Integer> imgAdPosition = new ArrayList<>();
    public List<Object> imgFeedAd(List<Object> ObJectDataList, int index_ads, int item_ads, Activity activity){
        imgAdPosition.clear();
        for (int i = index_ads; i < ObJectDataList.size(); i += item_ads) {
            final AdView adView = new AdView(mContext);
            adView.setAdSize(imgAdSize(activity));
            adView.setAdUnitId(CONTENT_ADS);
            ObJectDataList.add(i, adView);
            imgAdPosition.add(i);
        }

        if (!imgAdPosition.isEmpty()) {
            for (int x = 0; x < imgAdPosition.size(); x++) {
                loadImgAd(ObJectDataList, imgAdPosition.get(x));
            }
        }
        return ObJectDataList;
    }

    private void loadImgAd(List<Object> dataList, final int index){
        if (index >= dataList.size()) {return;}

        Object item = dataList.get(index);
        if (!(item instanceof AdView)) {}

        final AdView adView = (AdView) item;

        adView.setAdListener(
                new AdListener() {
                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {}
                });
        adView.loadAd(new AdRequest.Builder().build());
    }

    private AdSize imgAdSize(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;
        int adWidth = (int) (widthPixels / density);

        return AdSize.getInlineAdaptiveBannerAdSize(adWidth, 250);
    }
}
