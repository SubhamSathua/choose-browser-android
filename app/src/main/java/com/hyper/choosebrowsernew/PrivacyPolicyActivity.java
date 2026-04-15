package com.hyper.choosebrowsernew;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class PrivacyPolicyActivity extends AppCompatActivity {

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.backgroundPrimary));
            getWindow().setNavigationBarColor(getResources().getColor(R.color.backgroundPrimary));
        }

        setContentView(R.layout.activity_privacy_policy);

        WebView webView = findViewById(R.id.privacyWebView);
        webView.setWebViewClient(new WebViewClient());

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true); // needed for theme detection script
        settings.setAllowFileAccess(true);
        settings.setDomStorageEnabled(false);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        // Pass theme as query parameter so the HTML can apply dark/light styling
        int mode = ThemeHelper.getSavedThemeMode(this);
        boolean isDark;
        if (mode == AppCompatDelegate.MODE_NIGHT_YES) {
            isDark = true;
        } else if (mode == AppCompatDelegate.MODE_NIGHT_NO) {
            isDark = false;
        } else {
            // System default: check current configuration
            int uiMode = getResources().getConfiguration().uiMode
                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            isDark = uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        }

        String url = "file:///android_asset/privacy_policy.html?theme=" + (isDark ? "dark" : "light");
        webView.loadUrl(url);

        // Back button
        findViewById(R.id.privacyBackBtn).setOnClickListener(v -> finish());
    }
}
