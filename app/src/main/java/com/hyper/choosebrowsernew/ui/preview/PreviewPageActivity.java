package com.hyper.choosebrowsernew.ui.preview;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.os.Environment;
import android.webkit.URLUtil;
import java.io.File;
import com.hyper.choosebrowsernew.ui.downloads.DownloadsActivity;
import com.hyper.choosebrowsernew.ui.downloads.DownloadsHelper;
import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.http.SslCertificate;
import android.provider.Settings;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import java.text.DateFormat;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebViewDatabase;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.hyper.choosebrowsernew.ui.chooser.BrowserChooserBottomSheet;
import com.hyper.choosebrowsernew.data.model.AppInfo;
import com.hyper.choosebrowsernew.data.repository.BrowserRepository;
import com.hyper.choosebrowsernew.ui.common.ViewModelFactory;
import com.hyper.choosebrowsernew.ui.main.MainActivity;
import com.hyper.choosebrowsernew.R;
import com.hyper.choosebrowsernew.UpdateChecker;
import com.hyper.choosebrowsernew.ui.webview.WebViewActivity;
import com.hyper.choosebrowsernew.util.ThemeHelper;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import android.graphics.Typeface;
import android.text.style.StyleSpan;

/**
 * Incognito / Sandbox WebView preview.
 *
 * - All storage, cookies, cache, form data, passwords, etc. are disabled or wiped on exit.
 * - Known tracker / ad domains are blocked at request level.
 * - File access, content provider access are disabled.
 * - JS has a persistent "Allow JS" toggle + temporary address-bar toggle.
 * - Custom error page from assets on NET_ERR.
 * - Bottom nav: Back, Forward, Search, Fire (clear+exit), More menu.
 * - Editable address bar with DuckDuckGo search.
 * - Custom permission dialog (camera, mic, etc.).
 */
public class PreviewPageActivity extends AppCompatActivity {

    private WebView webView;
    private EditText etUrl;
    private ProgressBar progressBar;
    private ImageButton urlSecurityIcon;
    private boolean isCurrentPageSecure = false;
    private SslCertificate currentSslCert = null;
    private SslError currentSslError = null;
    private ImageButton btnToggleJs;
    private ImageButton btnBack;
    private ImageButton btnForward;
    private ImageButton btnClearUrl;
    private View focusActionsBar;

    /*  JS logic:
     *  allowJsEnabled = persistent toggle from More menu.
     *  jsEnabled      = current runtime state (starts as allowJsEnabled).
     *  Address-bar toggle flips jsEnabled temporarily.
     *  More-menu toggle updates BOTH allowJsEnabled and jsEnabled. */
    private View findBar;
    private EditText etFind;
    private TextView tvFindCount;
    private View errorOverlay;
    private TextView errorTitle;
    private TextView tvErrorCode;
    private TextView tvErrorDesc;
    private Button btnErrorRetry;

    private PreviewViewModel viewModel;
    private boolean darkModeEnabled = false;

    private static final String PREFS_NAME = "preview_prefs";
    private static final String KEY_ALLOW_JS = "allow_js";
    private static final String KEY_ALLOW_HTTP = "allow_http";
    private static final String KEY_ALLOW_DOWNLOAD = "allow_download";
    private static final String KEY_AD_BLOCK_MODE = "ad_block_mode";
    private static final int REQ_CODE_PERM = 2001;

    private boolean adBlockEnabled = true;
    private boolean desktopSiteEnabled = false;
    private boolean screenshotAllowed = false;
    private boolean allowHttpEnabled = false;
    private boolean allowDownloadEnabled = false;
    private String currentUrl = "";
    private String failedUrl = "";
    private Bitmap siteFavicon = null;
    private String sitePageTitle = "";

    private static final String DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final String MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";
    private static final String DUCKDUCKGO_SEARCH = "https://duckduckgo.com/?q=";

    /* ════════════════════════════════════════════
     *  onCreate
     * ════════════════════════════════════════════ */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this, new ViewModelFactory(this)).get(PreviewViewModel.class);

        // Block screenshots by default
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        // Dark status / nav bars
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.previewPage_primary));
            getWindow().setNavigationBarColor(getResources().getColor(R.color.previewPage_primary));
        }
        
        // Ensure white status bar icons
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);

        setContentView(R.layout.activity_preview_page);

        // Block access if critical update is required
        if (UpdateChecker.getCachedResult(this).priority == UpdateChecker.Priority.CRITICAL) {
            startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            finish();
            return;
        }

        // ── Bind views ──
        webView         = findViewById(R.id.webView);
        etUrl           = findViewById(R.id.etUrl);
        progressBar     = findViewById(R.id.progressBar);
        btnToggleJs     = findViewById(R.id.btnToggleJs);
        btnBack         = findViewById(R.id.btnBack);
        btnForward      = findViewById(R.id.btnForward);
        btnClearUrl     = findViewById(R.id.btnClearUrl);
        urlSecurityIcon = findViewById(R.id.urlSecurityIcon);
        if (urlSecurityIcon != null) {
            urlSecurityIcon.setOnClickListener(v -> showSecurityTopSheet());
        }
        focusActionsBar = findViewById(R.id.focusActionsBar);
        findBar         = findViewById(R.id.findBar);
        etFind          = findViewById(R.id.etFind);
        tvFindCount     = findViewById(R.id.tvFindCount);

        ImageButton btnFindPrev  = findViewById(R.id.btnFindPrev);
        ImageButton btnFindNext  = findViewById(R.id.btnFindNext);
        ImageButton btnFindClose = findViewById(R.id.btnFindClose);

        ImageButton btnReload  = findViewById(R.id.btnReload);
        ImageButton btnClose   = findViewById(R.id.btnClose);
        ImageButton btnSearch  = findViewById(R.id.btnSearch);
        ImageButton btnFire    = findViewById(R.id.btnFire);
        ImageButton btnMore    = findViewById(R.id.btnMore);

        View focusCopy  = findViewById(R.id.focusBtnCopy);
        View focusShare = findViewById(R.id.focusBtnShare);
        errorOverlay = findViewById(R.id.errorOverlay);
        errorTitle = findViewById(R.id.errorTitle);
        tvErrorCode = findViewById(R.id.errorCode);
        tvErrorDesc = findViewById(R.id.errorDesc);
        btnErrorRetry = findViewById(R.id.btnErrorRetry);
        if (btnErrorRetry != null) {
            btnErrorRetry.setOnClickListener(v -> retryPageLoad());
            applyPressScale(btnErrorRetry);
        }

        String url = getIntent().getStringExtra("url");
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "No URL provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUrl = url;
        allowHttpEnabled = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(KEY_ALLOW_HTTP, false);
        allowDownloadEnabled = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(KEY_ALLOW_DOWNLOAD, false);

        // Observe ViewModel state
        viewModel.jsEnabled.observe(this, enabled -> {
            if (webView != null) {
                webView.getSettings().setJavaScriptEnabled(enabled);
                btnToggleJs.setImageResource(enabled ? R.drawable.js_on : R.drawable.js_off);
            }
        });

        configureIncognitoWebView();
        if (url.contains("private_browser/index.html")) {
            etUrl.setText("");
        } else {
            etUrl.setText(url);
        }
        updateSecurityStatus(currentUrl);

        // ── Address bar: EditText focus behaviour ──
        etUrl.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                etUrl.post(etUrl::selectAll);
                btnClearUrl.setVisibility(View.VISIBLE);
                focusActionsBar.setVisibility(View.VISIBLE);
            } else {
                btnClearUrl.setVisibility(View.GONE);
                focusActionsBar.setVisibility(View.GONE);
                hideKeyboard();
                if (currentUrl != null && !currentUrl.contains("private_browser/index.html")) {
                    etUrl.setText(currentUrl);
                } else {
                    etUrl.setText("");
                }
            }
        });

        // IME "Go" action → navigate
        etUrl.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                        && event.getAction() == KeyEvent.ACTION_DOWN)) {
                navigateToInput(etUrl.getText().toString().trim());
                etUrl.clearFocus();
                return true;
            }
            return false;
        });

        // Clear (X) button in URL bar
        btnClearUrl.setOnClickListener(v -> etUrl.setText(""));

        // Focus-bar Copy
        focusCopy.setOnClickListener(v -> {
            String target = getShareableUrl();
            if (!target.isEmpty()) {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("URL", target));
                    Toast.makeText(this, "Link copied", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "No link to copy", Toast.LENGTH_SHORT).show();
            }
            etUrl.clearFocus();
        });

        // Focus-bar Share
        focusShare.setOnClickListener(v -> {
            String target = getShareableUrl();
            if (!target.isEmpty()) {
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, target);
                startActivity(Intent.createChooser(share, "Share URL"));
            } else {
                Toast.makeText(this, "No link to share", Toast.LENGTH_SHORT).show();
            }
            etUrl.clearFocus();
        });

        ViewCompat.setTooltipText(focusCopy, "Copy link");
        ViewCompat.setTooltipText(focusShare, "Share link");
        applyPressScale(focusCopy);
        applyPressScale(focusShare);

        // ── Address bar buttons ──
        btnReload.setOnClickListener(v -> retryPageLoad());

        btnToggleJs.setOnClickListener(v -> {
            viewModel.toggleJs();
            Toast.makeText(this,
                    (viewModel.jsEnabled.getValue() != null && viewModel.jsEnabled.getValue())
                            ? "JavaScript enabled (temporary)" : "JavaScript disabled (temporary)",
                    Toast.LENGTH_SHORT).show();
            retryPageLoad();
        });

        btnClose.setOnClickListener(v -> {
            clearAllData();
            Toast.makeText(this, "All data cleared", Toast.LENGTH_SHORT).show();
            finish();
        });

        // ── Bottom nav buttons ──
        btnBack.setOnClickListener(v -> {
            if (errorOverlay != null && errorOverlay.getVisibility() == View.VISIBLE) {
                errorOverlay.setVisibility(View.GONE);
            }
            if (webView.canGoBack()) webView.goBack();
        });
        btnForward.setOnClickListener(v -> {
            if (webView.canGoForward()) webView.goForward();
        });

        // Search → focus address bar
        btnSearch.setOnClickListener(v -> {
            etUrl.requestFocus();
            etUrl.post(etUrl::selectAll);
            showKeyboard(etUrl);
        });

        btnFire.setOnClickListener(v -> {
            clearAllData();
            Toast.makeText(this, "All data cleared", Toast.LENGTH_SHORT).show();
            finish();
        });

        btnMore.setOnClickListener(v -> showMoreMenu());

        // ── Tooltip on JS toggle (Temp JS toggle) ──
        ViewCompat.setTooltipText(btnToggleJs, "Temp JS toggle");

        // ── Press scale animation for all buttons ──
        applyPressScale(btnReload);
        applyPressScale(btnToggleJs);
        applyPressScale(btnClose);
        applyPressScale(btnBack);
        applyPressScale(btnForward);
        applyPressScale(btnSearch);
        applyPressScale(btnFire);
        applyPressScale(btnMore);

        // ── Find bar setup ──
        webView.setFindListener((activeMatchOrdinal, numberOfMatches, isDoneCounting) -> {
            if (isDoneCounting) {
                tvFindCount.setText(numberOfMatches > 0
                        ? (activeMatchOrdinal + 1) + "/" + numberOfMatches
                        : "0/0");
            }
        });

        etFind.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s.toString().trim();
                if (TextUtils.isEmpty(q)) {
                    webView.clearMatches();
                    tvFindCount.setText("0/0");
                } else {
                    webView.findAllAsync(q);
                }
            }
        });

        btnFindPrev.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(etFind.getText())) webView.findNext(false);
        });
        btnFindNext.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(etFind.getText())) webView.findNext(true);
        });
        btnFindClose.setOnClickListener(v -> closeFindBar());

        applyPressScale(btnFindPrev);
        applyPressScale(btnFindNext);

        // JS interface for error page retry / goBack
        webView.addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public void goBack() {
                runOnUiThread(() -> {
                    if (webView.canGoBack() && !currentUrl.contains("private_browser/error.html")) {
                        webView.goBack();
                    } else {
                        webView.loadUrl("file:///android_asset/private_browser/index.html");
                    }
                });
            }
            @android.webkit.JavascriptInterface
            public void retry() {
                runOnUiThread(PreviewPageActivity.this::retryPageLoad);
            }
        }, "Android");

        // Load page
        if (url.startsWith("http://") && !allowHttpEnabled) {
            failedUrl = url;
            currentUrl = url;
            if (etUrl != null && !etUrl.hasFocus()) {
                etUrl.setText(url);
            }
            updateSecurityStatus(url);
            loadErrorPage(url, "ERR_HTTP_BLOCKED",
                    "Unencrypted HTTP websites are blocked by default for your safety.\nTo load this page, enable 'Allow HTTP connection' in the More menu.");
            return;
        }
        webView.loadUrl(url);
    }

    /* ════════════════════════════════════════════
     *  Navigate to URL or DuckDuckGo search
     * ════════════════════════════════════════════ */
    private void navigateToInput(String input) {
        if (errorOverlay != null) {
            errorOverlay.setVisibility(View.GONE);
        }
        if (TextUtils.isEmpty(input)) return;

        String urlToLoad;
        if (input.startsWith("http://") || input.startsWith("https://")) {
            urlToLoad = input;
        } else if (input.contains(".") && !input.contains(" ")) {
            // Bare domain like "google.com"
            urlToLoad = "https://" + input;
        } else {
            // Search with DuckDuckGo
            try {
                urlToLoad = DUCKDUCKGO_SEARCH + URLEncoder.encode(input, "UTF-8");
            } catch (Exception e) {
                urlToLoad = DUCKDUCKGO_SEARCH + input;
            }
        }
        if (urlToLoad.startsWith("http://") && !allowHttpEnabled) {
            failedUrl = urlToLoad;
            currentUrl = urlToLoad;
            if (etUrl != null && !etUrl.hasFocus()) {
                etUrl.setText(urlToLoad);
            }
            updateSecurityStatus(urlToLoad);
            loadErrorPage(urlToLoad, "ERR_HTTP_BLOCKED",
                    "Unencrypted HTTP websites are blocked by default for your safety.\nTo load this page, enable 'Allow HTTP connection' in the More menu.");
            return;
        }
        webView.loadUrl(urlToLoad);
    }

    /* ════════════════════════════════════════════
     *  Configure WebView as sandboxed incognito
     * ════════════════════════════════════════════ */
    @SuppressLint("SetJavaScriptEnabled")
    private void configureIncognitoWebView() {
        WebSettings s = webView.getSettings();

        s.setUserAgentString(MOBILE_USER_AGENT);
        s.setJavaScriptEnabled(viewModel.jsEnabled.getValue() != null && viewModel.jsEnabled.getValue());
        s.setDomStorageEnabled(true);

        // Disable persistent storage
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);
        s.setDatabaseEnabled(false);
        s.setSaveFormData(false);
        s.setSavePassword(false);

        // Disable file / content access
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setAllowFileAccessFromFileURLs(false);
        s.setAllowUniversalAccessFromFileURLs(false);

        // Disable geolocation
        s.setGeolocationEnabled(false);

        // Other hardening
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setMediaPlaybackRequiresUserGesture(true);

        // Block cross-site (third-party) cookies while allowing first-party session cookies
        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(webView, false);

        // ── WebChromeClient (progress, title, favicon, permissions) ──
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                if (title != null && !title.isEmpty()) {
                    sitePageTitle = title;
                }
            }

            @Override
            public void onReceivedIcon(WebView view, Bitmap icon) {
                super.onReceivedIcon(view, icon);
                siteFavicon = icon;
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> showPermissionDialog(request));
            }
        });

        // ── WebViewClient (URL updates + tracker blocking + error page) ──
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                currentUrl = url;
                currentSslError = null;
                currentSslCert = null;
                updateSecurityStatus(url);
                if (url.startsWith("http://") && !allowHttpEnabled) {
                    view.stopLoading();
                    failedUrl = url;
                    loadErrorPage(url, "ERR_HTTP_BLOCKED",
                            "Unencrypted HTTP websites are blocked by default for your safety.\nTo load this page, enable 'Allow HTTP connection' in the More menu.");
                    return;
                }
                if (errorOverlay != null) {
                    errorOverlay.setVisibility(View.GONE);
                }
                if (!etUrl.hasFocus()) {
                    if (url.contains("private_browser/index.html")) {
                        etUrl.setText("");
                    } else {
                        etUrl.setText(url);
                    }
                }
                updateNavButtons();
                viewModel.resetBlockedCount();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                currentUrl = url;
                updateSecurityStatus(url);
                if (!etUrl.hasFocus()) {
                    if (url.contains("private_browser/index.html")) {
                        etUrl.setText("");
                    } else {
                        etUrl.setText(url);
                    }
                }
                updateNavButtons();
                if (darkModeEnabled) applyDarkMode(true);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    String errorCode = "";
                    String errorDesc = "The page could not be loaded.";
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        errorCode = "ERR_" + error.getErrorCode();
                        errorDesc = error.getDescription().toString();
                    }
                    loadErrorPage(request.getUrl().toString(), errorCode, errorDesc);
                }
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                if (request.isForMainFrame() && errorResponse.getStatusCode() >= 400) {
                    loadErrorPage(request.getUrl().toString(),
                            "HTTP_" + errorResponse.getStatusCode(),
                            "Server returned " + errorResponse.getStatusCode());
                }
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (adBlockEnabled && !request.isForMainFrame()) {
                    String host = request.getUrl().getHost();
                    if (host != null && viewModel.shouldBlock(host)) {
                        viewModel.incrementBlockedCount();
                        return new WebResourceResponse("text/plain", "utf-8",
                                new ByteArrayInputStream("".getBytes()));
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                String errUrl = error.getUrl();
                currentSslError = error;
                updateSecurityStatus(errUrl);
                if (errUrl != null && errUrl.equals(currentUrl)) {
                    handler.cancel();
                    loadErrorPage(errUrl, "SSL_ERROR", "SSL certificate error – page blocked for safety.");
                } else {
                    handler.cancel();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("http://")) {
                    if (!allowHttpEnabled) {
                        view.stopLoading();
                        failedUrl = url;
                        currentUrl = url;
                        if (etUrl != null && !etUrl.hasFocus()) {
                            etUrl.setText(url);
                        }
                        updateSecurityStatus(url);
                        loadErrorPage(url, "ERR_HTTP_BLOCKED",
                                "Unencrypted HTTP websites are blocked by default for your safety.\nTo load this page, enable 'Allow HTTP connection' in the More menu.");
                        return true;
                    }
                    return false;
                }
                if (url.startsWith("https://")) {
                    return false;
                }
                return true; // block intent://, market://, tel:, mailto:, etc.
            }
        });

        // Native download listener
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) ->
                handleIncomingDownload(url, userAgent, contentDisposition, mimetype, contentLength));
    }

    /* ════════════════════════════════════════════
     *  Download Management Engine
     * ════════════════════════════════════════════ */
    private void handleIncomingDownload(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
        String guessedName = URLUtil.guessFileName(url, contentDisposition, mimeType);
        if (!allowDownloadEnabled) {
            showDownloadBlockedDialog(url, userAgent, mimeType, guessedName, contentLength);
        } else {
            showDownloadPromptDialog(url, userAgent, mimeType, guessedName, contentLength);
        }
    }

    private void showDownloadBlockedDialog(String url, String userAgent, String mimeType, String fileName, long contentLength) {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.PreviewMoreBottomSheet);
        View view = getLayoutInflater().inflate(R.layout.dialog_download_blocked, null);
        dialog.setContentView(view);

        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) bottomSheet.setBackgroundColor(Color.TRANSPARENT);
        if (dialog.getWindow() != null) {
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.7f);
        }

        view.findViewById(R.id.btnBlockedClose).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnBlockedCancel).setOnClickListener(v -> dialog.dismiss());

        TextView tvBaseName = view.findViewById(R.id.tvBlockedBaseName);
        TextView tvExt = view.findViewById(R.id.tvBlockedExtension);
        TextView tvSize = view.findViewById(R.id.tvBlockedFileSize);
        TextView tvWarning = view.findViewById(R.id.tvBlockedWarningMsg);

        String baseName = DownloadsHelper.getBaseName(fileName);
        String ext = DownloadsHelper.getFileExtension(fileName);
        boolean isHarmful = DownloadsHelper.isHarmfulExtension(fileName);

        tvBaseName.setText(baseName);
        if (!ext.isEmpty()) {
            tvExt.setText("." + ext);
            tvExt.setVisibility(View.VISIBLE);
            if (isHarmful) {
                tvExt.setTextColor(Color.parseColor("#FFD600")); // Yellow
            } else {
                tvExt.setTextColor(Color.parseColor("#4C8BF5")); // Blue
            }
        } else {
            tvExt.setVisibility(View.GONE);
        }

        tvSize.setText("Estimated size: " + DownloadsHelper.formatFileSize(contentLength));

        if (isHarmful) {
            tvWarning.setText("Harmful File Warning: This file (." + ext + ") is an executable or script application that could potentially harm your device. Downloads are currently disabled.");
            tvWarning.setTextColor(Color.parseColor("#FFD600"));
        } else {
            tvWarning.setText("Downloads are currently disabled in Preview Mode. You can enable 'Allow Download' to download this file.");
            tvWarning.setTextColor(Color.parseColor("#CCFFFFFF"));
        }

        view.findViewById(R.id.btnBlockedEnable).setOnClickListener(v -> {
            allowDownloadEnabled = true;
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit().putBoolean(KEY_ALLOW_DOWNLOAD, true).apply();
            dialog.dismiss();
            Toast.makeText(this, "Downloads enabled", Toast.LENGTH_SHORT).show();
            showDownloadPromptDialog(url, userAgent, mimeType, fileName, contentLength);
        });

        dialog.show();
    }

    private void showDownloadPromptDialog(String url, String userAgent, String mimeType, String fileName, long contentLength) {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.PreviewMoreBottomSheet);
        View view = getLayoutInflater().inflate(R.layout.dialog_download_prompt, null);
        dialog.setContentView(view);

        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) bottomSheet.setBackgroundColor(Color.TRANSPARENT);
        if (dialog.getWindow() != null) {
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.7f);
        }

        EditText etFileName = view.findViewById(R.id.etPromptFileName);
        etFileName.setText(fileName);

        TextView tvSize = view.findViewById(R.id.tvPromptFileSize);
        tvSize.setText("Estimated size: " + DownloadsHelper.formatFileSize(contentLength));

        boolean isHarmful = DownloadsHelper.isHarmfulExtension(fileName);

        View layoutSafeActions = view.findViewById(R.id.layoutSafeActions);
        View layoutHarmfulCard = view.findViewById(R.id.layoutHarmfulCard);

        if (isHarmful) {
            layoutSafeActions.setVisibility(View.GONE);
            layoutHarmfulCard.setVisibility(View.VISIBLE);

            View headerAdvanced = view.findViewById(R.id.headerAdvancedOptions);
            View containerAdvanced = view.findViewById(R.id.containerAdvancedOptions);
            TextView tvArrow = view.findViewById(R.id.tvAdvancedArrow);

            headerAdvanced.setOnClickListener(v -> {
                boolean isExpanded = containerAdvanced.getVisibility() == View.VISIBLE;
                containerAdvanced.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
                tvArrow.setText(isExpanded ? "▼" : "▲");
            });

            view.findViewById(R.id.btnDownloadAnyway).setOnClickListener(v -> {
                String chosenName = etFileName.getText().toString().trim();
                if (chosenName.isEmpty()) chosenName = fileName;
                startNativeDownload(url, userAgent, mimeType, chosenName);
                dialog.dismiss();
            });

            view.findViewById(R.id.btnHarmfulCancel).setOnClickListener(v -> dialog.dismiss());
        } else {
            layoutSafeActions.setVisibility(View.VISIBLE);
            layoutHarmfulCard.setVisibility(View.GONE);

            view.findViewById(R.id.rowSafeDownload).setOnClickListener(v -> {
                String chosenName = etFileName.getText().toString().trim();
                if (chosenName.isEmpty()) chosenName = fileName;
                startNativeDownload(url, userAgent, mimeType, chosenName);
                dialog.dismiss();
            });

            view.findViewById(R.id.rowSafeCancel).setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void startNativeDownload(String url, String userAgent, String mimeType, String fileName) {
        try {
            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm == null) {
                Toast.makeText(this, "Download Manager unavailable", Toast.LENGTH_SHORT).show();
                return;
            }

            File dir = DownloadsHelper.getDownloadsDir();
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle(fileName);
            request.setDescription("Downloading via ChooseBrowser...");
            if (mimeType != null && !mimeType.isEmpty()) {
                request.setMimeType(mimeType);
            }
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, DownloadsHelper.FOLDER_NAME + "/" + fileName);

            String cookie = CookieManager.getInstance().getCookie(url);
            if (cookie != null) {
                request.addRequestHeader("Cookie", cookie);
            }
            if (userAgent != null) {
                request.addRequestHeader("User-Agent", userAgent);
            }

            dm.enqueue(request);
            Toast.makeText(this, "Downloading " + fileName + "...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to start download: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /* ════════════════════════════════════════════
     *  Custom Permission Dialog
     * ════════════════════════════════════════════ */
    private void showPermissionDialog(final PermissionRequest request) {
        String[] resources = request.getResources();
        StringBuilder perms = new StringBuilder();
        for (String r : resources) {
            if (r.equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                perms.append("camera");
            } else if (r.equals(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                perms.append("microphone");
            } else if (r.equals(PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID)) {
                perms.append("protected media");
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    && r.equals(PermissionRequest.RESOURCE_MIDI_SYSEX)) {
                perms.append("MIDI device");
            } else {
                perms.append("a resource");
            }
            perms.append("/");
        }
        if (perms.length() > 0) perms.setLength(perms.length() - 1);

        String host = "This site";
        try {
            Uri uri = Uri.parse(currentUrl);
            if (uri.getHost() != null) host = uri.getHost();
        } catch (Exception ignored) {}

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dlg = getLayoutInflater().inflate(R.layout.dialog_permission_request, null);
        builder.setView(dlg);
        builder.setCancelable(true);

        TextView msg = dlg.findViewById(R.id.permMessage);
        String fullMessage = host + " wants to use your " + perms;
        SpannableString styledMessage = new SpannableString(fullMessage);
        styledMessage.setSpan(
            new StyleSpan(Typeface.BOLD),
            0,
            host.length(),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        msg.setText(styledMessage);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dlg.findViewById(R.id.btnAllowOnce).setOnClickListener(v -> {
            request.grant(request.getResources());
            dialog.dismiss();
        });

        dlg.findViewById(R.id.btnDontAllow).setOnClickListener(v -> {
            request.deny();
            dialog.dismiss();
        });

        dialog.setOnCancelListener(d -> request.deny());
        dialog.show();
    }

    /* ════════════════════════════════════════════
     *  Native Chrome-style error overlay & retry
     * ════════════════════════════════════════════ */
    private void loadErrorPage(String url, String errorCode, String errorDesc) {
        failedUrl = url;
        runOnUiThread(() -> {
            if (errorOverlay != null) {
                if (tvErrorCode != null) {
                    tvErrorCode.setText(errorCode != null && !errorCode.isEmpty() ? errorCode : "NET_ERROR");
                }
                if (tvErrorDesc != null) {
                    tvErrorDesc.setText(errorDesc != null && !errorDesc.isEmpty() ? errorDesc : "The page could not be loaded.");
                }
                errorOverlay.setVisibility(View.VISIBLE);
            }
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void retryPageLoad() {
        if (errorOverlay != null) {
            errorOverlay.setVisibility(View.GONE);
        }
        String targetUrl = (failedUrl != null && !failedUrl.isEmpty()) ? failedUrl : currentUrl;
        if (targetUrl != null && !targetUrl.isEmpty() && !targetUrl.contains("private_browser/index.html")) {
            webView.loadUrl(targetUrl);
        } else {
            webView.reload();
        }
    }

    /* ── Update back/forward button alpha ── */
    private void updateNavButtons() {
        btnBack.setAlpha(webView.canGoBack() ? 1.0f : 0.4f);
        btnForward.setAlpha(webView.canGoForward() ? 1.0f : 0.4f);
    }

    /* ════════════════════════════════════════════
     *  Connection Security & Top Sheet
     * ════════════════════════════════════════════ */
    private void updateSecurityStatus(String url) {
        if (url == null || url.contains("private_browser/index.html") || url.equals("about:blank")) {
            if (urlSecurityIcon != null) {
                urlSecurityIcon.setVisibility(View.GONE);
            }
            isCurrentPageSecure = false;
            currentSslCert = null;
            return;
        }

        if (urlSecurityIcon == null) return;
        urlSecurityIcon.setVisibility(View.VISIBLE);

        if (url.startsWith("https://")) {
            SslCertificate cert = (webView != null) ? webView.getCertificate() : null;
            if (cert != null) {
                currentSslCert = cert;
            }
            if (currentSslError == null) {
                isCurrentPageSecure = true;
                urlSecurityIcon.setImageResource(R.drawable.pp_secure_lock);
                urlSecurityIcon.setContentDescription("Connection is secure");
            } else {
                isCurrentPageSecure = false;
                urlSecurityIcon.setImageResource(R.drawable.pp_site_warning);
                urlSecurityIcon.setContentDescription("Connection is not secure");
            }
        } else if (url.startsWith("http://")) {
            isCurrentPageSecure = false;
            currentSslCert = null;
            urlSecurityIcon.setImageResource(R.drawable.pp_site_warning);
            urlSecurityIcon.setContentDescription("Connection is not secure");
        } else {
            urlSecurityIcon.setVisibility(View.GONE);
        }
    }

    private void showSecurityTopSheet() {
        if (currentUrl == null || currentUrl.contains("private_browser/index.html")) {
            return;
        }
        if (etUrl != null && etUrl.hasFocus()) {
            etUrl.clearFocus();
            hideKeyboard();
        }

        Dialog dialog = new Dialog(this, R.style.PreviewTopSheetDialog);
        View sheetView = getLayoutInflater().inflate(R.layout.topsheet_preview_security, null);
        dialog.setContentView(sheetView);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setGravity(Gravity.TOP);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setDimAmount(0.5f);
            window.setWindowAnimations(R.style.Animation_TopSheet);
        }

        // Host name
        TextView tvHost = sheetView.findViewById(R.id.tvSecurityHost);
        ImageView headerHostIcon = sheetView.findViewById(R.id.headerHostIcon);
        String host = "";
        try {
            Uri uri = Uri.parse(currentUrl);
            host = uri.getHost() != null ? uri.getHost() : currentUrl;
        } catch (Exception e) {
            host = currentUrl;
        }
        if (tvHost != null) {
            tvHost.setText(host);
        }

        // Close button
        View btnClose = sheetView.findViewById(R.id.btnCloseSecuritySheet);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        // Status Header & Views
        ImageView statusIcon = sheetView.findViewById(R.id.securityStatusIcon);
        TextView statusTitle = sheetView.findViewById(R.id.securityStatusTitle);

        ImageView certIcon = sheetView.findViewById(R.id.securityCertIcon);
        TextView issuerTitle = sheetView.findViewById(R.id.securityIssuerTitle);
        TextView issuerDesc = sheetView.findViewById(R.id.securityIssuerDesc);
        TextView certDetails = sheetView.findViewById(R.id.securityCertDetails);

        ImageView encIcon = sheetView.findViewById(R.id.securityEncIcon);
        TextView encTitle = sheetView.findViewById(R.id.securityEncryptionTitle);
        TextView encDesc = sheetView.findViewById(R.id.securityEncryptionDesc);

        // Fetch certificate if available
        if (currentSslCert == null && webView != null) {
            currentSslCert = webView.getCertificate();
        }

        if (isCurrentPageSecure && currentUrl.startsWith("https://")) {
            // SECURE
            if (headerHostIcon != null) headerHostIcon.setImageResource(R.drawable.pp_secure_lock);
            if (statusIcon != null) statusIcon.setImageResource(R.drawable.pp_secure_lock);
            if (statusTitle != null) statusTitle.setText("Connection is secure");

            if (certIcon != null) certIcon.setImageResource(R.drawable.pp_secure_lock);
            if (issuerTitle != null) issuerTitle.setText("Verified Certificate");

            String issuer = null;
            if (currentSslCert != null && currentSslCert.getIssuedBy() != null) {
                issuer = currentSslCert.getIssuedBy().getCName();
                if (issuer == null || issuer.isEmpty()) {
                    issuer = currentSslCert.getIssuedBy().getOName();
                }
            }

            if (issuer != null && !issuer.isEmpty()) {
                if (issuerDesc != null) {
                    issuerDesc.setText("ChooseBrowser verified that " + issuer + " issued this website's certificate.");
                }
            } else {
                if (issuerDesc != null) {
                    issuerDesc.setText("ChooseBrowser verified that a trusted certificate authority issued this website's certificate.");
                }
            }

            if (currentSslCert != null && currentSslCert.getValidNotAfterDate() != null) {
                String expiryStr = DateFormat.getDateInstance(DateFormat.MEDIUM)
                        .format(currentSslCert.getValidNotAfterDate());
                if (certDetails != null) {
                    certDetails.setText("Valid until: " + expiryStr);
                    certDetails.setVisibility(View.VISIBLE);
                }
            } else {
                if (certDetails != null) certDetails.setVisibility(View.GONE);
            }

            if (encIcon != null) encIcon.setImageResource(R.drawable.pp_shield);
            if (encTitle != null) encTitle.setText("Private & Encrypted");
            if (encDesc != null) {
                encDesc.setText("Your information (like passwords or credit card numbers) is private when sent to this site.");
            }
        } else {
            // INSECURE / WARNING
            if (headerHostIcon != null) headerHostIcon.setImageResource(R.drawable.pp_site_warning);
            if (statusIcon != null) statusIcon.setImageResource(R.drawable.pp_site_warning);
            if (statusTitle != null) statusTitle.setText("Connection is not secure");

            if (certIcon != null) certIcon.setImageResource(R.drawable.pp_site_warning);
            if (issuerTitle != null) issuerTitle.setText("Sensitive Information Warning");
            if (issuerDesc != null) {
                issuerDesc.setText("You should not enter any sensitive information on this site (for example, passwords or credit cards), because it could be stolen by attackers.");
            }

            if (certDetails != null) {
                if (currentSslError != null) {
                    certDetails.setText("Reason: Invalid or untrusted security certificate.");
                } else {
                    certDetails.setText("Reason: Plain unencrypted HTTP connection.");
                }
                certDetails.setVisibility(View.VISIBLE);
            }

            if (encIcon != null) encIcon.setImageResource(R.drawable.pp_site_warning);
            if (encTitle != null) encTitle.setText("Unencrypted Connection");
            if (encDesc != null) {
                encDesc.setText("This website does not use modern HTTPS encryption. Anyone on your local network or Wi-Fi can see the pages you view.");
            }
        }

        dialog.show();
    }

    /* ════════════════════════════════════════════
     *  "More" bottom sheet menu
     * ════════════════════════════════════════════ */
    private void showMoreMenu() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.PreviewMoreBottomSheet);
        View view = getLayoutInflater().inflate(R.layout.bottomsheet_preview_more, null);
        dialog.setContentView(view);

        // Transparent background for rounded corners
        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.setBackgroundColor(Color.TRANSPARENT);
        }

        // ── Site title capsule: favicon + page <title> ──
        TextView siteTitle = view.findViewById(R.id.siteTitle);
        ImageView siteIcon  = view.findViewById(R.id.siteIcon);

        // Prefer page <title>, fall back to host
        if (!TextUtils.isEmpty(sitePageTitle)) {
            siteTitle.setText(sitePageTitle);
        } else {
            try {
                Uri uri = Uri.parse(currentUrl);
                siteTitle.setText(uri.getHost() != null ? uri.getHost() : currentUrl);
            } catch (Exception e) {
                siteTitle.setText(currentUrl);
            }
        }

        // Captured favicon
        if (siteFavicon != null) {
            siteIcon.setImageBitmap(siteFavicon);
        }

        // Copy link
        view.findViewById(R.id.moreCopyLinkBtn).setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("URL", currentUrl));
                Toast.makeText(this, "Link copied", Toast.LENGTH_SHORT).show();
            }
        });

        // Reload
        view.findViewById(R.id.moreReloadBtn).setOnClickListener(v -> {
            retryPageLoad();
            dialog.dismiss();
        });

        // ── Grid Row 1: Open In, Home, Print, Find ──

        view.findViewById(R.id.moreOpenIn).setOnClickListener(v -> {
            dialog.dismiss();
            if (!TextUtils.isEmpty(currentUrl)) {
                BrowserChooserBottomSheet chooser = BrowserChooserBottomSheet.newInstance(currentUrl);
                chooser.show(getSupportFragmentManager(), "browser_chooser");
            } else {
                Toast.makeText(this, "No URL to open", Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.moreHome).setOnClickListener(v -> {
            if (errorOverlay != null) {
                errorOverlay.setVisibility(View.GONE);
            }
            webView.loadUrl("file:///android_asset/private_browser/index.html");
            dialog.dismiss();
        });

        view.findViewById(R.id.morePrint).setOnClickListener(v -> {
            PrintManager pm = (PrintManager) getSystemService(Context.PRINT_SERVICE);
            if (pm != null) {
                PrintDocumentAdapter adapter = webView.createPrintDocumentAdapter("Preview_Page");
                pm.print("Preview Page", adapter, new PrintAttributes.Builder().build());
            }
            dialog.dismiss();
        });

        view.findViewById(R.id.moreFind).setOnClickListener(v -> {
            dialog.dismiss();
            findBar.setVisibility(View.VISIBLE);
            etFind.requestFocus();
            showKeyboard(etFind);
        });

        // ── Grid Row 2: Share, Ad Block, Permissions, Dark Mode ──

        view.findViewById(R.id.moreShare).setOnClickListener(v -> {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, currentUrl);
            startActivity(Intent.createChooser(share, "Share URL"));
            dialog.dismiss();
        });

        // Ad Block toggle
        ImageView adBlockIcon = view.findViewById(R.id.adBlockIcon);
        TextView adBlockText  = view.findViewById(R.id.adBlockText);
        adBlockIcon.setImageResource(adBlockEnabled ? R.drawable.ad_close : R.drawable.ad);
        adBlockText.setText(adBlockEnabled ? "Ad Blocker" : "Ads On");
        view.findViewById(R.id.moreAdBlock).setOnClickListener(v -> {
            adBlockEnabled = !adBlockEnabled;
            adBlockIcon.setImageResource(adBlockEnabled ? R.drawable.ad_close : R.drawable.ad);
            adBlockText.setText(adBlockEnabled ? "Ad Blocker" : "Ads On");
            Toast.makeText(this, adBlockEnabled ? "Ad Blocker enabled" : "Ad Blocker disabled",
                    Toast.LENGTH_SHORT).show();
            retryPageLoad();
        });

        // Permissions page (moved to bottom group)
        view.findViewById(R.id.morePermissions).setOnClickListener(v -> {
            dialog.dismiss();
            showPermissionsSheet();
        });

        // Downloads
        View moreDownloads = view.findViewById(R.id.moreDownloads);
        if (moreDownloads != null) {
            applyPressScale(moreDownloads);
            moreDownloads.setOnClickListener(v -> {
                dialog.dismiss();
                startActivity(new Intent(this, DownloadsActivity.class));
            });
        }

        // Dark Mode toggle
        ImageView darkModeIcon = view.findViewById(R.id.darkModeIcon);
        TextView  darkModeText = view.findViewById(R.id.darkModeText);
        darkModeIcon.setImageResource(darkModeEnabled ? R.drawable.darkmode : R.drawable.darkmode_off);
        darkModeText.setText(darkModeEnabled ? "Light Off" : "Dark Mode");
        view.findViewById(R.id.moreDarkMode).setOnClickListener(v -> {
            darkModeEnabled = !darkModeEnabled;
            applyDarkMode(darkModeEnabled);
            dialog.dismiss();
        });

        // ── Open in App (if native apps available for currentUrl) ──
        View rowOpenInApp = view.findViewById(R.id.rowOpenInApp);
        TextView tvOpenInAppName = view.findViewById(R.id.tvOpenInAppName);
        if (rowOpenInApp != null && !TextUtils.isEmpty(currentUrl)) {
            BrowserRepository repo = new BrowserRepository(this);
            List<AppInfo> nativeApps = repo.getNativeAppsForUrl(currentUrl);

            if (nativeApps != null && !nativeApps.isEmpty()) {
                rowOpenInApp.setVisibility(View.VISIBLE);
                AppInfo firstApp = nativeApps.get(0);
                if (nativeApps.size() == 1 && tvOpenInAppName != null) {
                    tvOpenInAppName.setText(firstApp.name);
                } else if (tvOpenInAppName != null) {
                    tvOpenInAppName.setText(nativeApps.size() + " apps");
                }

                rowOpenInApp.setOnClickListener(v -> {
                    dialog.dismiss();
                    if (nativeApps.size() == 1) {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl));
                            intent.setPackage(firstApp.packageName);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(this, "Failed to open app", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        BrowserChooserBottomSheet chooser = BrowserChooserBottomSheet.newInstance(currentUrl);
                        chooser.show(getSupportFragmentManager(), "browser_chooser");
                    }
                });
            } else {
                rowOpenInApp.setVisibility(View.GONE);
            }
        } else if (rowOpenInApp != null) {
            rowOpenInApp.setVisibility(View.GONE);
        }

        // ── Toggles ──

        // Desktop site
        SwitchCompat switchDesktop = view.findViewById(R.id.switchDesktopSite);
        switchDesktop.setChecked(desktopSiteEnabled);
        switchDesktop.setOnCheckedChangeListener((btn, isChecked) -> {
            desktopSiteEnabled = isChecked;
            WebSettings ws = webView.getSettings();
            ws.setUserAgentString(isChecked ? DESKTOP_USER_AGENT : MOBILE_USER_AGENT);
            ws.setUseWideViewPort(isChecked);
            ws.setLoadWithOverviewMode(isChecked);
            webView.reload();
        });

        // Screenshot
        SwitchCompat switchScreenshot = view.findViewById(R.id.switchScreenshot);
        switchScreenshot.setChecked(screenshotAllowed);
        switchScreenshot.setOnCheckedChangeListener((btn, isChecked) -> {
            screenshotAllowed = isChecked;
            if (isChecked) {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
            } else {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE);
            }
            Toast.makeText(this,
                    isChecked ? "Screenshots allowed" : "Screenshots blocked",
                    Toast.LENGTH_SHORT).show();
        });

        // Allow Download
        SwitchCompat switchAllowDownload = view.findViewById(R.id.switchAllowDownload);
        if (switchAllowDownload != null) {
            switchAllowDownload.setChecked(allowDownloadEnabled);
            switchAllowDownload.setOnCheckedChangeListener((btn, isChecked) -> {
                allowDownloadEnabled = isChecked;
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        .edit().putBoolean(KEY_ALLOW_DOWNLOAD, isChecked).apply();
                Toast.makeText(this,
                        isChecked ? "Downloads allowed" : "Downloads blocked",
                        Toast.LENGTH_SHORT).show();
            });
        }

        // Allow JS (persistent toggle)
        SwitchCompat switchAllowJs = view.findViewById(R.id.switchAllowJs);

        switchAllowJs.setChecked(viewModel.isPersistentJsEnabled());
        switchAllowJs.setOnCheckedChangeListener((btn, isChecked) -> {
            viewModel.setPersistentJs(isChecked);
            Toast.makeText(this,
                    isChecked ? "JavaScript enabled — will persist on next load"
                              : "JavaScript disabled — will persist on next load",
                    Toast.LENGTH_SHORT).show();
            retryPageLoad();
        });

        // Allow HTTP (insecure connection toggle - disabled by default)
        SwitchCompat switchAllowHttp = view.findViewById(R.id.switchAllowHttp);
        if (switchAllowHttp != null) {
            switchAllowHttp.setChecked(allowHttpEnabled);
            switchAllowHttp.setOnCheckedChangeListener((btn, isChecked) -> {
                allowHttpEnabled = isChecked;
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        .edit().putBoolean(KEY_ALLOW_HTTP, isChecked).apply();
                Toast.makeText(this,
                        isChecked ? "HTTP connections allowed (Insecure)"
                                  : "HTTP connections blocked",
                        Toast.LENGTH_SHORT).show();
                if (isChecked && failedUrl != null && failedUrl.startsWith("http://")) {
                    retryPageLoad();
                }
            });
        }

        // ── Style all switches with Material 3 Monochrome ──
        for (SwitchCompat sw : new SwitchCompat[]{ switchDesktop, switchScreenshot, switchAllowDownload, switchAllowJs, switchAllowHttp }) {
            if (sw != null) {
                ThemeHelper.applyMonochromeSwitchTint(sw, this, true);
            }
        }

        // Ad-Blocker Mode row (below Allow JavaScript switch)
        TextView tvAdBlockMode = view.findViewById(R.id.tvAdBlockMode);
        String currentMode = viewModel.adBlockMode.getValue();
        tvAdBlockMode.setText("dns".equals(currentMode) ? "AdGuard DNS" : "Domain Filter List");
        view.findViewById(R.id.moreAdBlockMode).setOnClickListener(v -> {
            dialog.dismiss();
            showAdBlockModeDialog();
        });

        // Privacy Policy
        view.findViewById(R.id.morePrivacyPolicy).setOnClickListener(v -> {
            WebViewActivity.openPrivacyPolicy(this);
            dialog.dismiss();
        });

        dialog.show();
    }

    /* ════════════════════════════════════════════
     *  Ad-Blocker Mode dialog
     * ════════════════════════════════════════════ */
    private void showAdBlockModeDialog() {
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.PreviewMoreBottomSheet);
        View view = getLayoutInflater().inflate(R.layout.bottomsheet_adblock_mode, null);
        sheet.setContentView(view);

        View bg = sheet.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bg != null) bg.setBackgroundColor(Color.TRANSPARENT);

        ImageView checkFilter = view.findViewById(R.id.adBlockCheckFilter);
        ImageView checkDns    = view.findViewById(R.id.adBlockCheckDns);

        String currentMode = viewModel.adBlockMode.getValue();
        checkFilter.setVisibility("filter".equals(currentMode) ? View.VISIBLE : View.GONE);
        checkDns.setVisibility("dns".equals(currentMode) ? View.VISIBLE : View.GONE);

        view.findViewById(R.id.adBlockModeFilter).setOnClickListener(v -> {
            viewModel.setAdBlockMode("filter");
            Toast.makeText(this, "Ad-Blocker: Domain Filter List", Toast.LENGTH_SHORT).show();
            sheet.dismiss();
            webView.reload();
        });

        view.findViewById(R.id.adBlockModeDns).setOnClickListener(v -> {
            viewModel.setAdBlockMode("dns");
            Toast.makeText(this, "Ad-Blocker: AdGuard DNS", Toast.LENGTH_SHORT).show();
            sheet.dismiss();
            webView.reload();
        });

        sheet.show();
    }

    /* ════════════════════════════════════════════
     *  Find bar helper
     * ════════════════════════════════════════════ */
    private void closeFindBar() {
        webView.clearMatches();
        etFind.setText("");
        tvFindCount.setText("0/0");
        findBar.setVisibility(View.GONE);
        hideKeyboard();
    }

    /* ════════════════════════════════════════════
     *  Dark Mode injection
     * ════════════════════════════════════════════ */
    @SuppressLint("SetJavaScriptEnabled")
    private void applyDarkMode(boolean enable) {
        boolean wasJs = webView.getSettings().getJavaScriptEnabled();
        if (!wasJs) webView.getSettings().setJavaScriptEnabled(true);
        if (enable) {
            webView.evaluateJavascript(
                "(function(){" +
                "if(document.getElementById('_pdm'))return;" +
                "var s=document.createElement('style');" +
                "s.id='_pdm';" +
                "s.textContent='html,body,div,header,footer,nav,main,section,article,aside," +
                "p,span,h1,h2,h3,h4,h5,h6,a,li,ul,ol,td,th,tr,table," +
                "form,input:not([type=image]),button,select,textarea," +
                "blockquote,pre,code,label,small,strong,em,b,i" +
                "{background-color:#1c1c1e!important;color:#e0e0e0!important;" +
                "border-color:#3a3a3c!important}" +
                "img,video,canvas,picture,svg,iframe{filter:none!important}';" +
                "(document.head||document.documentElement).appendChild(s);" +
                "})()", null);
        } else {
            webView.evaluateJavascript(
                "(function(){var s=document.getElementById('_pdm');if(s)s.remove();})()", null);
        }
        if (!wasJs) webView.getSettings().setJavaScriptEnabled(false);
    }

    /* ════════════════════════════════════════════
     *  Permissions sheet
     * ════════════════════════════════════════════ */
    private void showPermissionsSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.PreviewMoreBottomSheet);
        View v = getLayoutInflater().inflate(R.layout.bottomsheet_preview_permissions, null);
        sheet.setContentView(v);
        View bg = sheet.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bg != null) bg.setBackgroundColor(Color.TRANSPARENT);

        String[] perms = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE
        };
        int[] rowIds    = { R.id.permRowCamera,    R.id.permRowMic,    R.id.permRowLocation,    R.id.permRowStorage };
        int[] statusIds = { R.id.permStatusCamera, R.id.permStatusMic, R.id.permStatusLocation, R.id.permStatusStorage };
        int[] actionIds = { R.id.permActionCamera, R.id.permActionMic, R.id.permActionLocation, R.id.permActionStorage };

        for (int i = 0; i < perms.length; i++) {
            final String perm = perms[i];
            boolean granted = ContextCompat.checkSelfPermission(this, perm)
                    == PackageManager.PERMISSION_GRANTED;

            TextView statusTv = v.findViewById(statusIds[i]);
            TextView actionTv = v.findViewById(actionIds[i]);
            statusTv.setText(granted ? "Granted" : "Not granted");
            statusTv.setTextColor(granted ? 0xFF4CAF50 : 0xFFFF5252);
            actionTv.setText(granted ? "Manage" : "Allow");

            v.findViewById(rowIds[i]).setOnClickListener(rowV -> {
                sheet.dismiss();
                if (ContextCompat.checkSelfPermission(this, perm)
                        == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.fromParts("package", getPackageName(), null));
                    startActivity(intent);
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{perm}, REQ_CODE_PERM);
                }
            });
        }

        // Downloads toggle in permissions
        SwitchCompat switchPermDownloads = v.findViewById(R.id.switchPermDownloads);
        TextView permStatusDownloads = v.findViewById(R.id.permStatusDownloads);
        if (switchPermDownloads != null) {
            switchPermDownloads.setChecked(allowDownloadEnabled);
            if (permStatusDownloads != null) {
                permStatusDownloads.setText(allowDownloadEnabled ? "Allowed" : "Disabled");
                permStatusDownloads.setTextColor(allowDownloadEnabled ? 0xFF4CAF50 : 0xFFFF5252);
            }
            ThemeHelper.applyMonochromeSwitchTint(switchPermDownloads, this, true);
            switchPermDownloads.setOnCheckedChangeListener((btn, isChecked) -> {
                allowDownloadEnabled = isChecked;
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        .edit().putBoolean(KEY_ALLOW_DOWNLOAD, isChecked).apply();
                if (permStatusDownloads != null) {
                    permStatusDownloads.setText(isChecked ? "Allowed" : "Disabled");
                    permStatusDownloads.setTextColor(isChecked ? 0xFF4CAF50 : 0xFFFF5252);
                }
                Toast.makeText(this, isChecked ? "Downloads allowed" : "Downloads disabled", Toast.LENGTH_SHORT).show();
            });
        }

        sheet.show();
    }

    /* ════════════════════════════════════════════
     *  Keyboard helpers
     * ════════════════════════════════════════════ */
    private void showKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etUrl.getWindowToken(), 0);
        }
    }

    /* ── Resolve shareable URL: typed address or loaded page URL ── */
    private String getShareableUrl() {
        if (etUrl != null) {
            String text = etUrl.getText().toString().trim();
            if (!text.isEmpty() && !text.contains("private_browser/index.html") && !text.equals("Search or type URL")) {
                return text;
            }
        }
        if (currentUrl != null && !currentUrl.isEmpty() && !currentUrl.contains("private_browser/index.html")) {
            return currentUrl;
        }
        return "";
    }

    /* ── Dismiss keyboard when tapping outside the URL bar ── */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN && etUrl.hasFocus()) {
            float x = ev.getRawX(), y = ev.getRawY();
            if (isTouchInside(focusActionsBar, x, y) || isTouchInside(findViewById(R.id.urlBox), x, y)) {
                // Inside urlBox or focusActionsBar (copy/share) - keep focus
            } else {
                etUrl.clearFocus();
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private boolean isTouchInside(View view, float x, float y) {
        if (view == null || view.getVisibility() != View.VISIBLE) return false;
        int[] loc = new int[2];
        view.getLocationOnScreen(loc);
        int slop = (int) (8 * getResources().getDisplayMetrics().density);
        return x >= (loc[0] - slop) && x <= (loc[0] + view.getWidth() + slop)
                && y >= (loc[1] - slop) && y <= (loc[1] + view.getHeight() + slop);
    }

    /* ── Scale-down on press, scale-up on release ── */
    @SuppressLint("ClickableViewAccessibility")
    private void applyPressScale(View v) {
        v.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    view.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    break;
            }
            return false; // pass through to click listeners
        });
    }

    /* ════════════════════════════════════════════
     *  Clear ALL data
     * ════════════════════════════════════════════ */
    private void clearAllData() {
        if (webView != null) {
            webView.stopLoading();
            webView.clearCache(true);
            webView.clearFormData();
            webView.clearHistory();
            webView.clearSslPreferences();
            webView.clearMatches();
        }

        CookieManager cm = CookieManager.getInstance();
        cm.removeAllCookies(null);
        cm.removeSessionCookies(null);
        cm.flush();

        WebStorage.getInstance().deleteAllData();

        WebViewDatabase wdb = WebViewDatabase.getInstance(this);
        wdb.clearFormData();
        wdb.clearHttpAuthUsernamePassword();
    }

    /* ════════════════════════════════════════════
     *  Clear EVERYTHING on exit
     * ════════════════════════════════════════════ */
    @Override
    protected void onDestroy() {
        clearAllData();
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.stay, R.anim.slide_down);
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        // Close find bar first
        if (findBar != null && findBar.getVisibility() == View.VISIBLE) {
            closeFindBar();
            return;
        }
        // If URL bar is focused, just clear focus first
        if (etUrl.hasFocus()) {
            etUrl.clearFocus();
            return;
        }
        if (errorOverlay != null && errorOverlay.getVisibility() == View.VISIBLE) {
            errorOverlay.setVisibility(View.GONE);
            if (webView != null && webView.canGoBack()) {
                webView.goBack();
                return;
            } else {
                clearAllData();
                Toast.makeText(this, "All data cleared", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            clearAllData();
            Toast.makeText(this, "All data cleared", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
