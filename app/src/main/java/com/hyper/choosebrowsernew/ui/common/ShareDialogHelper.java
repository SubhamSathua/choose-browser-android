package com.hyper.choosebrowsernew.ui.common;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.hyper.choosebrowsernew.R;
import com.hyper.choosebrowsernew.util.LinkCleaner;
import com.hyper.choosebrowsernew.util.MotionUiHelper;
import com.hyper.choosebrowsernew.util.ThemeHelper;
import com.hyper.choosebrowsernew.util.qr.QrBitmapGenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Locale;

/**
 * Chrome-style Share Menu dialog featuring a live on-device QR Code generator,
 * Clean & Share, Copy Link, and Android system chooser sharing.
 */
public class ShareDialogHelper {

    private static final String TAG = "ShareDialogHelper";

    public static void show(Context context, String url) {
        show(context, url, false);
    }

    public static void showQr(Context context, String url) {
        show(context, url, true);
    }

    public static void show(Context context, String rawUrl, boolean startInQrMode) {
        if (context == null) return;
        final String url = rawUrl != null ? rawUrl.trim() : "";
        if (TextUtils.isEmpty(url)) {
            Toast.makeText(context, "No link to share", Toast.LENGTH_SHORT).show();
            return;
        }

        Context themedContext = ThemeHelper.wrapWithColorThemeOverlay(context);
        Dialog dialog = new Dialog(themedContext);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);

        View view = LayoutInflater.from(themedContext).inflate(R.layout.chooser_dialog_share, null);
        dialog.setContentView(view);

        applyTheme(themedContext, view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setDimAmount(0.55f);
        }

        // Header views
        ImageView backBtn = view.findViewById(R.id.shareBackBtn);
        ImageView iconView = view.findViewById(R.id.shareIcon);
        TextView titleText = view.findViewById(R.id.shareTitle);
        TextView subtitleText = view.findViewById(R.id.shareSubtitle);
        ImageView closeBtn = view.findViewById(R.id.shareCloseBtn);

        // Mode containers
        View optionsMode = view.findViewById(R.id.shareOptionsMode);
        View qrMode = view.findViewById(R.id.shareQrMode);

        // Share Options views
        TextView previewDomain = view.findViewById(R.id.sharePreviewDomain);
        TextView previewUrl = view.findViewById(R.id.sharePreviewUrl);
        View actionQrCode = view.findViewById(R.id.actionQrCode);
        View actionCopyLink = view.findViewById(R.id.actionCopyLink);
        View actionCleanShare = view.findViewById(R.id.actionCleanShare);
        View actionMoreApps = view.findViewById(R.id.actionMoreApps);

        // QR Mode views
        ImageView qrImageView = view.findViewById(R.id.qrImageView);
        TextView qrLinkText = view.findViewById(R.id.qrLinkText);
        View qrDownloadBtn = view.findViewById(R.id.qrDownloadBtn);
        View qrShareImageBtn = view.findViewById(R.id.qrShareImageBtn);
        ImageView qrCopyBtn = view.findViewById(R.id.qrCopyBtn);

        // Setup domain and char count
        String domain = extractDomain(url);
        int chars = url.length();
        String subtitle = domain + "  •  " + chars + (chars == 1 ? " char" : " chars");
        subtitleText.setText(subtitle);
        previewDomain.setText(domain);
        previewUrl.setText(url);
        qrLinkText.setText(url);

        // Micro-interactions
        MotionUiHelper.applyTapScale(backBtn);
        MotionUiHelper.applyTapScale(closeBtn);
        MotionUiHelper.applyTapScale(actionQrCode);
        MotionUiHelper.applyTapScale(actionCopyLink);
        MotionUiHelper.applyTapScale(actionCleanShare);
        MotionUiHelper.applyTapScale(actionMoreApps);
        MotionUiHelper.applyTapScale(qrDownloadBtn);
        MotionUiHelper.applyTapScale(qrShareImageBtn);
        MotionUiHelper.applyTapScale(qrCopyBtn);

        // Mode switching logic
        Runnable showQrView = () -> {
            titleText.setText("QR Code");
            iconView.setImageResource(R.drawable.ic_qr_code_24);
            backBtn.setVisibility(View.VISIBLE);
            optionsMode.setVisibility(View.GONE);
            qrMode.setVisibility(View.VISIBLE);

            // Lazy generate QR code on display
            Bitmap qrBitmap = QrBitmapGenerator.generate(url);
            if (qrBitmap != null) {
                qrImageView.setImageBitmap(qrBitmap);
            }
        };

        Runnable showOptionsView = () -> {
            titleText.setText("Share Link");
            iconView.setImageResource(R.drawable.ix_share);
            backBtn.setVisibility(View.GONE);
            optionsMode.setVisibility(View.VISIBLE);
            qrMode.setVisibility(View.GONE);
        };

        if (startInQrMode) {
            showQrView.run();
        } else {
            showOptionsView.run();
        }

        // Header actions
        backBtn.setOnClickListener(v -> showOptionsView.run());
        closeBtn.setOnClickListener(v -> dialog.dismiss());

        // Mode 1: Option clicks
        actionQrCode.setOnClickListener(v -> showQrView.run());

        actionCopyLink.setOnClickListener(v -> {
            copyToClipboard(context, url);
            dialog.dismiss();
        });

        actionCleanShare.setOnClickListener(v -> {
            String cleaned = LinkCleaner.clean(themedContext, url);
            shareTextViaSystem(context, cleaned, "Share Clean Link");
            dialog.dismiss();
        });

        actionMoreApps.setOnClickListener(v -> {
            shareTextViaSystem(context, url, "Share URL");
            dialog.dismiss();
        });

        // Mode 2: QR view clicks
        qrDownloadBtn.setOnClickListener(v -> {
            Bitmap qrBitmap = QrBitmapGenerator.generate(url);
            if (qrBitmap != null) {
                String filename = "QR_" + domain.replaceAll("[^a-zA-Z0-9.-]", "_") + "_" + System.currentTimeMillis();
                boolean success = saveQrToStorage(context, qrBitmap, filename);
                if (success) {
                    Toast.makeText(context, "QR code saved to Pictures", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Failed to save QR code", Toast.LENGTH_SHORT).show();
                }
            }
        });

        qrShareImageBtn.setOnClickListener(v -> {
            Bitmap qrBitmap = QrBitmapGenerator.generate(url);
            if (qrBitmap != null) {
                shareQrBitmap(context, qrBitmap, url);
                dialog.dismiss();
            }
        });

        qrCopyBtn.setOnClickListener(v -> copyToClipboard(context, url));

        dialog.show();
    }

    private static void copyToClipboard(Context context, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("URL", text));
            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    private static void shareTextViaSystem(Context context, String text, String title) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, text);
        context.startActivity(Intent.createChooser(share, title));
    }

    private static void shareQrBitmap(Context context, Bitmap bitmap, String url) {
        try {
            File dir = new File(context.getCacheDir(), "shared_qr");
            if (!dir.exists() && !dir.mkdirs()) {
                Log.w(TAG, "Failed to create shared_qr directory");
            }
            File file = new File(dir, "qr_share.png");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            }

            Uri uri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    file
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.putExtra(Intent.EXTRA_TEXT, url);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(shareIntent, "Share QR Code"));
        } catch (Exception e) {
            Log.e(TAG, "Failed to share QR image", e);
            Toast.makeText(context, "Could not share QR image", Toast.LENGTH_SHORT).show();
        }
    }

    private static boolean saveQrToStorage(Context context, Bitmap bitmap, String filename) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, filename + ".png");
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ChooseBrowser");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);

                Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
                        if (os != null) {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                        }
                    }
                    values.clear();
                    values.put(MediaStore.Images.Media.IS_PENDING, 0);
                    context.getContentResolver().update(uri, values, null, null);
                    return true;
                }
            } else {
                File pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File dir = new File(pictures, "ChooseBrowser");
                if (!dir.exists() && !dir.mkdirs()) {
                    Log.w(TAG, "Failed to create picture directory");
                }
                File file = new File(dir, filename + ".png");
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Save QR failed", e);
        }
        return false;
    }

    private static String extractDomain(String url) {
        if (TextUtils.isEmpty(url)) return "Link";
        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            if (host != null) {
                return host.startsWith("www.") ? host.substring(4) : host;
            }
        } catch (Exception ignored) {}
        return "Link";
    }

    private static void applyTheme(Context context, View root) {
        int surface = ThemeHelper.resolveThemeColor(context, R.attr.colorPopupSurface, R.color.PopUpCardBg);
        int dock = ThemeHelper.resolveThemeColor(context, R.attr.colorPopupDock, R.color.PopUpCardDockBg);
        int dockBtn = ThemeHelper.resolveThemeColor(context, R.attr.colorPopupDockButton, R.color.PopUpCardDockBtn);
        int dockBtnText = ThemeHelper.resolveThemeColor(context, R.attr.colorPopupDockButtonText, R.color.PopUpCardDockText);
        int action = ThemeHelper.resolveThemeColor(context, R.attr.colorPopupAction, R.color.PopUpCard_ActionBtnBg);
        int text = ThemeHelper.resolveThemeColor(context, R.attr.colorPopupText, R.color.text);

        // Root
        tintBackground(root, surface);

        // Header
        tintBackground(root.findViewById(R.id.shareIconContainer), action);
        tintBackground(root.findViewById(R.id.shareBackBtn), action);
        tintBackground(root.findViewById(R.id.shareCloseBtn), action);

        // Options mode cards
        tintBackground(root.findViewById(R.id.shareUrlPreviewCard), dock);
        tintBackground(root.findViewById(R.id.actionQrCode), dock);
        tintBackground(root.findViewById(R.id.actionCopyLink), dock);
        tintBackground(root.findViewById(R.id.actionCleanShare), dock);
        tintBackground(root.findViewById(R.id.actionMoreApps), dock);

        // QR Mode buttons
        tintBackground(root.findViewById(R.id.qrDownloadBtn), dockBtn);
        tintBackground(root.findViewById(R.id.qrShareImageBtn), action);
        tintBackground(root.findViewById(R.id.qrCopyBtn), action);

        // Text colors
        TextView title = root.findViewById(R.id.shareTitle);
        if (title != null) title.setTextColor(text);

        TextView subtitle = root.findViewById(R.id.shareSubtitle);
        if (subtitle != null) {
            subtitle.setTextColor(Color.argb(190, Color.red(text), Color.green(text), Color.blue(text)));
        }

        TextView previewDomain = root.findViewById(R.id.sharePreviewDomain);
        if (previewDomain != null) previewDomain.setTextColor(text);

        TextView previewUrl = root.findViewById(R.id.sharePreviewUrl);
        if (previewUrl != null) {
            previewUrl.setTextColor(Color.argb(190, Color.red(text), Color.green(text), Color.blue(text)));
        }

        // Action titles and descs
        int subtextColor = Color.argb(160, Color.red(text), Color.green(text), Color.blue(text));
        tintText(root, R.id.actionQrTitle, text);
        tintText(root, R.id.actionQrDesc, subtextColor);
        tintText(root, R.id.actionCopyTitle, text);
        tintText(root, R.id.actionCopyDesc, subtextColor);
        tintText(root, R.id.actionCleanTitle, text);
        tintText(root, R.id.actionCleanDesc, subtextColor);
        tintText(root, R.id.actionMoreTitle, text);
        tintText(root, R.id.actionMoreDesc, subtextColor);

        tintText(root, R.id.qrLinkText, subtextColor);
        tintText(root, R.id.qrDownloadText, dockBtnText);
        tintText(root, R.id.qrShareImageText, text);

        // Icons tint
        tintIcon(root, R.id.shareBackBtn, text);
        tintIcon(root, R.id.shareIcon, text);
        tintIcon(root, R.id.shareCloseBtn, text);
        tintIcon(root, R.id.actionQrIcon, text);
        tintIcon(root, R.id.actionCopyIcon, text);
        tintIcon(root, R.id.actionCleanIcon, text);
        tintIcon(root, R.id.actionMoreIcon, text);
        tintIcon(root, R.id.qrDownloadIcon, dockBtnText);
        tintIcon(root, R.id.qrShareImageIcon, text);
        tintIcon(root, R.id.qrCopyBtn, text);
    }

    private static void tintBackground(View view, int color) {
        if (view == null) return;
        android.graphics.drawable.Drawable d = view.getBackground();
        if (d != null) {
            android.graphics.drawable.Drawable mutated = androidx.core.graphics.drawable.DrawableCompat.wrap(d.mutate());
            androidx.core.graphics.drawable.DrawableCompat.setTint(mutated, color);
            view.setBackground(mutated);
        }
    }

    private static void tintText(View root, int viewId, int color) {
        TextView tv = root.findViewById(viewId);
        if (tv != null) tv.setTextColor(color);
    }

    private static void tintIcon(View root, int viewId, int color) {
        ImageView iv = root.findViewById(viewId);
        if (iv != null) iv.setColorFilter(color);
    }
}
