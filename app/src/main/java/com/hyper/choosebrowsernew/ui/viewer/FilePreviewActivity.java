package com.hyper.choosebrowsernew.ui.viewer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.hyper.choosebrowsernew.R;
import com.hyper.choosebrowsernew.util.ThemeHelper;

import java.util.Locale;

/**
 * Modular zero-permission activity for previewing HTML, Markdown, and Text files.
 */
public class FilePreviewActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private LinearLayout errorContainer;
    private TextView errorText;
    private TextView titleView;
    private TextView badgeView;
    private ImageButton wrapBtn;
    private ImageButton openWithBtn;
    private ImageButton shareBtn;
    private TextView zoomBadge;

    private FileContentLoader contentLoader;
    private PinchZoomController zoomController;

    private Uri targetUri;
    private String currentFileName;
    private FileType currentFileType = FileType.TEXT;
    private String currentContent;
    private boolean isWrapped = true;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable hideZoomBadgeRunnable = () -> {
        if (zoomBadge != null) {
            zoomBadge.animate()
                    .alpha(0f)
                    .setDuration(250)
                    .withEndAction(() -> zoomBadge.setVisibility(View.GONE))
                    .start();
        }
    };

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ThemeHelper.wrapWithColorThemeOverlay(newBase));
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(ThemeHelper.getColorThemeOverlayStyleRes(this));
        super.onCreate(savedInstanceState);

        int surface = ThemeHelper.resolveThemeColor(this, R.attr.colorPopupSurface, R.color.backgroundSecondary);
        getWindow().setStatusBarColor(surface);
        getWindow().setNavigationBarColor(surface);
        boolean isDark = ThemeHelper.isDarkTheme(this);
        androidx.core.view.WindowInsetsControllerCompat controller =
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(!isDark);
            controller.setAppearanceLightNavigationBars(!isDark);
        }

        setContentView(R.layout.activity_file_preview);

        initViews();
        setupWebView();
        setupActions();

        contentLoader = new FileContentLoader(this);
        resolveAndLoadFile();
    }

    private void initViews() {
        titleView = findViewById(R.id.filePreviewTitle);
        badgeView = findViewById(R.id.filePreviewBadge);
        wrapBtn = findViewById(R.id.filePreviewWrapBtn);
        openWithBtn = findViewById(R.id.filePreviewOpenWithBtn);
        shareBtn = findViewById(R.id.filePreviewShareBtn);
        webView = findViewById(R.id.filePreviewWebView);
        progressBar = findViewById(R.id.filePreviewProgress);
        errorContainer = findViewById(R.id.filePreviewErrorContainer);
        errorText = findViewById(R.id.filePreviewErrorText);
        zoomBadge = findViewById(R.id.filePreviewZoomBadge);

        findViewById(R.id.filePreviewBack).setOnClickListener(v -> finish());
    }

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    private void setupWebView() {
        int surface = ThemeHelper.resolveThemeColor(this, R.attr.colorPopupSurface, R.color.backgroundSecondary);
        webView.setBackgroundColor(surface);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        zoomController = new PinchZoomController(this, webView);
        zoomController.setOnZoomChangeListener(this::onZoomChanged);

        webView.setOnTouchListener((v, event) -> {
            zoomController.onTouchEvent(event);
            return false;
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress >= 100) {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void setupActions() {
        updateWrapButton();
        wrapBtn.setOnClickListener(v -> toggleWrap());

        openWithBtn.setOnClickListener(v -> openWithOtherApp());
        shareBtn.setOnClickListener(v -> shareFileOrContent());

        zoomBadge.setOnClickListener(v -> {
            if (zoomController != null) {
                zoomController.resetZoom();
            }
        });
    }

    private void resolveAndLoadFile() {
        Intent intent = getIntent();
        if (intent == null) {
            showError("No file intent received.");
            return;
        }

        targetUri = intent.getData();
        if (targetUri == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            targetUri = intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri.class);
        } else if (targetUri == null) {
            targetUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        }

        if (targetUri == null) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (!TextUtils.isEmpty(sharedText)) {
                currentFileName = "Shared Text";
                currentFileType = FileType.TEXT;
                currentContent = sharedText;
                renderContent();
                return;
            }
            showError("Could not resolve file location.");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        errorContainer.setVisibility(View.GONE);

        contentLoader.loadUri(targetUri, intent.getType(), new FileContentLoader.Callback() {
            @Override
            public void onLoaded(@NonNull String fileName, @NonNull FileType fileType, @NonNull String content) {
                currentFileName = fileName;
                currentFileType = fileType;
                currentContent = content;
                renderContent();
            }

            @Override
            public void onError(@NonNull Throwable error) {
                showError("Error opening file: " + error.getMessage());
            }
        });
    }

    private void renderContent() {
        titleView.setText(currentFileName != null ? currentFileName : getString(R.string.title_file_preview));
        badgeView.setText(currentFileType.getBadge());

        if (currentFileType == FileType.MARKDOWN) {
            String html = PreviewHtmlBuilder.buildMarkdownPreviewHtml(this, currentContent, isWrapped);
            webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null);
        } else {
            String html = PreviewHtmlBuilder.buildTextPreviewHtml(this, currentContent, isWrapped);
            webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null);
        }
    }

    private void toggleWrap() {
        isWrapped = !isWrapped;
        updateWrapButton();
        if (webView != null) {
            webView.evaluateJavascript("if (window.setWrapMode) { window.setWrapMode(" + isWrapped + "); }", null);
        }
    }

    private void updateWrapButton() {
        wrapBtn.setImageResource(isWrapped ? R.drawable.ic_wrap_text : R.drawable.ic_unwrap_text);
        wrapBtn.setContentDescription(getString(isWrapped ? R.string.preview_wrap_on : R.string.preview_wrap_off));
    }

    private void onZoomChanged(int zoomPercent) {
        handler.removeCallbacks(hideZoomBadgeRunnable);
        zoomBadge.setText(String.format(Locale.getDefault(), "%d%% • %s", zoomPercent, getString(R.string.preview_zoom_reset)));
        zoomBadge.setAlpha(1f);
        zoomBadge.setVisibility(View.VISIBLE);
        handler.postDelayed(hideZoomBadgeRunnable, 1800);
    }

    private void openWithOtherApp() {
        if (targetUri == null) {
            Toast.makeText(this, "No file URI to open", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent viewIntent = new Intent(Intent.ACTION_VIEW);
        String mime = FileContentLoader.resolveMimeType(this, targetUri, getIntent().getType());
        viewIntent.setDataAndType(targetUri, mime != null ? mime : "*/*");
        viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent chooser = Intent.createChooser(viewIntent, getString(R.string.preview_open_with));
        try {
            startActivity(chooser);
        } catch (Exception e) {
            Toast.makeText(this, "No app available to open this file", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareFileOrContent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        if (targetUri != null) {
            String mime = FileContentLoader.resolveMimeType(this, targetUri, getIntent().getType());
            shareIntent.setType(mime != null ? mime : "text/plain");
            shareIntent.putExtra(Intent.EXTRA_STREAM, targetUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else if (!TextUtils.isEmpty(currentContent)) {
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, currentContent);
        } else {
            return;
        }

        try {
            startActivity(Intent.createChooser(shareIntent, getString(R.string.preview_share)));
        } catch (Exception e) {
            Toast.makeText(this, "Unable to share", Toast.LENGTH_SHORT).show();
        }
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        webView.setVisibility(View.GONE);
        errorContainer.setVisibility(View.VISIBLE);
        errorText.setText(message != null ? message : getString(R.string.preview_error));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (contentLoader != null) {
            contentLoader.shutdown();
        }
        if (webView != null) {
            webView.destroy();
        }
    }
}
