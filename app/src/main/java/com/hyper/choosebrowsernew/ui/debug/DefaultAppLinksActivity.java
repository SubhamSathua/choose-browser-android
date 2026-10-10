package com.hyper.choosebrowsernew.ui.debug;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hyper.choosebrowsernew.R;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DefaultAppLinksActivity extends AppCompatActivity {

    private RecyclerView rvAppLinks;
    private DefaultAppLinksAdapter adapter;
    private ProgressBar pbLoading;
    private TextView tvStatus;
    private TextView tvEmpty;
    private LinearLayout barSearch;
    private EditText etSearch;

    private ExecutorService executor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.backgroundPrimary));
            getWindow().setNavigationBarColor(getResources().getColor(R.color.backgroundPrimary));
        }

        setContentView(R.layout.activity_default_app_links);

        findViewById(R.id.btnAppLinksBack).setOnClickListener(v -> finish());

        rvAppLinks = findViewById(R.id.rvAppLinks);
        pbLoading = findViewById(R.id.pbAppLinksLoading);
        tvStatus = findViewById(R.id.tvAppLinksStatus);
        tvEmpty = findViewById(R.id.tvAppLinksEmpty);
        barSearch = findViewById(R.id.barAppLinksSearch);
        etSearch = findViewById(R.id.etAppLinksSearch);

        adapter = new DefaultAppLinksAdapter(this);
        rvAppLinks.setLayoutManager(new LinearLayoutManager(this));
        rvAppLinks.setAdapter(adapter);

        // Search Toggle
        ImageButton btnSearch = findViewById(R.id.btnAppLinksSearch);
        btnSearch.setOnClickListener(v -> {
            if (barSearch.getVisibility() == View.VISIBLE) {
                barSearch.setVisibility(View.GONE);
                etSearch.setText("");
                adapter.filter("");
            } else {
                barSearch.setVisibility(View.VISIBLE);
                etSearch.requestFocus();
            }
        });

        // Clear Search Button
        findViewById(R.id.btnClearAppLinksSearch).setOnClickListener(v -> {
            etSearch.setText("");
            adapter.filter("");
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s != null ? s.toString() : "");
                tvEmpty.setVisibility(adapter.getVisibleCount() == 0 ? View.VISIBLE : View.GONE);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        startOptimisticScan();
    }

    private void startOptimisticScan() {
        executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            PackageManager pm = getPackageManager();
            Set<String> packageNames = new LinkedHashSet<>();

            // 1. Gather all launcher apps
            try {
                Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                List<ResolveInfo> mainApps = pm.queryIntentActivities(mainIntent, 0);
                for (ResolveInfo ri : mainApps) {
                    if (ri != null && ri.activityInfo != null && ri.activityInfo.packageName != null) {
                        packageNames.add(ri.activityInfo.packageName);
                    }
                }
            } catch (Throwable ignored) {}

            // 2. Gather all web link handler apps
            try {
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"));
                webIntent.addCategory(Intent.CATEGORY_BROWSABLE);
                List<ResolveInfo> webApps = pm.queryIntentActivities(webIntent, 0);
                for (ResolveInfo ri : webApps) {
                    if (ri != null && ri.activityInfo != null && ri.activityInfo.packageName != null) {
                        packageNames.add(ri.activityInfo.packageName);
                    }
                }
            } catch (Throwable ignored) {}

            // 3. Fallback to installed packages list
            try {
                List<PackageInfo> installed = pm.getInstalledPackages(0);
                for (PackageInfo pi : installed) {
                    if (pi != null && pi.packageName != null) {
                        packageNames.add(pi.packageName);
                    }
                }
            } catch (Throwable ignored) {}

            int count = 0;
            for (String pkg : packageNames) {
                if (isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed())) {
                    break;
                }

                try {
                    PackageInfo pi = pm.getPackageInfo(pkg, 0);
                    AppLinksItem item = AppLinksHelper.parseApp(this, pm, pi);
                    if (item != null) {
                        count++;
                        final int currentCount = count;
                        runOnUiThread(() -> {
                            if (!isFinishing()) {
                                adapter.addApp(item);
                                tvStatus.setText("Scanning apps… Found " + currentCount + " with supported links");
                            }
                        });
                    }
                } catch (Throwable ignored) {}
            }

            final int finalTotal = count;
            runOnUiThread(() -> {
                if (!isFinishing()) {
                    pbLoading.setVisibility(View.GONE);
                    tvStatus.setText("Scan complete • " + finalTotal + " apps with supported links found");
                    tvEmpty.setVisibility(finalTotal == 0 ? View.VISIBLE : View.GONE);
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}
