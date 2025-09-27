package com.el.cmr.nusaindah.ui.app_dashboard.utils;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

public class GPref {

    private Context context;
    private SharedPreferences sharedPreferences;
    public static final String DATA_PREF = "data_preferences";

    public GPref(Context context){
        super();
        this.context = context;
    }

    public Boolean getIndStatus(String prefType){
        sharedPreferences = context.getSharedPreferences(DATA_PREF, MODE_PRIVATE);
        Boolean prefBoolean = sharedPreferences.getBoolean(prefType, false);
        return prefBoolean;
    }

    public void setIndStatus(String prefType, Boolean prefVal) {
        sharedPreferences = context.getSharedPreferences(DATA_PREF, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(prefType, prefVal);
        editor.commit();
    }
}
