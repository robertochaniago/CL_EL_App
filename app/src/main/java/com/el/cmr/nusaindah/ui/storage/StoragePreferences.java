package com.el.cmr.nusaindah.ui.storage;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Storage preferences for SAF
 */
public class StoragePreferences {

    private static final String PREF_NAME = "storage_preferences";
    private static final String KEY_FOLDER_URI = "selected_folder_uri";
    private static final String KEY_FIRST_TIME = "first_time_storage";

    private SharedPreferences preferences;

    public StoragePreferences(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setSelectedFolderUri(String uri) {
        preferences.edit().putString(KEY_FOLDER_URI, uri).apply();
    }

    public String getSelectedFolderUri() {
        return preferences.getString(KEY_FOLDER_URI, null);
    }

    public void clearSelectedFolderUri() {
        preferences.edit().remove(KEY_FOLDER_URI).apply();
    }

    public boolean isFirstTimeStorage() {
        return preferences.getBoolean(KEY_FIRST_TIME, true);
    }

    public void setFirstTimeStorage(boolean firstTime) {
        preferences.edit().putBoolean(KEY_FIRST_TIME, firstTime).apply();
    }
}