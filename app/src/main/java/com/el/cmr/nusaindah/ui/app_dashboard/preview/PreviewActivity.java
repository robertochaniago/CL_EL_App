package com.el.cmr.nusaindah.ui.app_dashboard.preview;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.el.cmr.nusaindah.R;
import com.el.cmr.nusaindah.databinding.ActivityHomeBinding;
import com.el.cmr.nusaindah.databinding.ActivityPreviewBinding;
import com.el.cmr.nusaindah.ui.app_dashboard.utils.ItemDecoration;
import com.el.cmr.nusaindah.ui.app_home.progress.ads.GoogleAdsLibs;
import com.el.cmr.nusaindah.ui.app_notifications.main_page.HomeActivity;
import com.el.cmr.nusaindah.ui.app_notifications.main_page.HomeAdapter;
import com.el.cmr.nusaindah.ui.my_home.SQLiteHelper;

import java.util.ArrayList;
import java.util.List;

public class PreviewActivity extends AppCompatActivity {

    private View view;
    private Activity activity = PreviewActivity.this;
    private ActivityPreviewBinding binding;

    private SQLiteHelper sqLiteHelper;
    private List<Object> dataListImage = new ArrayList<>();
    private RecyclerView.Adapter<RecyclerView.ViewHolder> imgAdapter;

    private GoogleAdsLibs googleAdsLibs;
    private Intent intent;
    private Bundle extras;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPreviewBinding.inflate(getLayoutInflater());
        view = binding.getRoot();
        setContentView(view);

        intent = getIntent();
        extras = intent.getExtras();

        googleAdsLibs = new GoogleAdsLibs(getApplicationContext());
        sqLiteHelper = new SQLiteHelper(getApplicationContext());
        dataListImage.addAll(sqLiteHelper.getDataImageActivity(extras.getInt("mod_id")));

        googleAdsLibs.imgFeedAd(dataListImage,1, 100, activity);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(activity);
        binding.recyclerviewImage.setLayoutManager(linearLayoutManager);
        binding.recyclerviewImage.setHasFixedSize(true);
        binding.recyclerviewImage.addItemDecoration(new ItemDecoration(20));

        imgAdapter = new PreviewAdapter(activity, dataListImage);
        binding.recyclerviewImage.setAdapter(imgAdapter);
        imgAdapter.notifyDataSetChanged();
        binding.recyclerviewImage.setVisibility(View.VISIBLE);
    }
}