package com.hyper.choosebrowsernew.data.repository;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;

import com.hyper.choosebrowsernew.data.local.BrowserExclusionManager;
import com.hyper.choosebrowsernew.data.model.AppInfo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrowserRepository {

    private final Context context;
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?i)\\b((?:https?://|www\\.)[^\\s<>()\\[\\]{}]+|"
                    + "(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,}(?::\\d{2,5})?(?:[/?#][^\\s<>()\\[\\]{}]*)?|"
                    + "(?:\\d{1,3}\\.){3}\\d{1,3}(?::\\d{2,5})?(?:[/?#][^\\s<>()\\[\\]{}]*)?|"
                    + "localhost(?::\\d{2,5})?(?:[/?#][^\\s<>()\\[\\]{}]*)?)",
            Pattern.CASE_INSENSITIVE
    );

    public BrowserRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public List<AppInfo> getInstalledBrowsers() {
        PackageManager pm = context.getPackageManager();
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://"));
        browserIntent.addCategory(Intent.CATEGORY_BROWSABLE);

        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(browserIntent, PackageManager.MATCH_ALL);
        Set<String> excludedPackages = BrowserExclusionManager.getExcludedPackages(context);

        List<AppInfo> browsers = new ArrayList<>();
        String myPackage = context.getPackageName();

        for (ResolveInfo info : resolveInfos) {
            if (info == null || info.activityInfo == null || info.activityInfo.packageName == null) continue;
            String pkgName = info.activityInfo.packageName;
            if (pkgName.equals(myPackage)) continue;
            if (excludedPackages.contains(pkgName)) continue;
            
            String label = info.loadLabel(pm).toString();
            Drawable icon = info.loadIcon(pm);
            browsers.add(new AppInfo(label, pkgName, icon));
        }
        return browsers;
    }

    public List<String> extractUrls(String text) {
        List<String> result = new ArrayList<>();
        if (TextUtils.isEmpty(text)) return result;

        LinkedHashSet<String> unique = new LinkedHashSet<>();
        Matcher matcher = URL_PATTERN.matcher(text);
        while (matcher.find()) {
            String found = matcher.group(1);
            String cleaned = cleanMatchedUrl(found);
            if (!TextUtils.isEmpty(cleaned) && isProbablyAUrl(cleaned)) {
                unique.add(cleaned);
            }
        }
        result.addAll(unique);
        return result;
    }

    private String cleanMatchedUrl(String found) {
        if (TextUtils.isEmpty(found)) return "";
        String cleaned = found.trim();
        cleaned = cleaned.replaceAll("^[\\(\\[\\{<\"']+", "");
        cleaned = cleaned.replaceAll("[\\)\\]\\}>,!;:\"']+$", "");
        cleaned = cleaned.replaceAll("[\\.\\?\\!]+$", "");
        return cleaned;
    }

    private boolean isProbablyAUrl(String candidate) {
        if (TextUtils.isEmpty(candidate)) return false;
        String lower = candidate.toLowerCase(Locale.US);
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("www.")) return true;

        try {
            String test = candidate;
            if (!test.contains("://")) test = "https://" + test;
            Uri uri = Uri.parse(test);
            String host = uri.getHost();
            if (TextUtils.isEmpty(host)) return false;

            host = host.toLowerCase(Locale.US);
            if ("localhost".equals(host)) return true;
            if (host.matches("^\\d{1,3}(\\.\\d{1,3}){3}$")) return true;

            int lastDot = host.lastIndexOf('.');
            if (lastDot < 0 || lastDot == host.length() - 1) return false;
            String tld = host.substring(lastDot + 1);
            return tld.length() >= 2 && tld.matches("[a-z0-9]+");
        } catch (Exception e) {
            return false;
        }
    }
}
