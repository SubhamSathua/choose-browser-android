package com.hyper.choosebrowsernew.ui.common;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.hyper.choosebrowsernew.AppConstantsDetails;
import com.hyper.choosebrowsernew.R;
import com.hyper.choosebrowsernew.UpdateChecker;

/**
 * Shared UI helpers for showing update-related bottom sheets and kill-switch locks.
 */
public class UpdateUiHelper {

    /**
     * Applies standard styling to the update card based on priority.
     */
    public static void applyUpdateCardStyle(AppCompatActivity activity, View innerLayout, 
                                            View dotView, TextView titleTv, TextView msgTv, 
                                            UpdateChecker.Priority priority) {
        int bgRes, dotRes, titleCol, msgCol;

        switch (priority) {
            case KILL_SWITCH:
            case CRITICAL:
                bgRes = R.drawable.bg_update_card_critical;
                dotRes = R.drawable.dot_critical;
                titleCol = activity.getResources().getColor(R.color.updateCritical_title);
                msgCol = activity.getResources().getColor(R.color.updateCritical_msg);
                break;
            case WARNING:
                bgRes = R.drawable.bg_update_card_warning;
                dotRes = R.drawable.dot_warning;
                titleCol = activity.getResources().getColor(R.color.updateWarning_title);
                msgCol = activity.getResources().getColor(R.color.updateWarning_msg);
                break;
            case LATEST:
            default:
                bgRes = R.drawable.bg_update_card_latest;
                dotRes = R.drawable.dot_blue;
                titleCol = activity.getResources().getColor(R.color.updateLatest_title);
                msgCol = activity.getResources().getColor(R.color.updateLatest_msg);
                break;
        }

        innerLayout.setBackgroundResource(bgRes);
        dotView.setBackgroundResource(dotRes);
        titleTv.setTextColor(titleCol);
        msgTv.setTextColor(msgCol);

        // Tint any icon (like forward arrow) in the card to title color
        if (innerLayout instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) innerLayout;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View v = vg.getChildAt(i);
                if (v instanceof ImageView) {
                    ((ImageView) v).setColorFilter(titleCol);
                }
            }
        }
    }

    /**
     * Shows either the kill-switch blocking screen or update info sheet.
     */
    public static void showInfoSheet(AppCompatActivity activity, UpdateChecker.Result result) {
        if (result == null) return;

        if (result.priority == UpdateChecker.Priority.KILL_SWITCH) {
            showKillSwitchSheet(activity, result);
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(activity, R.style.PreviewMoreBottomSheet);
        View sheet = activity.getLayoutInflater().inflate(R.layout.common_sheet_update_info, null);
        dialog.setContentView(sheet);

        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.setBackgroundResource(android.R.color.transparent);
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setDraggable(false); // Disable swipe to dismiss
        }

        TextView tvTitle  = sheet.findViewById(R.id.updateInfoTitle);
        TextView tvMdFile = sheet.findViewById(R.id.updateInfoMdFile);
        Button btnUpdate  = sheet.findViewById(R.id.updateInfoBtn);
        Button btnClose   = sheet.findViewById(R.id.updateInfoCloseBtn);

        String displayTitle = (result.title != null && !result.title.isEmpty()) ? result.title : "Update Available";
        tvTitle.setText(displayTitle);
        tvTitle.setTextColor(activity.getResources().getColor(R.color.text));

        if (result.mdFileUrl != null && !result.mdFileUrl.isEmpty()) {
            tvMdFile.setVisibility(View.VISIBLE);
            tvMdFile.setText("What's New >");
            tvMdFile.setOnClickListener(v -> {
                String mdUrl;
                if (result.mdFileUrl.startsWith("http://") || result.mdFileUrl.startsWith("https://")) {
                    mdUrl = result.mdFileUrl;
                } else {
                    String baseUrl = AppConstantsDetails.UPDATE_JSON_URL;
                    mdUrl = baseUrl.replace("update.json", result.mdFileUrl);
                }
                showMarkdownPopup(activity, mdUrl);
            });
        } else {
            tvMdFile.setVisibility(View.GONE);
        }

        if (result.isBlocking || result.priority == UpdateChecker.Priority.CRITICAL) {
            // Mandatory update: No close, cannot dismiss
            btnClose.setVisibility(View.GONE);
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
        } else {
            btnClose.setVisibility(View.VISIBLE);
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        btnUpdate.setOnClickListener(v -> {
            dialog.setOnDismissListener(d -> showStoresSheet(activity, result));
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Emergency Kill-Switch screen: completely locks application actions.
     */
    public static void showKillSwitchSheet(AppCompatActivity activity, UpdateChecker.Result result) {
        if (result == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(activity, R.style.PreviewMoreBottomSheet);
        View sheet = activity.getLayoutInflater().inflate(R.layout.common_sheet_update_info, null);
        dialog.setContentView(sheet);

        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.setBackgroundResource(android.R.color.transparent);
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setDraggable(false);
        }

        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        TextView tvTitle  = sheet.findViewById(R.id.updateInfoTitle);
        TextView tvStatus = sheet.findViewById(R.id.updateInfoMdFile);
        Button btnAction  = sheet.findViewById(R.id.updateInfoBtn);
        Button btnClose   = sheet.findViewById(R.id.updateInfoCloseBtn);

        String title = (result.title != null && !result.title.isEmpty()) ? result.title : "Service Temporarily Suspended";
        String msg = (result.shortMsg != null && !result.shortMsg.isEmpty()) ? result.shortMsg : "Choose Browser operations are temporarily paused for maintenance.";

        tvTitle.setText(title + "\n\n" + msg);
        tvTitle.setTextColor(activity.getResources().getColor(R.color.updateCritical_title));

        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText("Check Live Status >");
        tvStatus.setOnClickListener(v -> openUrl(activity, AppConstantsDetails.UPDATE_HUB_URL));

        btnClose.setVisibility(View.GONE);
        btnAction.setText("Exit App");
        btnAction.setOnClickListener(v -> activity.finishAffinity());

        dialog.show();
    }

    /**
     * Renders markdown in a bottom sheet viewer.
     */
    public static void showMarkdownPopup(AppCompatActivity activity, String url) {
        BottomSheetDialog dialog = new BottomSheetDialog(activity, R.style.PreviewMoreBottomSheet);
        View sheet = activity.getLayoutInflater().inflate(R.layout.common_sheet_markdown, null);
        dialog.setContentView(sheet);

        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.setBackgroundResource(android.R.color.transparent);
            bottomSheet.getLayoutParams().height = (int) (activity.getResources().getDisplayMetrics().heightPixels * 0.85);
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setDraggable(false);
        }

        View closeBtn = sheet.findViewById(R.id.markdownClose);
        if (closeBtn != null) closeBtn.setOnClickListener(v -> dialog.dismiss());

        WebView webView = sheet.findViewById(R.id.markdownWebView);
        ProgressBar loader = sheet.findViewById(R.id.markdownLoader);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setBackgroundColor(Color.TRANSPARENT);

        boolean isDark = (activity.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) 
                          == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        String bodyTextCol = isDark ? "#E0E0E0" : "#212121";
        String h1TextCol = isDark ? "#FFFFFF" : "#000000";
        String codeBg = isDark ? "#2A2D35" : "#F0F0F0";
        String codeText = isDark ? "#FFD700" : "#D32F2F";

        String htmlTemplate = 
            "<!DOCTYPE html><html><head>" +
            "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "<script src='https://cdn.jsdelivr.net/npm/marked/marked.min.js'></script>" +
            "<style>" +
            "  @font-face { font-family: 'Lora'; src: url('file:///android_res/font/lora_variablefont_wght.ttf'); }" +
            "  @font-face { font-family: 'Poppins'; src: url('file:///android_res/font/poppins_medium.ttf'); }" +
            "  body { " +
            "    background-color: transparent; color: " + bodyTextCol + "; " +
            "    font-family: 'Lora', serif; line-height: 1.6; " +
            "    padding: 10px 24px 60px 24px; margin: 0; " +
            "    font-size: 18px; " +
            "    animation: fadeIn 0.6s cubic-bezier(0.22, 1, 0.36, 1); " +
            "  }" +
            "  @keyframes fadeIn { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }" +
            "  h1, h2, h3 { font-family: 'Poppins', sans-serif; color: " + h1TextCol + "; margin-top: 1.4em; font-weight: 600; }" +
            "  h1 { border-bottom: 2px solid #377aff; padding-bottom: 0.4em; font-size: 24px; letter-spacing: -0.02em; }" +
            "  h2 { font-size: 20px; color: #377aff; border-left: 4px solid #377aff; padding-left: 12px; }" +
            "  code { background: " + codeBg + "; padding: 3px 6px; border-radius: 6px; font-family: monospace; font-size: 0.85em; color: " + codeText + "; }" +
            "  pre { background: " + (isDark ? "#1A1B1F" : "#F5F5F5") + "; padding: 18px; border-radius: 16px; overflow-x: auto; border: 1px solid " + (isDark ? "#333" : "#DDD") + "; margin: 20px 0; }" +
            "  pre code { background: transparent; padding: 0; color: " + bodyTextCol + "; }" +
            "  blockquote { border-left: 4px solid #377aff; padding: 2px 0 2px 20px; color: #B0B3B8; font-style: italic; margin: 24px 0; background: " + (isDark ? "#1A1B1F" : "#F9F9F9") + "; border-radius: 0 12px 12px 0; }" +
            "  ul, ol { padding-left: 24px; }" +
            "  li { margin-bottom: 12px; }" +
            "  hr { border: 0; border-top: 1px solid " + (isDark ? "#333" : "#EEE") + "; margin: 30px 0; }" +
            "  a { color: #377aff; text-decoration: none; font-weight: 600; }" +
            "  img { max-width: 100%; border-radius: 12px; margin: 10px 0; }" +
            "  ::selection { background: #377aff; color: white; }" +
            "</style></head><body>" +
            "<div id='content'><div style='text-align:center; padding-top:100px;'><div style='color:#377aff; font-size:20px; font-family:Poppins;'>Loading...</div></div></div>" +
            "<script>" +
            "  fetch('" + url + "')" +
            "    .then(response => response.text())" +
            "    .then(text => {" +
            "      document.getElementById('content').innerHTML = marked.parse(text);" +
            "    })" +
            "    .catch(err => {" +
            "      document.getElementById('content').innerHTML = '<div style=\"text-align:center; padding-top:100px; color:#ff4444; font-family:Poppins;\"><h3>Oops!</h3><p>Could not load the update notes.</p></div>';" +
            "    });" +
            "</script></body></html>";

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                loader.setVisibility(View.GONE);
            }
        });

        webView.loadDataWithBaseURL("https://github.com", htmlTemplate, "text/html", "UTF-8", null);
        dialog.show();
    }

    /**
     * Shows the download sources bottom sheet.
     */
    public static void showStoresSheet(AppCompatActivity activity) {
        showStoresSheet(activity, null);
    }

    public static void showStoresSheet(AppCompatActivity activity, UpdateChecker.Result result) {
        BottomSheetDialog dialog = new BottomSheetDialog(activity, R.style.PreviewMoreBottomSheet);
        View sheet = activity.getLayoutInflater().inflate(R.layout.common_sheet_update_stores, null);
        dialog.setContentView(sheet);

        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) bottomSheet.setBackgroundResource(android.R.color.transparent);

        final String githubUrl = (result != null && result.githubUrl != null && !result.githubUrl.isEmpty())
                ? result.githubUrl : AppConstantsDetails.GITHUB_URL;
        final String directApk = (result != null) ? result.directApkUrl : null;
        final String obtainium = (result != null) ? result.obtainiumUrl : null;
        final String fdroid = (result != null) ? result.fdroidUrl : null;

        View vReleases = sheet.findViewById(R.id.storeGithubReleases);
        if (vReleases != null) {
            vReleases.setOnClickListener(v -> {
                String target = (directApk != null && !directApk.isEmpty()) ? directApk : (githubUrl.endsWith("/releases") ? githubUrl : (githubUrl + "/releases"));
                openUrl(activity, target);
                dialog.dismiss();
            });
        }

        View vGithub = sheet.findViewById(R.id.storeGithub);
        if (vGithub != null) {
            vGithub.setOnClickListener(v -> {
                openUrl(activity, githubUrl);
                dialog.dismiss();
            });
        }

        View vFDroid = sheet.findViewById(R.id.storeFDroid);
        if (vFDroid != null && fdroid != null && !fdroid.isEmpty()) {
            vFDroid.setVisibility(View.VISIBLE);
            vFDroid.setAlpha(1.0f);
            vFDroid.setClickable(true);
            vFDroid.setFocusable(true);
            vFDroid.setOnClickListener(v -> {
                openUrl(activity, fdroid);
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    private static void openUrl(AppCompatActivity activity, String url) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ignored) {
        }
    }
}
