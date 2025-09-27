package com.el.cmr.nusaindah.ui.app_notifications.main_page;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.el.cmr.nusaindah.databinding.ActivityHomeBinding;
import com.el.cmr.nusaindah.ui.app_dashboard.utils.ItemDecoration;
import com.el.cmr.nusaindah.ui.app_home.progress.ads.GoogleAdsLibs;
import com.el.cmr.nusaindah.ui.my_home.SQLiteHelper;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private View view;
    private Activity activity = HomeActivity.this;
    private ActivityHomeBinding binding;
    private AppCloseDialog appCloseDialog;
    private SQLiteHelper sqLiteHelper;
    private List<Object> dataListHome = new ArrayList<>();
    private RecyclerView.Adapter<RecyclerView.ViewHolder> homeAdapter;

    private GoogleAdsLibs googleAdsLibs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        view = binding.getRoot();
        setContentView(view);

        initHomeActivity();
        appCloseDialog = new AppCloseDialog();
        getOnBackPressedDispatcher().addCallback( this, onBackPressedCallback );
    }

    OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback( true ) {
        @Override
        public void handleOnBackPressed() {
            try {
                appCloseDialog.dialogShow(activity);
            }catch (Exception e){
                finish();
            }
        }
    };

    private void initHomeActivity(){
        sqLiteHelper = new SQLiteHelper(getApplicationContext());
        dataListHome.addAll(sqLiteHelper.getDataHomeActivity(1));
        dataListHome.addAll(sqLiteHelper.getDataHomeActivity(0));

        googleAdsLibs = new GoogleAdsLibs(getApplicationContext());
        googleAdsLibs.interstitialCreated(false);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(activity);
        binding.recyclerviewHome.setLayoutManager(linearLayoutManager);
        binding.recyclerviewHome.setHasFixedSize(true);
        binding.recyclerviewHome.addItemDecoration(new ItemDecoration(20));

        loadData();
    }

    private void loadData(){
        homeAdapter = new HomeAdapter(activity, dataListHome);
        binding.recyclerviewHome.setAdapter(homeAdapter);
        homeAdapter.notifyDataSetChanged();
        binding.recyclerviewHome.setVisibility(View.VISIBLE);
    }
}