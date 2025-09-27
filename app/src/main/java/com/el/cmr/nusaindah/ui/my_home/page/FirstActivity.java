package com.el.cmr.nusaindah.ui.my_home.page;

import static com.el.cmr.nusaindah.ui.app_dashboard.utils.GLibs.INTERS_FIRST_STATUS;
import static com.el.cmr.nusaindah.ui.app_dashboard.utils.GLibs.INTERS_SECOND_STATUS;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.el.cmr.nusaindah.R;
import com.el.cmr.nusaindah.databinding.ActivityFirstBinding;
import com.el.cmr.nusaindah.ui.app_dashboard.utils.GPref;
import com.el.cmr.nusaindah.ui.app_home.progress.ads.ConsentLibs;
import com.el.cmr.nusaindah.ui.app_home.progress.ads.GoogleAdsLibs;
import com.el.cmr.nusaindah.ui.app_notifications.main_page.HomeActivity;
import com.el.cmr.nusaindah.ui.my_home.SQLiteHelper;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.interstitial.InterstitialAd;

import java.io.IOException;

public class FirstActivity extends AppCompatActivity {

    private SQLiteHelper sqLiteHelper;

    private View view;
    private Activity activity = FirstActivity.this;
    private ActivityFirstBinding binding;

    private GoogleAdsLibs googleAdsLibs;
    private ConsentLibs consentLibs;
    private InterstitialAd _interstitialAd;
    private GPref gPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        binding = ActivityFirstBinding.inflate(getLayoutInflater());
        view = binding.getRoot();
        setContentView(view);

        gPref = new GPref(getApplicationContext());
        googleAdsLibs = new GoogleAdsLibs(getApplicationContext());
        consentUserAd();

        binding.btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _interstitialAd = googleAdsLibs.interstitialLoaded();
                if(_interstitialAd!=null){
                    _interstitialAd.show(activity);
                    _interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            super.onAdDismissedFullScreenContent();
                            gPref.setIndStatus(INTERS_SECOND_STATUS, true);
                            goActivity();
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                            super.onAdFailedToShowFullScreenContent(adError);
                            gPref.setIndStatus(INTERS_SECOND_STATUS, false);
                            goActivity();
                        }
                    });
                }else{
                    gPref.setIndStatus(INTERS_SECOND_STATUS, false);
                    goActivity();
                }
            }
        });
    }

    private void copyDBApps(){
        sqLiteHelper = new SQLiteHelper(activity);
        try {
            sqLiteHelper.createParkourDB();
        } catch (IOException ioe) {
            throw new Error("Copy Database Gagal");
        }
    }

    private void consentUserAd(){
        consentLibs = ConsentLibs.getInstance(getApplicationContext());
        consentLibs.gatherConsent(
                this,
                consentError -> {
                    if (consentError != null) {
                        initAds();
                    }

                    if (consentLibs.canRequestAds()) {
                        initAds();
                    }
                });
    }

    private void initAds(){
        googleAdsLibs.admobInit();
        googleAdsLibs.interstitialCreated(true);
        copyDBApps();
        try{
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    binding.btnNext.setVisibility(View.VISIBLE);
                    binding.layoutProgress.setVisibility(View.GONE);
                }
            }, 8000);
        }catch (Exception e){
            binding.btnNext.setVisibility(View.VISIBLE);
            binding.layoutProgress.setVisibility(View.GONE);
        }
    }

    private void goActivity(){
        Intent intent = new Intent(activity, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}