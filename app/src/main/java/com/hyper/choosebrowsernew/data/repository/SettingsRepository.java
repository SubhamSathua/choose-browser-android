package com.hyper.choosebrowsernew.data.repository;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.appcompat.app.AppCompatDelegate;

import com.hyper.choosebrowsernew.util.ThemeHelper;

public class SettingsRepository {

    private final Context context;

    public SettingsRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public int getThemeMode() {
        return ThemeHelper.getSavedThemeMode(context);
    }

    public void setThemeMode(int mode) {
        ThemeHelper.saveTheme(context, mode);
    }

    public String getColorThemeId() {
        return ThemeHelper.getSavedColorThemeId(context);
    }

    public void setColorThemeId(String themeId) {
        ThemeHelper.saveColorThemeId(context, themeId);
    }

    public String getAppVersion() {
        try {
            return context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0)
                    .versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "N/A";
        }
    }
}
