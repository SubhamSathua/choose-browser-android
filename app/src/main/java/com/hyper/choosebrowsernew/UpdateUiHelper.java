package com.hyper.choosebrowsernew;

import android.content.Intent;
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
     * Renders the markdown (or text) file in a bottom sheet viewer.
     */
    private static void showMarkdownPopup(AppCompatActivity activity, String url) {
        BottomSheetDialog dialog = new BottomSheetDialog(activity, R.style.PreviewMoreBottomSheet);
        View sheet = activity.getLayoutInflater().inflate(R.layout.bottomsheet_markdown_viewer, null);
        dialog.setContentView(sheet);

        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.setBackgroundResource(android.R.color.transparent);
            // Allow the sheet to be full screen if content is long
            bottomSheet.getLayoutParams().height = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
        }

        WebView webView = sheet.findViewById(R.id.markdownWebView);
        ProgressBar loader = sheet.findViewById(R.id.markdownLoader);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                loader.setVisibility(View.GONE);
                // Inject basic styling for raw text files to look decent in dark mode
                view.evaluateJavascript(
                    "document.body.style.backgroundColor = '#1c1c1e';" +
                    "document.body.style.color = '#ffffff';" +
                    "document.body.style.fontFamily = 'sans-serif';" +
                    "document.body.style.padding = '20px';" +
                    "document.body.style.lineHeight = '1.6';", null);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                loader.setVisibility(View.GONE);
            }
        });

        webView.loadUrl(url);
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
