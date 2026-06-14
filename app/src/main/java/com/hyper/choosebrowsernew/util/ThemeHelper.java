package com.hyper.choosebrowsernew.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;

import androidx.appcompat.app.AppCompatDelegate;

import com.hyper.choosebrowsernew.R;
import androidx.core.content.ContextCompat;

public class ThemeHelper {

    private static final String PREFS_NAME = "theme_prefs";
    private static final String THEME_KEY = "selected_theme";
    private static final String COLOR_THEME_KEY = "selected_color_theme";

    public static final String COLOR_THEME_DEFAULT = "default";
    public static final String COLOR_THEME_ECLIPSE = "eclipse";
    public static final String COLOR_THEME_NEURONIGHT = "neuronight";
    public static final String COLOR_THEME_NEURAL_BLUE = "neural_blue";
    public static final String COLOR_THEME_COSMIC = "cosmic";
    public static final String COLOR_THEME_OBSIDIAN_PULSE = "obsidian_pulse";

    public static void applySavedTheme(Context context) {
        int mode = getSavedThemeMode(context);
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public static void saveTheme(Context context, int mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(THEME_KEY, mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public static int getSavedThemeMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(THEME_KEY, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public static void saveColorThemeId(Context context, String themeId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(COLOR_THEME_KEY, sanitizeColorThemeId(context, themeId)).apply();
    }

    public static String getSavedColorThemeId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String themeId = prefs.getString(COLOR_THEME_KEY, COLOR_THEME_DEFAULT);
        String normalized = sanitizeColorThemeId(context, themeId);
        if (!normalized.equals(themeId)) {
            prefs.edit().putString(COLOR_THEME_KEY, normalized).apply();
        }
        return normalized;
    }

    public static int getColorThemeOverlayStyleRes(Context context) {
        String themeId = getSavedColorThemeId(context);
        switch (themeId) {
            case COLOR_THEME_ECLIPSE:
                return R.style.ThemeOverlay_ChooseBrowserNEW_Color_eclipse;
            case COLOR_THEME_NEURONIGHT:
                return R.style.ThemeOverlay_ChooseBrowserNEW_Color_neuronight;
            case COLOR_THEME_NEURAL_BLUE:
                return R.style.ThemeOverlay_ChooseBrowserNEW_Color_neural_blue;
            case COLOR_THEME_COSMIC:
                return R.style.ThemeOverlay_ChooseBrowserNEW_Color_cosmic;
            case COLOR_THEME_OBSIDIAN_PULSE:
                return R.style.ThemeOverlay_ChooseBrowserNEW_Color_obsidian_pulse;
            case COLOR_THEME_DEFAULT:
            default:
                return R.style.ThemeOverlay_ChooseBrowserNEW_Color_default;
        }
    }

    public static Context wrapWithColorThemeOverlay(Context context) {
        return new ContextThemeWrapper(context, getColorThemeOverlayStyleRes(context));
    }

    public static int resolveThemeColor(Context themedContext, int attrRes, int fallbackColorRes) {
        TypedValue typedValue = new TypedValue();
        if (themedContext.getTheme().resolveAttribute(attrRes, typedValue, true)) {
            if (typedValue.resourceId != 0) {
                return ContextCompat.getColor(themedContext, typedValue.resourceId);
            }
            return typedValue.data;
        }
        return ContextCompat.getColor(themedContext, fallbackColorRes);
    }

    private static String sanitizeColorThemeId(Context context, String themeId) {
        if (themeId == null || themeId.trim().isEmpty()) {
            return COLOR_THEME_DEFAULT;
        }

        // One-time legacy migrations.
        if ("dracula".equals(themeId)) themeId = COLOR_THEME_NEURONIGHT;
        if ("copilot".equals(themeId)) themeId = COLOR_THEME_ECLIPSE;
        if ("theme4".equals(themeId)) themeId = COLOR_THEME_OBSIDIAN_PULSE;

        switch (themeId) {
            case COLOR_THEME_DEFAULT:
            case COLOR_THEME_ECLIPSE:
            case COLOR_THEME_NEURONIGHT:
            case COLOR_THEME_NEURAL_BLUE:
            case COLOR_THEME_COSMIC:
            case COLOR_THEME_OBSIDIAN_PULSE:
                return themeId;
            default:
                return COLOR_THEME_DEFAULT;
        }
    }
}
