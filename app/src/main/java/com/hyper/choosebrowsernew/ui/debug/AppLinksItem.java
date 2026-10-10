package com.hyper.choosebrowsernew.ui.debug;

import android.graphics.drawable.Drawable;

import java.util.List;

public class AppLinksItem {
    private final String appName;
    private final String packageName;
    private final Drawable icon;
    private final List<String> supportedLinks;
    private final boolean isAutoVerify;
    private boolean isExpanded;

    public AppLinksItem(String appName, String packageName, Drawable icon, List<String> supportedLinks, boolean isAutoVerify) {
        this.appName = appName;
        this.packageName = packageName;
        this.icon = icon;
        this.supportedLinks = supportedLinks;
        this.isAutoVerify = isAutoVerify;
        this.isExpanded = false;
    }

    public String getAppName() {
        return appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public List<String> getSupportedLinks() {
        return supportedLinks;
    }

    public boolean isAutoVerify() {
        return isAutoVerify;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

    public String getFormattedLinks(boolean all) {
        int max = (all || supportedLinks.size() <= 4) ? supportedLinks.size() : 4;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < max; i++) {
            sb.append("- ").append(supportedLinks.get(i));
            if (i < max - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
