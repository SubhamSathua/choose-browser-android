package com.hyper.choosebrowsernew;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeHelper {

    private static final String PREFS_NAME = "theme_prefs";
    private static final String THEME_KEY = "selected_theme";
    private static final String COLOR_THEME_KEY = "selected_color_theme";

    public static final String COLOR_THEME_DEFAULT = "default";
    public static final String COLOR_THEME_DRACULA = "dracula";
    public static final String COLOR_THEME_COPILOT = "copilot";
    public static final String COLOR_THEME_THEME4 = "theme4";

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
        prefs.edit().putString(COLOR_THEME_KEY, sanitizeColorThemeId(themeId)).apply();
    }

    public static String getSavedColorThemeId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String themeId = prefs.getString(COLOR_THEME_KEY, COLOR_THEME_DEFAULT);
        return sanitizeColorThemeId(themeId);
    }

    public static int getPopupSurfaceColorRes(Context context) {
        String themeId = getSavedColorThemeId(context);
        if (COLOR_THEME_DRACULA.equals(themeId)) return R.color.dracula_popup_surface;
        if (COLOR_THEME_COPILOT.equals(themeId)) return R.color.copilot_popup_surface;
        return R.color.PopUpCardBg;
    }

    public static int getPopupDockColorRes(Context context) {
        String themeId = getSavedColorThemeId(context);
        if (COLOR_THEME_DRACULA.equals(themeId)) return R.color.dracula_popup_dock;
        if (COLOR_THEME_COPILOT.equals(themeId)) return R.color.copilot_popup_dock;
        return R.color.PopUpCardDockBg;
    }

    public static int getPopupDockButtonColorRes(Context context) {
        String themeId = getSavedColorThemeId(context);
        if (COLOR_THEME_DRACULA.equals(themeId)) return R.color.dracula_popup_dock_button;
        if (COLOR_THEME_COPILOT.equals(themeId)) return R.color.copilot_popup_dock_button;
        return R.color.PopUpCardDockBtn;
    }

    public static int getPopupDockButtonTextColorRes(Context context) {
        String themeId = getSavedColorThemeId(context);
        if (COLOR_THEME_DRACULA.equals(themeId)) return R.color.dracula_popup_dock_button_text;
        if (COLOR_THEME_COPILOT.equals(themeId)) return R.color.copilot_popup_dock_button_text;
        return R.color.PopUpCardDockText;
    }

    public static int getPopupActionColorRes(Context context) {
        String themeId = getSavedColorThemeId(context);
        if (COLOR_THEME_DRACULA.equals(themeId)) return R.color.dracula_popup_action;
        if (COLOR_THEME_COPILOT.equals(themeId)) return R.color.copilot_popup_action;
        return R.color.PopUpCard_ActionBtnBg;
    }

    public static int getPopupTextColorRes(Context context) {
        String themeId = getSavedColorThemeId(context);
        if (COLOR_THEME_DRACULA.equals(themeId)) return R.color.dracula_popup_text;
        if (COLOR_THEME_COPILOT.equals(themeId)) return R.color.copilot_popup_text;
        return R.color.text;
    }

    private static String sanitizeColorThemeId(String themeId) {
        if (COLOR_THEME_DRACULA.equals(themeId)
                || COLOR_THEME_COPILOT.equals(themeId)
                || COLOR_THEME_THEME4.equals(themeId)) {
            return themeId;
        }
        return COLOR_THEME_DEFAULT;
    }
}
