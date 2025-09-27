package com.el.cmr.nusaindah.ui.app_dashboard.utils;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

public class ItemDecoration extends RecyclerView.ItemDecoration {
    private int space;

    public ItemDecoration(int space) {
        this.space = space;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view,RecyclerView parent, RecyclerView.State state) {
        outRect.bottom = space;
    }
}
