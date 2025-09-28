package com.el.cmr.nusaindah.ui.my_detail;

import static com.el.cmr.nusaindah.ui.app_dashboard.utils.GLibs.BANNER_ADS;
import static com.el.cmr.nusaindah.ui.app_dashboard.utils.GLibs.CONTENT_ADS;
import static com.el.cmr.nusaindah.ui.app_dashboard.utils.GLibs.INTERS_BACK_STATUS;
import static com.el.cmr.nusaindah.ui.app_dashboard.utils.GLibs.INTERS_SECOND_STATUS;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.el.cmr.nusaindah.R;
import com.el.cmr.nusaindah.databinding.ActivityContentBinding;
import com.el.cmr.nusaindah.databinding.ActivityPreviewBinding;
import com.el.cmr.nusaindah.ui.app_dashboard.preview.PreviewActivity;
import com.el.cmr.nusaindah.ui.app_dashboard.preview.PreviewAdapter;
import com.el.cmr.nusaindah.ui.app_dashboard.utils.GPref;
import com.el.cmr.nusaindah.ui.app_dashboard.utils.ItemDecoration;
import com.el.cmr.nusaindah.ui.app_home.progress.ads.GoogleAdsLibs;
import com.el.cmr.nusaindah.ui.download.FirebaseDownloadManager;
import com.el.cmr.nusaindah.ui.my_home.SQLiteHelper;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.squareup.picasso.Picasso;
import com.el.cmr.nusaindah.ui.download.SafDownloadManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ContentActivity extends AppCompatActivity {

    private View view;
    private Activity activity = ContentActivity.this;
    private ActivityContentBinding binding;

    private SQLiteHelper sqLiteHelper;
    private List<Object> dataListMore = new ArrayList<>();
    private RecyclerView.Adapter<RecyclerView.ViewHolder> moreAdapter;

    private GoogleAdsLibs googleAdsLibs;
    private Intent intent;
    private Bundle extras;
    private GPref gPref;
    private InterstitialAd _interstitialAd;
    private SafDownloadManager downloadManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityContentBinding.inflate(getLayoutInflater());
        view = binding.getRoot();
        setContentView(view);

        intent = getIntent();
        extras = intent.getExtras();

        gPref = new GPref(getApplicationContext());

        googleAdsLibs = new GoogleAdsLibs(getApplicationContext());
        _interstitialAd = googleAdsLibs.interstitialLoaded();

        sqLiteHelper = new SQLiteHelper(getApplicationContext());
        downloadManager = new SafDownloadManager(this);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(activity);
        binding.recyclerviewMore.setLayoutManager(linearLayoutManager);
        binding.recyclerviewMore.setHasFixedSize(true);
        binding.recyclerviewMore.addItemDecoration(new ItemDecoration(20));

        if(!gPref.getIndStatus(INTERS_SECOND_STATUS) && _interstitialAd != null){
            _interstitialAd.show(activity);
            _interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent();
                    gPref.setIndStatus(INTERS_SECOND_STATUS, true);
                    showContent();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    super.onAdFailedToShowFullScreenContent(adError);
                    googleAdsLibs.interstitialCreated(false);
                    gPref.setIndStatus(INTERS_SECOND_STATUS, false);
                    showContent();
                }
            });
        }else{
            googleAdsLibs.interstitialCreated(false);
            gPref.setIndStatus(INTERS_SECOND_STATUS, false);
            showContent();
        }
        getOnBackPressedDispatcher().addCallback( this, onBackPressedCallback );
    }

    OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback( true ) {
        @Override
        public void handleOnBackPressed() {
            try {
                if(_interstitialAd != null && !gPref.getIndStatus(INTERS_BACK_STATUS)){
                    try {
                        _interstitialAd.show(activity);
                        _interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                super.onAdDismissedFullScreenContent();
                                gPref.setIndStatus(INTERS_BACK_STATUS, true);
                                finish();
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                                super.onAdFailedToShowFullScreenContent(adError);
                                finish();
                            }
                        });
                    }catch (Exception e){finish();}
                }else{
                    finish();
                }
            }catch (Exception e){
                finish();
            }
        }
    };

    private void showContent(){
        Picasso.get()
                .load(extras.getString("image_url"))
                .placeholder(R.drawable.img_mod_placeholder)
                .error(R.drawable.img_mod_placeholder)
                .fit()
                .centerCrop()
                .into(binding.imageMod);

        binding.titleMod.setText(extras.getString("title") + " " +
                getResources().getString(R.string.title_map));
        binding.textPreview.setText("PREVIEW " + extras.getString("title"));
        binding.textDownload.setText(extras.getString("title") + "\n" + extras.getString("name"));
        showNativeAd(binding.adBannerNative);
        showMore();

        binding.btnImagePreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(activity, PreviewActivity.class);
                intent.putExtra("mod_id", extras.getInt("mod_id"));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

        binding.btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(extras.getString("download_url")));
//                startActivity(browserIntent);
                startSafDownload();
            }
        });

//        binding.btnYoutube.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.url_apply_map)));
//                startActivity(browserIntent);
//            }
//        });
    }

    private void startSafDownload() {
        // Check if download already in progress
        if (downloadManager.isDownloading()) {
            Toast.makeText(this, "Download already in progress", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get Firebase Storage path and filename from database
        String firebaseStoragePath = extras.getString("download_url", "");
        String fileName = extras.getString("name", "addon_file.mcaddon");

        Log.d("ContentActivity", "Firebase Storage Path: " + firebaseStoragePath);
        Log.d("ContentActivity", "File Name: " + fileName);

        // Validate Firebase Storage path
        if (firebaseStoragePath.isEmpty()) {
            Toast.makeText(this, "Invalid download link", Toast.LENGTH_SHORT).show();
            return;
        }

        // Start SAF download
        downloadManager.startDownload(firebaseStoragePath, fileName,
                new SafDownloadManager.DownloadCallback() {
                    @Override
                    public void onDownloadStart() {
                        Log.d("ContentActivity", "Download started for: " + fileName);
                        binding.btnDownload.setEnabled(false);
                        binding.btnDownload.setText("Downloading...");
                    }

                    @Override
                    public void onProgress(int progress, long downloaded, long total, String speed) {
                        Log.d("ContentActivity", "Download progress: " + progress + "% - " + speed);
                    }

                    @Override
                    public void onSuccess(String filePath) {
                        Log.d("ContentActivity", "Download completed: " + filePath);

                        binding.btnDownload.setEnabled(true);
                        binding.btnDownload.setText("DOWNLOAD");

                        Toast.makeText(ContentActivity.this,
                                "Download completed successfully!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("ContentActivity", "Download error: " + error);

                        binding.btnDownload.setEnabled(true);
                        binding.btnDownload.setText("DOWNLOAD");

                        Toast.makeText(ContentActivity.this,
                                "Download failed: " + error, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCancelled() {
                        Log.d("ContentActivity", "Download cancelled by user");

                        binding.btnDownload.setEnabled(true);
                        binding.btnDownload.setText("DOWNLOAD");

                        Toast.makeText(ContentActivity.this,
                                "Download cancelled", Toast.LENGTH_SHORT).show();
                    }
                });
    }

//    private void startFirebaseDownload() {
//        // Check if download already in progress
//        if (downloadManager.isDownloading()) {
//            Toast.makeText(this, "Download already in progress", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        // Get Firebase Storage path and filename from database
//        String firebaseStoragePath = extras.getString("download_url", "");
//        String fileName = extras.getString("name", "addon_file.mcaddon");
//
//        Log.d("ContentActivity", "Firebase Storage Path: " + firebaseStoragePath);
//        Log.d("ContentActivity", "File Name: " + fileName);
//
//        // Validate Firebase Storage path
//        if (firebaseStoragePath.isEmpty()) {
//            Toast.makeText(this, "Invalid download link", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        // Start download with modern UI
//        downloadManager.startDownload(firebaseStoragePath, fileName,
//                new FirebaseDownloadManager.DownloadCallback() {
//                    @Override
//                    public void onDownloadStart() {
//                        Log.d("ContentActivity", "Download started for: " + fileName);
//                        // Optional: Hide download button temporarily
//                        binding.btnDownload.setEnabled(false);
//                        binding.btnDownload.setText("Downloading...");
//                    }
//
//                    @Override
//                    public void onProgress(int progress, long downloaded, long total, String speed) {
//                        // Progress is automatically shown in modern dialog
//                        Log.d("ContentActivity", "Download progress: " + progress + "% - " + speed);
//                    }
//
//                    @Override
//                    public void onSuccess(String filePath) {
//                        Log.d("ContentActivity", "Download completed: " + filePath);
//
//                        // Re-enable download button
//                        binding.btnDownload.setEnabled(true);
//                        binding.btnDownload.setText("DOWNLOAD");
//
//                        Toast.makeText(ContentActivity.this,
//                                "Download completed successfully!", Toast.LENGTH_SHORT).show();
//
//                        // Optional: Analytics tracking
//                        // Analytics.logEvent("addon_downloaded", bundleWith("addon_name", fileName));
//                    }
//
//                    @Override
//                    public void onError(String error) {
//                        Log.e("ContentActivity", "Download error: " + error);
//
//                        // Re-enable download button
//                        binding.btnDownload.setEnabled(true);
//                        binding.btnDownload.setText("DOWNLOAD");
//
//                        Toast.makeText(ContentActivity.this,
//                                "Download failed: " + error, Toast.LENGTH_LONG).show();
//                    }
//
//                    @Override
//                    public void onCancelled() {
//                        Log.d("ContentActivity", "Download cancelled by user");
//
//                        // Re-enable download button
//                        binding.btnDownload.setEnabled(true);
//                        binding.btnDownload.setText("DOWNLOAD");
//
//                        Toast.makeText(ContentActivity.this,
//                                "Download cancelled", Toast.LENGTH_SHORT).show();
//                    }
//                });
//    }

    private void showMore(){
        dataListMore.addAll(sqLiteHelper.getDataMoreActivity(extras.getInt("mod_id")));
        moreAdapter = new MoreAdapter(activity, dataListMore);
        binding.recyclerviewMore.setAdapter(moreAdapter);
        moreAdapter.notifyDataSetChanged();
        binding.recyclerviewMore.setVisibility(View.VISIBLE);
    }

    private void showNativeAd(FrameLayout adContainer){
        AdView adView = new AdView(getApplicationContext());
        adView.setAdUnitId(CONTENT_ADS);
        adContainer.removeAllViews();
        adContainer.addView(adView);

        AdSize adSize = nativeAdSize();
        adView.setAdSize(adSize);

        AdRequest adRequest = new AdRequest.Builder().build();
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull @NotNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                adContainer.setVisibility(View.GONE);
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                adContainer.setVisibility(View.VISIBLE);
            }
        });
        adView.loadAd(adRequest);
    }

    private AdSize nativeAdSize() {
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;

        int adWidth = (int) (widthPixels / density);
        return AdSize.getInlineAdaptiveBannerAdSize(adWidth, 260);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // TAMBAHAN: Cleanup download manager
        if (downloadManager != null) {
            downloadManager.cleanup();
        }
    }
}