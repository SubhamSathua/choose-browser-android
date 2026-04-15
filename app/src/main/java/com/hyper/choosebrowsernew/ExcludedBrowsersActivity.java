package com.hyper.choosebrowsernew;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ExcludedBrowsersActivity extends AppCompatActivity {

    private final List<AppInfo> browserList = new ArrayList<>();
    private BrowserExclusionAdapter adapter;
    private Set<String> excludedPackages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.backgroundPrimary));
            getWindow().setNavigationBarColor(getResources().getColor(R.color.backgroundPrimary));
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_excluded_browsers);

        ListView listView = findViewById(R.id.listViewBrowsers);
        SwitchCompat showDefaultAppsSwitch = findViewById(R.id.switchShowDefaultApps);
        ImageView backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> finish());

        showDefaultAppsSwitch.setChecked(BrowserExclusionManager.shouldShowDefaultAppsForLink(this));
        showDefaultAppsSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
            BrowserExclusionManager.setShowDefaultAppsForLink(this, isChecked));

        excludedPackages = BrowserExclusionManager.getExcludedPackages(this);
        loadInstalledBrowsers();

        adapter = new BrowserExclusionAdapter(this, browserList, excludedPackages, this::toggleExclusion);
        listView.setAdapter(adapter);
    }

    private void toggleExclusion(AppInfo app) {
        if (app == null) return;
        boolean isExcluded = excludedPackages.contains(app.packageName);
        if (isExcluded) {
            excludedPackages.remove(app.packageName);
        } else {
            excludedPackages.add(app.packageName);
        }
        BrowserExclusionManager.setExcluded(this, app.packageName, !isExcluded);
        adapter.notifyDataSetChanged();
    }

    private void loadInstalledBrowsers() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://"));
        browserIntent.addCategory(Intent.CATEGORY_BROWSABLE);

        PackageManager pm = getPackageManager();
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(browserIntent, PackageManager.MATCH_ALL);
        String myPackage = getPackageName();

        browserList.clear();
        for (ResolveInfo info : resolveInfos) {
            if (info.activityInfo == null || info.activityInfo.packageName == null) continue;
            String pkgName = info.activityInfo.packageName;
            if (myPackage.equals(pkgName)) continue;

            String label = info.loadLabel(pm).toString();
            Drawable icon = info.loadIcon(pm);
            browserList.add(new AppInfo(label, pkgName, icon));
        }
    }
}