package com.hyper.choosebrowsernew;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class PrivacyPolicyActivity extends AppCompatActivity {

    private ProgressBar privacyProgress;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.backgroundPrimary));
            getWindow().setNavigationBarColor(getResources().getColor(R.color.backgroundPrimary));
        }

        setContentView(R.layout.activity_privacy_policy);

        privacyProgress = findViewById(R.id.privacyProgress);

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

        applyProgressTheme(isDark);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (privacyProgress == null) return;
                privacyProgress.setProgress(newProgress);
                privacyProgress.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }
        });

        String url = "file:///android_asset/privacy_policy.html?theme=" + (isDark ? "dark" : "light");
        webView.loadUrl(url);

        // Back button
        findViewById(R.id.privacyBackBtn).setOnClickListener(v -> finish());
    }

    private void applyProgressTheme(boolean isDark) {
        if (privacyProgress == null) return;
        int progressColor = isDark ? Color.WHITE : Color.BLACK;
        ColorStateList tint = ColorStateList.valueOf(progressColor);
        privacyProgress.setProgressTintList(tint);
        privacyProgress.setSecondaryProgressTintList(tint);
        privacyProgress.setIndeterminateTintList(tint);
    }
}
