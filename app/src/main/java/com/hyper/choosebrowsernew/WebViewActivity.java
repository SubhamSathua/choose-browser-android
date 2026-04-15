package com.hyper.choosebrowsernew;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class WebViewActivity extends AppCompatActivity {

    public static final String EXTRA_URL   = "extra_url";
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_SHOW_OPEN_WITH = "extra_show_open_with";

    private WebView webView;
    private String currentUrl;

    // ── Static helpers ───────────────────────────────────────────────

    public static void openPrivacyPolicy(AppCompatActivity ctx) {
        String theme = isDark(ctx) ? "dark" : "light";
        String url = "file:///android_asset/privacy_policy.html?theme=" + theme + "&app=1";
        start(ctx, url, "Privacy Policy");
    }

    public static void openFeedback(AppCompatActivity ctx) {
        start(ctx, "https://tally.so/r/VLlZpM", "Feedback");
    }

    public static void openGitHub(AppCompatActivity ctx, String url) {
        Intent i = new Intent(ctx, WebViewActivity.class);
        i.putExtra(EXTRA_URL, url);
        i.putExtra(EXTRA_TITLE, "GitHub");
        i.putExtra(EXTRA_SHOW_OPEN_WITH, true);
        ctx.startActivity(i);
    }

    public static void start(Context ctx, String url, String title) {
        Intent i = new Intent(ctx, WebViewActivity.class);
        i.putExtra(EXTRA_URL, url);
        i.putExtra(EXTRA_TITLE, title);
        ctx.startActivity(i);
    }

    // ── Activity ─────────────────────────────────────────────────────

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.backgroundPrimary));
            getWindow().setNavigationBarColor(getResources().getColor(R.color.backgroundPrimary));
        }

        setContentView(R.layout.activity_webview);

        currentUrl = getIntent().getStringExtra(EXTRA_URL);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        boolean showOpenWith = getIntent().getBooleanExtra(EXTRA_SHOW_OPEN_WITH, false);
        if (currentUrl == null) currentUrl = "";
        if (title == null) title = "";

        android.widget.TextView tvTitle = findViewById(R.id.webviewTitle);
        tvTitle.setText(title);

        // Open-with button
        ImageButton openWithBtn = findViewById(R.id.webviewOpenWith);
        if (showOpenWith && currentUrl.startsWith("http")) {
            openWithBtn.setVisibility(View.VISIBLE);
            openWithBtn.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl));
                startActivity(intent);
            });
        }

        webView = findViewById(R.id.webViewContent);

        // JS interface for retry from error page
        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void retry() {
                runOnUiThread(() -> {
                    if (currentUrl != null && !currentUrl.isEmpty()) {
                        webView.loadUrl(currentUrl);
                    }
                });
            }
        }, "Android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                // Only handle main frame errors
                if (request.isForMainFrame()) {
                    String theme = isDark(WebViewActivity.this) ? "dark" : "light";
                    String desc = "";
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && error.getDescription() != null) {
                        desc = error.getDescription().toString();
                    }
                    view.loadUrl("file:///android_asset/error.html?theme=" + theme
                            + "&code=" + (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? error.getErrorCode() : "")
                            + "&desc=" + Uri.encode(desc));
                }
            }
        });

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setDomStorageEnabled(false);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        webView.loadUrl(currentUrl);

        findViewById(R.id.webviewBackBtn).setOnClickListener(v -> finish());
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private static boolean isDark(Context ctx) {
        int mode = ThemeHelper.getSavedThemeMode(ctx);
        if (mode == AppCompatDelegate.MODE_NIGHT_YES) return true;
        if (mode == AppCompatDelegate.MODE_NIGHT_NO) return false;
        int uiMode = ctx.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return uiMode == Configuration.UI_MODE_NIGHT_YES;
    }
}
