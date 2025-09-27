package com.el.cmr.nusaindah.ui.app_dashboard.preview;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.el.cmr.nusaindah.R;
import com.el.cmr.nusaindah.databinding.ModAdBinding;
import com.el.cmr.nusaindah.databinding.ModPreviewBinding;
import com.el.cmr.nusaindah.ui.app_notifications.main_page.HomeAdapter;
import com.el.cmr.nusaindah.ui.my_home.page.model.HomeModel;
import com.el.cmr.nusaindah.ui.my_home.page.model.ImageModel;
import com.google.android.gms.ads.AdView;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class PreviewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    static List<Object> recyclerviewList;
    static Context context;

    static final int IMG_ROW = 1;
    static final int ADS_ROW = 2;

    public PreviewAdapter(Context context, List<Object> recyclerviewList){
        this.context = context;
        this.recyclerviewList = recyclerviewList;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == ADS_ROW){
            return new AdsViewHolder(ModAdBinding.inflate(LayoutInflater.from(parent.getContext()),parent, false));
        }else{
            return new ModViewHolder(ModPreviewBinding.inflate(LayoutInflater.from(parent.getContext()),parent, false));
        }
    }

    @Override
    public int getItemViewType(int position) {
        Object recyclerViewItem = recyclerviewList.get(position);
        if(recyclerViewItem instanceof AdView){
            return ADS_ROW;
        }else{
            return IMG_ROW;
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
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        switch (holder.getItemViewType()) {
            case ADS_ROW:
                try{
                    AdsViewHolder adsViewHolder = (AdsViewHolder) holder;
                    AdView adView = (AdView) recyclerviewList.get(position);
                    if (adsViewHolder.modAdBinding.adFeeds.getChildCount() > 0) {
                        adsViewHolder.modAdBinding.adFeeds.removeAllViews();
                    }
                    if (adView.getParent() != null) {
                        ((ViewGroup) adView.getParent()).removeView(adView);
                    }
                    adsViewHolder.modAdBinding.adFeeds.addView(adView);
                }catch (Exception e){}
                break;
            case IMG_ROW:
                ModViewHolder modViewHolder = (ModViewHolder) holder;
                ImageModel main = (ImageModel) recyclerviewList.get(position);
                Picasso.get()
                        .load(main.getImage_url())
                        .placeholder(R.drawable.img_mod_placeholder)
                        .error(R.drawable.img_mod_placeholder)
                        .fit()
                        .centerCrop()
                        .into(modViewHolder.modPreviewBinding.imageMod);
                break;
        }
    }

    public class AdsViewHolder extends RecyclerView.ViewHolder {
        private ModAdBinding modAdBinding;
        AdsViewHolder(ModAdBinding binding) {
            super(binding.getRoot());
            modAdBinding = binding;
        }
    }

    public class ModViewHolder extends RecyclerView.ViewHolder {
        private ModPreviewBinding modPreviewBinding;
        ModViewHolder(ModPreviewBinding binding) {
            super(binding.getRoot());
            modPreviewBinding = binding;
        }
    }
}
