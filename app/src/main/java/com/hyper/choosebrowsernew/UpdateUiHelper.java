package com.hyper.choosebrowsernew;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;

/**
 * Shared UI helpers for showing update-related bottom sheets.
 */
public class UpdateUiHelper {

    /**
     * Shows the update info sheet with short_msg, md_file note, and conditional close button.
     * For CRITICAL: no close, non-cancellable. Otherwise: close + update.
     */
    public static void showInfoSheet(AppCompatActivity activity, UpdateChecker.Result result) {
        if (result == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(activity, R.style.PreviewMoreBottomSheet);
        View sheet = activity.getLayoutInflater().inflate(R.layout.bottomsheet_update_info, null);
        dialog.setContentView(sheet);

        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) bottomSheet.setBackgroundResource(android.R.color.transparent);

        TextView tvTitle  = sheet.findViewById(R.id.updateInfoTitle);
        TextView tvMdFile = sheet.findViewById(R.id.updateInfoMdFile);
        Button btnUpdate  = sheet.findViewById(R.id.updateInfoBtn);
        Button btnClose   = sheet.findViewById(R.id.updateInfoCloseBtn);

        tvTitle.setText(result.shortMsg != null ? result.shortMsg : "Update Available");

        if (result.mdFileUrl != null && !result.mdFileUrl.isEmpty()) {
            tvMdFile.setVisibility(View.VISIBLE);
            tvMdFile.setText("What's New >");
            tvMdFile.setOnClickListener(v -> {
                // Construct MD URL: replace update.json with the md_file name
                String baseUrl = AppConstantsDetails.UPDATE_JSON_URL;
                String mdUrl = baseUrl.replace("update.json", result.mdFileUrl);
                showMarkdownPopup(activity, mdUrl);
            });
        } else {
            tvMdFile.setVisibility(View.GONE);
        }

        if (result.priority == UpdateChecker.Priority.CRITICAL) {
            // No close, cannot dismiss
            btnClose.setVisibility(View.GONE);
            dialog.setCancelable(false);
        } else {
            btnClose.setVisibility(View.VISIBLE);
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        btnUpdate.setOnClickListener(v -> {
            dialog.setOnDismissListener(d -> showStoresSheet(activity));
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Renders the markdown file in a bottom sheet viewer with premium styling.
     */
    private static void showMarkdownPopup(AppCompatActivity activity, String mdUrl) {
        BottomSheetDialog dialog = new BottomSheetDialog(activity, R.style.PreviewMoreBottomSheet);
        View sheet = activity.getLayoutInflater().inflate(R.layout.bottomsheet_markdown_viewer, null);
        dialog.setContentView(sheet);

        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.setBackgroundResource(android.R.color.transparent);
            bottomSheet.getLayoutParams().height = (int) (activity.getResources().getDisplayMetrics().heightPixels * 0.85);
        }

        View closeBtn = sheet.findViewById(R.id.markdownClose);
        if (closeBtn != null) closeBtn.setOnClickListener(v -> dialog.dismiss());

        WebView webView = sheet.findViewById(R.id.markdownWebView);
        ProgressBar loader = sheet.findViewById(R.id.markdownLoader);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setBackgroundColor(Color.TRANSPARENT);

        // "Top Notch" Markdown Renderer Template
        String htmlTemplate = 
            "<!DOCTYPE html><html><head>" +
            "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "<script src='https://cdn.jsdelivr.net/npm/marked/marked.min.js'></script>" +
            "<style>" +
            "  @font-face { font-family: 'Lora'; src: url('file:///android_res/font/lora_regular.ttf'); }" +
            "  @font-face { font-family: 'Poppins'; src: url('file:///android_res/font/poppins_medium.ttf'); }" +
            "  body { " +
            "    background-color: transparent; color: #E0E0E0; " +
            "    font-family: 'Lora', serif; line-height: 1.6; " +
            "    padding: 10px 24px 60px 24px; margin: 0; " +
            "    font-size: 18px; /* Increased readability */" +
            "    animation: fadeIn 0.6s cubic-bezier(0.22, 1, 0.36, 1); " +
            "  }" +
            "  @keyframes fadeIn { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }" +
            "  h1, h2, h3 { font-family: 'Poppins', sans-serif; color: #FFFFFF; margin-top: 1.4em; font-weight: 600; }" +
            "  h1 { border-bottom: 2px solid #377aff; padding-bottom: 0.4em; font-size: 24px; letter-spacing: -0.02em; }" +
            "  h2 { font-size: 20px; color: #377aff; border-left: 4px solid #377aff; padding-left: 12px; }" +
            "  code { background: #2A2D35; padding: 3px 6px; border-radius: 6px; font-family: monospace; font-size: 0.85em; color: #FFD700; }" +
            "  pre { background: #1A1B1F; padding: 18px; border-radius: 16px; overflow-x: auto; border: 1px solid #333; margin: 20px 0; }" +
            "  pre code { background: transparent; padding: 0; color: #E0E0E0; }" +
            "  blockquote { border-left: 4px solid #377aff; padding: 2px 0 2px 20px; color: #B0B3B8; font-style: italic; margin: 24px 0; background: #1A1B1F; border-radius: 0 12px 12px 0; }" +
            "  ul, ol { padding-left: 24px; }" +
            "  li { margin-bottom: 12px; }" +
            "  hr { border: 0; border-top: 1px solid #333; margin: 30px 0; }" +
            "  a { color: #377aff; text-decoration: none; font-weight: 600; }" +
            "  img { max-width: 100%; border-radius: 12px; margin: 10px 0; }" +
            "  ::selection { background: #377aff; color: white; }" +
            "</style></head><body>" +
            "<div id='content'><div style='text-align:center; padding-top:100px;'><div style='color:#377aff; font-size:20px; font-family:Poppins;'>Loading...</div></div></div>" +
            "<script>" +
            "  fetch('" + mdUrl + "')" +
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
     * Store rows are easy to enable/disable by toggling visibility in the layout.
     */
    public static void showStoresSheet(AppCompatActivity activity) {
        BottomSheetDialog dialog = new BottomSheetDialog(activity, R.style.PreviewMoreBottomSheet);
        View sheet = activity.getLayoutInflater().inflate(R.layout.bottomsheet_update_stores, null);
        dialog.setContentView(sheet);

        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) bottomSheet.setBackgroundResource(android.R.color.transparent);

        sheet.findViewById(R.id.storeGithubReleases).setOnClickListener(v -> {
            openUrl(activity, "https://github.com/Subham-x/Testing/releases");
            dialog.dismiss();
        });

        sheet.findViewById(R.id.storeGithub).setOnClickListener(v -> {
            openUrl(activity, "https://github.com/Subham-x/Testing");
            dialog.dismiss();
        });

        dialog.show();
    }

    private static void openUrl(AppCompatActivity activity, String url) {
        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }
}
