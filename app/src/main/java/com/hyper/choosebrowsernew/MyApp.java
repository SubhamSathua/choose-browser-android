package com.hyper.choosebrowsernew;

import android.app.Application;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ThemeHelper.applySavedTheme(this); // Apply saved theme globally
    }
}
