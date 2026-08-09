package com.adamrussin.urlnfctap;

import android.content.Context;
import android.content.SharedPreferences;

public final class ShareState {
    private static final String PREFERENCES_NAME = "share_state";
    private static final String SELECTED_URL_KEY = "selected_url";
    private static volatile boolean active;

    private ShareState() {
    }

    public static void select(Context context, String url) {
        preferences(context).edit().putString(SELECTED_URL_KEY, url).commit();
    }

    public static String selectedUrl(Context context) {
        return preferences(context).getString(SELECTED_URL_KEY, BuildConfig.PRIMARY_URL);
    }

    public static void setActive(boolean isActive) {
        active = isActive;
    }

    public static boolean isActive() {
        return active;
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }
}
