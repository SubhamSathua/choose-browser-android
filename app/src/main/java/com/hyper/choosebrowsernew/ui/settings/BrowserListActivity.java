package com.hyper.choosebrowsernew.ui.settings;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.hyper.choosebrowsernew.R;
import com.hyper.choosebrowsernew.data.model.AppInfo;

import java.util.ArrayList;
import java.util.List;

public class BrowserListActivity extends AppCompatActivity {

    ListView listView;
    List<AppInfo> browserList = new ArrayList<>();
    ImageView backBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ==== Status and Navbar color ====
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.backgroundPrimary));
            getWindow().setNavigationBarColor(getResources().getColor(R.color.backgroundPrimary));
        }
//        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser_list);

        listView = findViewById(R.id.browserListView);
        backBtn = findViewById(R.id.backBtn);

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        loadInstalledBrowsers();
    }

    private void loadInstalledBrowsers() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://"));
        browserIntent.addCategory(Intent.CATEGORY_BROWSABLE);

        PackageManager pm = getPackageManager();
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(browserIntent, PackageManager.MATCH_ALL);

        List<AppInfo> toolsList = new ArrayList<>();
        List<AppInfo> otherBrowsers = new ArrayList<>();

        for (ResolveInfo info : resolveInfos) {
            if (info.activityInfo == null || info.activityInfo.packageName == null) continue;

            String label = info.loadLabel(pm).toString();
            String pkgName = info.activityInfo.packageName;
            Drawable icon = info.loadIcon(pm);

            AppInfo app = new AppInfo(label, pkgName, icon);

            if (pkgName.startsWith("com.hyper.choosebrowser") || pkgName.startsWith("com.hyper.choosebrowsernew")) {
                toolsList.add(app);
            } else {
                otherBrowsers.add(app);
            }
        }

        // Merge both lists with tool-based first
        browserList.clear();
        browserList.addAll(toolsList);
        browserList.addAll(otherBrowsers);

        BrowserListAdapter adapter = new BrowserListAdapter(this, browserList);
        listView.setAdapter(adapter);
    }

}
