package com.hyper.choosebrowsernew.ui.preview;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
import com.hyper.choosebrowsernew.ui.common.ViewModelFactory;
import com.hyper.choosebrowsernew.ui.main.MainActivity;
import com.hyper.choosebrowsernew.R;
import com.hyper.choosebrowsernew.UpdateChecker;
import com.hyper.choosebrowsernew.ui.webview.WebViewActivity;
import com.hyper.choosebrowsernew.util.ThemeHelper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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
    private ImageView urlSiteIcon;
    private View findBar;
    private EditText etFind;
    private TextView tvFindCount;

    private PreviewViewModel viewModel;
    private boolean darkModeEnabled = false;

    private static final String PREFS_NAME = "preview_prefs";
    private static final String KEY_ALLOW_JS = "allow_js";
    private static final String KEY_AD_BLOCK_MODE = "ad_block_mode";
    private static final int REQ_CODE_PERM = 2001;

    private boolean adBlockEnabled = true;
    private boolean desktopSiteEnabled = false;
    private boolean screenshotAllowed = false;
    private String currentUrl = "";
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
        focusActionsBar = findViewById(R.id.focusActionsBar);
        urlSiteIcon     = findViewById(R.id.urlSiteIcon);
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

        String url = getIntent().getStringExtra("url");
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "No URL provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUrl = url;

        // Observe ViewModel state
        viewModel.jsEnabled.observe(this, enabled -> {
            if (webView != null) {
                webView.getSettings().setJavaScriptEnabled(enabled);
                btnToggleJs.setImageResource(enabled ? R.drawable.js_on : R.drawable.js_off);
            }
        });

        configureIncognitoWebView();
        etUrl.setText(url);

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
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("URL", currentUrl));
                Toast.makeText(this, "Link copied", Toast.LENGTH_SHORT).show();
            }
            etUrl.clearFocus();
        });

        // Focus-bar Share
        focusShare.setOnClickListener(v -> {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, currentUrl);
            startActivity(Intent.createChooser(share, "Share URL"));
            etUrl.clearFocus();
        });

        // ── Address bar buttons ──
        btnReload.setOnClickListener(v -> webView.reload());

        btnToggleJs.setOnClickListener(v -> {
            viewModel.toggleJs();
            Toast.makeText(this,
                    (viewModel.jsEnabled.getValue() != null && viewModel.jsEnabled.getValue())
                            ? "JavaScript enabled (temporary)" : "JavaScript disabled (temporary)",
                    Toast.LENGTH_SHORT).show();
            webView.reload();
        });

        btnClose.setOnClickListener(v -> {
            clearAllData();
            finish();
        });

        // ── Bottom nav buttons ──
        btnBack.setOnClickListener(v -> {
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
            Toast.makeText(this, "Data cleared", Toast.LENGTH_SHORT).show();
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

        // Load page
        webView.loadUrl(url);
    }

    /* ════════════════════════════════════════════
     *  Navigate to URL or DuckDuckGo search
     * ════════════════════════════════════════════ */
    private void navigateToInput(String input) {
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

        // Cookies – fully disabled
        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(false);
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
                if (icon != null) {
                    urlSiteIcon.setImageBitmap(icon);
                } else {
                    urlSiteIcon.setImageResource(R.drawable.site_icon_demo);
                }
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
                if (!etUrl.hasFocus()) {
                    etUrl.setText(url);
                }
                updateNavButtons();
                viewModel.resetBlockedCount();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                currentUrl = url;
                if (!etUrl.hasFocus()) {
                    etUrl.setText(url);
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
                if (adBlockEnabled) {
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
                handler.cancel();
                loadErrorPage(error.getUrl(), "SSL_ERROR", "SSL certificate error – page blocked for safety.");
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    return false;
                }
                return true; // block intent://, market://, tel:, mailto:, etc.
            }
        });

        // Disable download
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) ->
                Toast.makeText(this, "Downloads disabled in preview mode", Toast.LENGTH_SHORT).show());
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
     *  Load custom error page from assets
     * ════════════════════════════════════════════ */
    private void loadErrorPage(String failedUrl, String errorCode, String errorDesc) {
        try {
            InputStream is = getAssets().open("preview_error.html");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String html = new String(buffer, "UTF-8");
            String injection = "<script>"
                    + "document.getElementById('errorCode').textContent='" + escapeJs(errorCode) + "';"
                    + "document.getElementById('errorDesc').textContent='" + escapeJs(errorDesc) + "';"
                    + "</script>";
            html = html.replace("</body>", injection + "</body>");
            webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", failedUrl);
        } catch (Exception e) {
            webView.loadData("<h2 style='color:#fff;font-family:sans-serif;text-align:center;margin-top:40vh'>"
                    + errorCode + "<br><small>" + errorDesc + "</small></h2>",
                    "text/html", "UTF-8");
        }
    }

    private String escapeJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n");
    }

    /* ── Update back/forward button alpha ── */
    private void updateNavButtons() {
        btnBack.setAlpha(webView.canGoBack() ? 1.0f : 0.4f);
        btnForward.setAlpha(webView.canGoForward() ? 1.0f : 0.4f);
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

        // ── Resolve theme-aware colors ──
        Context themedContext = ThemeHelper.wrapWithColorThemeOverlay(this);
        int surface = ThemeHelper.resolveThemeColor(themedContext, R.attr.colorPopupSurface, R.color.previewPage_primary);
        int dock    = ThemeHelper.resolveThemeColor(themedContext, R.attr.colorPopupDock, R.color.previewPage_secondary);
        int text    = ThemeHelper.resolveThemeColor(themedContext, R.attr.colorPopupText, R.color.previewPage_textSecondary);

        // Apply background drawable tint
        if (view.getBackground() != null) {
            view.getBackground().mutate().setTint(surface);
        }

        // ── Site title capsule ──
        TextView siteTitle = view.findViewById(R.id.siteTitle);
        ImageView siteIcon  = view.findViewById(R.id.siteIcon);
        View siteInfoCard = view.findViewById(R.id.siteInfoCard);
        if (siteInfoCard != null && siteInfoCard.getBackground() != null) {
            siteInfoCard.getBackground().mutate().setTint(dock);
        }
        siteTitle.setTextColor(text);

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

        // Copy/Reload buttons
        ImageButton copyBtn = view.findViewById(R.id.moreCopyLinkBtn);
        ImageButton reloadBtn = view.findViewById(R.id.moreReloadBtn);
        copyBtn.setColorFilter(text);
        reloadBtn.setColorFilter(text);

        copyBtn.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("URL", currentUrl));
                Toast.makeText(this, "Link copied", Toast.LENGTH_SHORT).show();
            }
        });

        reloadBtn.setOnClickListener(v -> {
            webView.reload();
            dialog.dismiss();
        });

        // ── Grid Row 1 ──
        View gridCard = view.findViewById(R.id.bg_grid_card);
        if (gridCard != null && gridCard.getBackground() != null) {
            gridCard.getBackground().mutate().setTint(dock);
        }

        // ── Grid row items ──
        int[] itemIds = {R.id.moreOpenIn, R.id.moreHome, R.id.morePrint, R.id.moreFind, 
                         R.id.moreShare, R.id.moreAdBlock, R.id.morePermissions, R.id.moreDarkMode};
        
        for (int id : itemIds) {
            View item = view.findViewById(id);
            if (item instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) item;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    View child = vg.getChildAt(i);
                    if (child instanceof ImageView) {
                        ((ImageView) child).setColorFilter(text);
                    } else if (child instanceof TextView) {
                        ((TextView) child).setTextColor(text);
                    }
                }
            }
        }

        view.findViewById(R.id.moreOpenIn).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try { startActivity(intent); } catch (Exception e) {
                Toast.makeText(this, "No browser found", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        view.findViewById(R.id.moreHome).setOnClickListener(v -> {
            webView.loadUrl("file:///android_asset/home.html");
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
            webView.reload();
        });

        // Permissions page
        view.findViewById(R.id.morePermissions).setOnClickListener(v -> {
            dialog.dismiss();
            showPermissionsSheet();
        });

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

        // ── Toggles section ──
        // ... (remaining toggle logic)

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

        // Allow JS (persistent toggle)
        SwitchCompat switchAllowJs = view.findViewById(R.id.switchAllowJs);
        Boolean currentJs = viewModel.jsEnabled.getValue();
        switchAllowJs.setChecked(currentJs != null && currentJs);
        switchAllowJs.setOnCheckedChangeListener((btn, isChecked) -> {
            viewModel.setPersistentJs(isChecked);
            Toast.makeText(this,
                    isChecked ? "JavaScript enabled — will persist on next load"
                              : "JavaScript disabled — will persist on next load",
                    Toast.LENGTH_SHORT).show();
            webView.reload();
        });

        // ── Style all switches with Windows blue ──
        ColorStateList thumbTint = new ColorStateList(
                new int[][]{ new int[]{android.R.attr.state_checked}, new int[]{} },
                new int[]{ 0xFF0078D4, 0xFFAAAAAA });
        ColorStateList trackTint = new ColorStateList(
                new int[][]{ new int[]{android.R.attr.state_checked}, new int[]{} },
                new int[]{ 0x550078D4, 0x33888888 });
        for (SwitchCompat sw : new SwitchCompat[]{ switchDesktop, switchScreenshot, switchAllowJs }) {
            sw.setThumbTintList(thumbTint);
            sw.setTrackTintList(trackTint);
        }

        // Ad-Blocker Mode row (below Allow JavaScript switch)
        TextView tvAdBlockMode = view.findViewById(R.id.tvAdBlockMode);
        String currentMode = viewModel.adBlockMode.getValue();
        tvAdBlockMode.setText("dns".equals(currentMode) ? "AdGuard DNS" : "Domain Filter List");
        tvAdBlockMode.setTextColor(text);
        tvAdBlockMode.setAlpha(0.7f); // Slightly subtle

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

    /* ── Dismiss keyboard when tapping outside the URL bar ── */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN && etUrl.hasFocus()) {
            View urlBox = findViewById(R.id.urlBox);
            if (urlBox != null) {
                int[] loc = new int[2];
                urlBox.getLocationOnScreen(loc);
                float x = ev.getRawX(), y = ev.getRawY();
                if (x < loc[0] || x > loc[0] + urlBox.getWidth()
                        || y < loc[1] || y > loc[1] + urlBox.getHeight()) {
                    etUrl.clearFocus();
                }
            }
        }
        return super.dispatchTouchEvent(ev);
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
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            clearAllData();
            finish();
        }
    }
}
