package com.hyper.choosebrowsernew.ui.debug;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.verify.domain.DomainVerificationManager;
import android.content.pm.verify.domain.DomainVerificationUserState;
import android.content.res.AssetManager;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Build;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AppLinksHelper {

    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    public static AppLinksItem parseApp(Context context, PackageManager pm, PackageInfo pkgInfo) {
        if (pkgInfo == null || pkgInfo.packageName == null) return null;
        String packageName = pkgInfo.packageName;

        // Skip our own app
        if (packageName.equals(context.getPackageName())) return null;

        Set<String> linksSet = new LinkedHashSet<>();
        boolean isAutoVerify = false;

        // 1. Android 12+ DomainVerificationManager (Official API)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                DomainVerificationManager dvm = context.getSystemService(DomainVerificationManager.class);
                if (dvm != null) {
                    DomainVerificationUserState userState = dvm.getDomainVerificationUserState(packageName);
                    if (userState != null && userState.getHostToStateMap() != null) {
                        for (String host : userState.getHostToStateMap().keySet()) {
                            linksSet.add("https://" + host);
                            isAutoVerify = true;
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        // 2. Parse AndroidManifest.xml from package context assets
        try {
            Context pkgContext = context.createPackageContext(packageName, 0);
            AssetManager assets = pkgContext.getAssets();
            XmlResourceParser parser = assets.openXmlResourceParser("AndroidManifest.xml");

            int eventType = parser.getEventType();
            boolean inActivity = false;
            boolean inIntentFilter = false;
            boolean isViewAction = false;
            boolean autoVerifyAttr = false;

            List<String> schemes = new ArrayList<>();
            List<String> hosts = new ArrayList<>();
            List<String> pathPrefixes = new ArrayList<>();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String tagName = parser.getName();
                    if ("activity".equals(tagName) || "activity-alias".equals(tagName)) {
                        inActivity = true;
                    } else if (inActivity && "intent-filter".equals(tagName)) {
                        inIntentFilter = true;
                        isViewAction = false;
                        schemes.clear();
                        hosts.clear();
                        pathPrefixes.clear();

                        String autoVerify = getAttributeValue(parser, "autoVerify");
                        if ("true".equalsIgnoreCase(autoVerify)) {
                            autoVerifyAttr = true;
                        }
                    } else if (inIntentFilter && "action".equals(tagName)) {
                        String name = getAttributeValue(parser, "name");
                        if (Intent.ACTION_VIEW.equals(name)) {
                            isViewAction = true;
                        }
                    } else if (inIntentFilter && "data".equals(tagName)) {
                        String scheme = getAttributeValue(parser, "scheme");
                        String host = getAttributeValue(parser, "host");
                        String pathPrefix = getAttributeValue(parser, "pathPrefix");

                        if (scheme != null && !scheme.isEmpty()) schemes.add(scheme);
                        if (host != null && !host.isEmpty()) hosts.add(host);
                        if (pathPrefix != null && !pathPrefix.isEmpty()) pathPrefixes.add(pathPrefix);
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    String tagName = parser.getName();
                    if ("intent-filter".equals(tagName)) {
                        if (isViewAction && !schemes.isEmpty()) {
                            if (autoVerifyAttr) {
                                isAutoVerify = true;
                            }
                            for (String scheme : schemes) {
                                if (hosts.isEmpty()) {
                                    // Custom non-http scheme like flipkart:// or spotify://
                                    if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                                        linksSet.add(scheme + "://*");
                                    }
                                } else {
                                    for (String host : hosts) {
                                        if (pathPrefixes.isEmpty()) {
                                            linksSet.add(scheme + "://" + host);
                                        } else {
                                            for (String prefix : pathPrefixes) {
                                                linksSet.add(scheme + "://" + host + prefix);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        inIntentFilter = false;
                    } else if ("activity".equals(tagName) || "activity-alias".equals(tagName)) {
                        inActivity = false;
                    }
                }
                eventType = parser.next();
            }
            parser.close();
        } catch (Throwable ignored) {}

        // If no supported links found, return null so we don't clutter the UI
        if (linksSet.isEmpty()) {
            return null;
        }

        // App Label & Icon
        CharSequence label = pkgInfo.applicationInfo != null ? pkgInfo.applicationInfo.loadLabel(pm) : packageName;
        Drawable icon = pkgInfo.applicationInfo != null ? pkgInfo.applicationInfo.loadIcon(pm) : null;

        List<String> sortedLinks = new ArrayList<>(linksSet);
        Collections.sort(sortedLinks);

        return new AppLinksItem(
                label != null ? label.toString() : packageName,
                packageName,
                icon,
                sortedLinks,
                isAutoVerify
        );
    }

    private static String getAttributeValue(XmlResourceParser parser, String attrName) {
        String val = parser.getAttributeValue(ANDROID_NS, attrName);
        if (val == null) {
            val = parser.getAttributeValue(null, attrName);
        }
        return val;
    }
}
