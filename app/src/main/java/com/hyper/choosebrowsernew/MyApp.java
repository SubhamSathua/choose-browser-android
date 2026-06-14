package com.hyper.choosebrowsernew;

import android.app.Application;

import com.hyper.choosebrowsernew.util.ThemeHelper;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ThemeHelper.applySavedTheme(this); // Apply saved theme globally
    }
}
