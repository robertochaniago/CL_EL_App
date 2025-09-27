package com.el.cmr.nusaindah.ui.app_notifications.main_page;

import static android.view.View.VISIBLE;
import static com.el.cmr.nusaindah.ui.app_dashboard.utils.GLibs.BANNER_ADS;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.el.cmr.nusaindah.R;
import com.el.cmr.nusaindah.databinding.ModLayoutBinding;
import com.el.cmr.nusaindah.databinding.ModSingleLayoutBinding;
import com.el.cmr.nusaindah.ui.app_dashboard.preview.PreviewActivity;
import com.el.cmr.nusaindah.ui.download.FirebaseDownloadManager;
import com.el.cmr.nusaindah.ui.my_detail.ContentActivity;
import com.el.cmr.nusaindah.ui.my_home.page.model.HomeModel;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.squareup.picasso.Picasso;
import com.el.cmr.nusaindah.ui.download.SafDownloadManager;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class HomeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    static List<Object> recyclerviewList;
    static Context context;
    //private FirebaseDownloadManager downloadManager;
    private SafDownloadManager downloadManager;
    static final int FIRST_ROW = 1;
    static final int SECOND_ROW = 2;
    static final int OTHER_ROW = 3;

    public HomeAdapter(Context context, List<Object> recyclerviewList){
        this.context = context;
        this.recyclerviewList = recyclerviewList;
        this.downloadManager = new SafDownloadManager((AppCompatActivity) context);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == FIRST_ROW){
            return new MainViewHolder(ModSingleLayoutBinding.inflate(LayoutInflater.from(parent.getContext()),
                    parent, false));
        }else{
            return new ModViewHolder(ModLayoutBinding.inflate(LayoutInflater.from(parent.getContext()),
                    parent, false));
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        switch (holder.getItemViewType()) {
            case FIRST_ROW:
                MainViewHolder mainViewHolder = (MainViewHolder) holder;
                HomeModel main = (HomeModel) recyclerviewList.get(position);

                Picasso.get()
                        .load(main.getImage_url())
                        .placeholder(R.drawable.img_mod_placeholder)
                        .error(R.drawable.img_mod_placeholder)
                        .fit()
                        .centerCrop()
                        .into(mainViewHolder.modSingleLayoutBinding.imageMod);

                mainViewHolder.modSingleLayoutBinding.titleMod.setText(main.getTitle() + " " +
                        context.getResources().getString(R.string.title_map));

                showNativeAd(mainViewHolder.modSingleLayoutBinding.adBannerNative);
                mainViewHolder.modSingleLayoutBinding.textPreview.setText("PREVIEW " + main.getTitle());
                mainViewHolder.modSingleLayoutBinding.textDownload.setText(main.getTitle() + "\n("+ main.getName() +")");

                mainViewHolder.modSingleLayoutBinding.btnImagePreview.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(context, PreviewActivity.class);
                        intent.putExtra("mod_id", main.getMod_id());
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    }
                });

                mainViewHolder.modSingleLayoutBinding.btnDownload.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startSafDownload(mainViewHolder, main);
                    }
                });

                mainViewHolder.modSingleLayoutBinding.btnYoutube.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(context.getResources().getString(R.string.url_apply_map)));
                        context.startActivity(browserIntent);
                    }
                });
                break;
            case SECOND_ROW:
                ModViewHolder secondModViewHolder = (ModViewHolder) holder;
                HomeModel secondData = (HomeModel) recyclerviewList.get(position);

                secondModViewHolder.modLayoutBinding.textOther.setVisibility(VISIBLE);

                Picasso.get()
                        .load(secondData.getImage_url())
                        .placeholder(R.drawable.img_mod_placeholder)
                        .error(R.drawable.img_mod_placeholder)
                        .fit()
                        .centerCrop()
                        .into(secondModViewHolder.modLayoutBinding.imageMod);

                secondModViewHolder.modLayoutBinding.titleMod.setText(secondData.getTitle() + " " +
                        context.getResources().getString(R.string.title_map));
                secondModViewHolder.modLayoutBinding.rootLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(context, ContentActivity.class);
                        intent.putExtra("mod_id", secondData.getMod_id());
                        intent.putExtra("title", secondData.getTitle());
                        intent.putExtra("name", secondData.getName());
                        intent.putExtra("image_url", secondData.getImage_url());
                        intent.putExtra("download_url", secondData.getDownload_url());
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    }
                });
            case OTHER_ROW:
                ModViewHolder modViewHolder = (ModViewHolder) holder;
                HomeModel data = (HomeModel) recyclerviewList.get(position);

                Picasso.get()
                        .load(data.getImage_url())
                        .placeholder(R.drawable.img_mod_placeholder)
                        .error(R.drawable.img_mod_placeholder)
                        .fit()
                        .centerCrop()
                        .into(modViewHolder.modLayoutBinding.imageMod);

                modViewHolder.modLayoutBinding.titleMod.setText(data.getTitle() + " " +
                        context.getResources().getString(R.string.title_map));
                modViewHolder.modLayoutBinding.rootLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(context, ContentActivity.class);
                        intent.putExtra("mod_id", data.getMod_id());
                        intent.putExtra("title", data.getTitle());
                        intent.putExtra("name", data.getName());
                        intent.putExtra("image_url", data.getImage_url());
                        intent.putExtra("download_url", data.getDownload_url());
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    }
                });
                break;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return recyclerviewList.size();
    }

    @Override
    public int getItemViewType(int position) {
        if(position==0){
            return FIRST_ROW;
        }else if(position==1){
            return SECOND_ROW;
        }else{
            return OTHER_ROW;
        }
    }

    public class ModViewHolder extends RecyclerView.ViewHolder {
        private ModLayoutBinding modLayoutBinding;
        ModViewHolder(ModLayoutBinding binding) {
            super(binding.getRoot());
            modLayoutBinding = binding;
        }
    }

    public class MainViewHolder extends RecyclerView.ViewHolder {
        private ModSingleLayoutBinding modSingleLayoutBinding;
        MainViewHolder(ModSingleLayoutBinding binding) {
            super(binding.getRoot());
            modSingleLayoutBinding = binding;
        }
    }

    private void showNativeAd(FrameLayout adContainer){
        AdView adView = new AdView(context);
        adView.setAdUnitId(BANNER_ADS);
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
                adContainer.setVisibility(VISIBLE);
            }
        });
        adView.loadAd(adRequest);
    }

    private AdSize nativeAdSize() {
        Activity activity = (Activity) context;
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;

        int adWidth = (int) (widthPixels / density);
        return AdSize.getInlineAdaptiveBannerAdSize(adWidth, 260);
    }

    private void startSafDownload(MainViewHolder mainViewHolder, HomeModel main) {
        // Check if download already in progress
        if (downloadManager.isDownloading()) {
            Toast.makeText(context, "Download already in progress", Toast.LENGTH_SHORT).show();
            return;
        }

        String firebaseStoragePath = main.getDownload_url();
        String fileName = main.getName();

        Log.d("HomeAdapter", "Firebase Storage Path: " + firebaseStoragePath);
        Log.d("HomeAdapter", "File Name: " + fileName);

        // Validate Firebase Storage path
        if (firebaseStoragePath.isEmpty()) {
            Toast.makeText(context, "Invalid download link", Toast.LENGTH_SHORT).show();
            return;
        }

        // Start SAF download
        downloadManager.startDownload(firebaseStoragePath, fileName,
                new SafDownloadManager.DownloadCallback() {
                    @Override
                    public void onDownloadStart() {
                        Log.d("HomeAdapter", "Download started for: " + fileName);
                        mainViewHolder.modSingleLayoutBinding.btnDownload.setEnabled(false);
                        mainViewHolder.modSingleLayoutBinding.btnDownload.setText("Downloading...");
                    }

                    @Override
                    public void onProgress(int progress, long downloaded, long total, String speed) {
                        Log.d("HomeAdapter", "Download progress: " + progress + "% - " + speed);
                    }

                    @Override
                    public void onSuccess(String filePath) {
                        Log.d("HomeAdapter", "Download completed: " + filePath);

                        mainViewHolder.modSingleLayoutBinding.btnDownload.setEnabled(true);
                        mainViewHolder.modSingleLayoutBinding.btnDownload.setText("DOWNLOAD");

                        Toast.makeText(context,
                                "Download completed successfully!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("HomeAdapter", "Download error: " + error);

                        mainViewHolder.modSingleLayoutBinding.btnDownload.setEnabled(true);
                        mainViewHolder.modSingleLayoutBinding.btnDownload.setText("DOWNLOAD");

                        Toast.makeText(context,
                                "Download failed: " + error, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCancelled() {
                        Log.d("HomeAdapter", "Download cancelled by user");

                        mainViewHolder.modSingleLayoutBinding.btnDownload.setEnabled(true);
                        mainViewHolder.modSingleLayoutBinding.btnDownload.setText("DOWNLOAD");

                        Toast.makeText(context,
                                "Download cancelled", Toast.LENGTH_SHORT).show();
                    }
                });
    }

//    private void startFirebaseDownload(MainViewHolder mainViewHolder, HomeModel main) {
//
//        // Check if download already in progress
//        if (downloadManager.isDownloading()) {
//            Toast.makeText(context, "Download already in progress", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        // Get Firebase Storage path and filename from database
//        String firebaseStoragePath = main.getDownload_url(); //extras.getString("download_url", "");
//        String fileName = main.getName(); //extras.getString("name", "addon_file.mcaddon");
//
//        Log.d("ContentActivity", "Firebase Storage Path: " + firebaseStoragePath);
//        Log.d("ContentActivity", "File Name: " + fileName);
//
//        // Validate Firebase Storage path
//        if (firebaseStoragePath.isEmpty()) {
//            Toast.makeText(context, "Invalid download link", Toast.LENGTH_SHORT).show();
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
//                        mainViewHolder.modSingleLayoutBinding.btnDownload.setEnabled(false);
//                        mainViewHolder.modSingleLayoutBinding.btnDownload.setText("Downloading...");
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
//                        mainViewHolder.modSingleLayoutBinding.btnDownload.setEnabled(true);
//                        mainViewHolder.modSingleLayoutBinding.btnDownload.setText("DOWNLOAD");
//
//                        Toast.makeText(context,
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
//                        mainViewHolder.modSingleLayoutBinding.btnDownload.setEnabled(true);
//                        mainViewHolder.modSingleLayoutBinding.btnDownload.setText("DOWNLOAD");
//
//                        Toast.makeText(context,
//                                "Download failed: " + error, Toast.LENGTH_LONG).show();
//                    }
//
//                    @Override
//                    public void onCancelled() {
//                        Log.d("ContentActivity", "Download cancelled by user");
//
//                        // Re-enable download button
//                        mainViewHolder.modSingleLayoutBinding.btnDownload.setEnabled(true);
//                        mainViewHolder.modSingleLayoutBinding.btnDownload.setText("DOWNLOAD");
//
//                        Toast.makeText(context,
//                                "Download cancelled", Toast.LENGTH_SHORT).show();
//                    }
//                });
//    }
}
