package com.el.cmr.nusaindah.ui.app_notifications.main_page;

import static com.el.cmr.nusaindah.ui.app_dashboard.utils.GLibs.BANNER_ADS;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.el.cmr.nusaindah.databinding.ExitDialogBinding;

public class AppCloseDialog {

    public void dialogShow(Activity context){
        DialogPropsID dialogPropsID = new DialogPropsID(context);
        dialogPropsID.setCancelable(false);
        dialogPropsID.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialogPropsID.show();

        Window window = dialogPropsID.getWindow();
        assert window != null;
        window.setLayout(ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT);
    }

    public class DialogPropsID extends Dialog{

        private Activity context;
        private ExitDialogBinding binding;
        private View view;
        private AdView adView;

        public DialogPropsID(Activity context) {
            super(context);
            this.context = context;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            requestWindowFeature(Window.FEATURE_NO_TITLE);

            binding = ExitDialogBinding.inflate(LayoutInflater.from(context));
            view = binding.getRoot();
            setContentView(view);

            loadAdExit();

            binding.btnYes.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try{
                        if (adView != null) {
                            adView.destroy();
                        }
                    }catch (Exception e){}
                    dismiss();
                    context.finish();
                }
            });

            binding.btnNo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try{
                        if (adView != null) {
                            adView.destroy();
                        }
                    }catch (Exception e){}
                    dismiss();
                }
            });
        }

        private void loadAdExit(){
            try{
                Activity activity = (Activity) context;
                adView = new AdView(activity);

                adView.setAdUnitId(BANNER_ADS);
                adView.setAdSize(AdSize.MEDIUM_RECTANGLE);

                binding.adBannerExit.removeAllViews();
                binding.adBannerExit.addView(adView);
                adView.loadAd(new AdRequest.Builder().build());
            }catch (Exception e){}
        }
    }
}
