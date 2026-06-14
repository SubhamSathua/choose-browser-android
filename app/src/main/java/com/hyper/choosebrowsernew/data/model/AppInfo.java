package com.hyper.choosebrowsernew.data.model;

import android.graphics.drawable.Drawable;

public class AppInfo {
    public String name;
    public String packageName;
    public Drawable icon;

    public AppInfo(String name, String packageName, Drawable icon) {
        this.name = name;
        this.packageName = packageName;
        this.icon = icon;
    }
}
