package com.hyper.choosebrowsernew;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class BrowserExclusionManager {

    private static final String PREFS_NAME = "browser_exclusion_prefs";
    private static final String KEY_EXCLUDED_PACKAGES = "excluded_packages";
    private static final String KEY_SHOW_DEFAULT_APPS = "show_default_apps_for_link";

    private BrowserExclusionManager() {
        // Utility class
    }

    public static Set<String> getExcludedPackages(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> saved = prefs.getStringSet(KEY_EXCLUDED_PACKAGES, Collections.emptySet());
        return new HashSet<>(saved);
    }

    public static boolean isExcluded(Context context, String packageName) {
        if (packageName == null) return false;
        return getExcludedPackages(context).contains(packageName);
    }

    public static void setExcluded(Context context, String packageName, boolean excluded) {
        if (packageName == null) return;
        Set<String> set = getExcludedPackages(context);
        if (excluded) {
            set.add(packageName);
        } else {
            set.remove(packageName);
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putStringSet(KEY_EXCLUDED_PACKAGES, set)
                .apply();
    }

    public static boolean shouldShowDefaultAppsForLink(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_SHOW_DEFAULT_APPS, true);
    }

    public static void setShowDefaultAppsForLink(Context context, boolean show) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_SHOW_DEFAULT_APPS, show)
                .apply();
    }
}