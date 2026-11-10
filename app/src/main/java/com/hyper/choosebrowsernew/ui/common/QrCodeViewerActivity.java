package com.hyper.choosebrowsernew.ui.common;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.drawable.DrawableCompat;

import com.hyper.choosebrowsernew.R;
import com.hyper.choosebrowsernew.util.MotionUiHelper;
import com.hyper.choosebrowsernew.util.ThemeHelper;
import com.hyper.choosebrowsernew.util.qr.QrBitmapGenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Full-page QR Code screen featuring:
 * - Clean top bar with close button at top
 * - Centered scannable QR Code (240dp x 240dp)
 * - URL truncated with '...' at end in Inter font (max 2 lines)
 * - Row 1: Download button (100% width = same 240dp size as QR code)
 * - Row 2: Share and Copy circular/pill buttons (50% / 50% shared width)
 * - Dynamic theme engine compliance across all 6 custom themes
 */
public class QrCodeViewerActivity extends AppCompatActivity {

    public static final String EXTRA_URL = "extra_qr_url";
    private static final String TAG = "QrCodeViewerActivity";

    private String url;
    private Bitmap qrBitmap;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ThemeHelper.applySystemBars(this);
        setContentView(R.layout.activity_qr_code_viewer);

        url = getIntent().getStringExtra(EXTRA_URL);
        if (TextUtils.isEmpty(url)) {
            url = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        }

        if (TextUtils.isEmpty(url)) {
            Toast.makeText(this, "No link to display", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        applyTheme();

        // Top bar
        ImageView closeBtn = findViewById(R.id.qrScreenCloseBtn);
        closeBtn.setOnClickListener(v -> finish());
        MotionUiHelper.applyTapScale(closeBtn);

        // URL display
        TextView urlTextView = findViewById(R.id.qrLinkText);
        urlTextView.setText(url);

        // Generate QR Code
        ImageView qrImageView = findViewById(R.id.qrImageView);
        qrBitmap = QrBitmapGenerator.generate(url);
        if (qrBitmap != null) {
            qrImageView.setImageBitmap(qrBitmap);
        }

        // Action buttons
        View downloadBtn = findViewById(R.id.qrDownloadBtn);
        View shareImageBtn = findViewById(R.id.qrShareImageBtn);
        View copyBtn = findViewById(R.id.qrCopyBtn);

        MotionUiHelper.applyTapScale(downloadBtn);
        MotionUiHelper.applyTapScale(shareImageBtn);
        MotionUiHelper.applyTapScale(copyBtn);

        // Row 1: Download button
        downloadBtn.setOnClickListener(v -> {
            if (qrBitmap != null) {
                String domain = extractDomain(url);
                String filename = "QR_" + domain.replaceAll("[^a-zA-Z0-9.-]", "_") + "_" + System.currentTimeMillis();
                boolean success = saveQrToStorage(filename);
                if (success) {
                    Toast.makeText(this, "QR code saved to Pictures", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to save QR code", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Row 2: Share button
        shareImageBtn.setOnClickListener(v -> {
            if (qrBitmap != null) {
                shareQrBitmap();
            }
        });

        // Row 2: Copy button
        copyBtn.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("URL", url));
                Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyTheme() {
        int surface = ThemeHelper.resolveThemeColor(this, R.attr.colorPopupSurface, R.color.PopUpCardBg);
        int dockBtn = ThemeHelper.resolveThemeColor(this, R.attr.colorPopupDockButton, R.color.PopUpCardDockBtn);
        int dockBtnText = ThemeHelper.resolveThemeColor(this, R.attr.colorPopupDockButtonText, R.color.PopUpCardDockText);
        int action = ThemeHelper.resolveThemeColor(this, R.attr.colorPopupAction, R.color.PopUpCard_ActionBtnBg);
        int text = ThemeHelper.resolveThemeColor(this, R.attr.colorPopupText, R.color.text);

        // Full screen root background
        View root = findViewById(R.id.qrScreenRoot);
        if (root != null) root.setBackgroundColor(surface);

        // Top bar
        tintBackground(findViewById(R.id.qrScreenCloseBtn), action);
        tintIcon(findViewById(R.id.qrScreenCloseBtn), text);

        TextView title = findViewById(R.id.qrScreenTitle);
        if (title != null) title.setTextColor(text);

        // URL text
        TextView linkText = findViewById(R.id.qrLinkText);
        if (linkText != null) {
            linkText.setTextColor(Color.argb(190, Color.red(text), Color.green(text), Color.blue(text)));
        }

        // Action buttons
        tintBackground(findViewById(R.id.qrDownloadBtn), dockBtn);
        tintBackground(findViewById(R.id.qrShareImageBtn), action);
        tintBackground(findViewById(R.id.qrCopyBtn), action);

        // Action text
        TextView downloadText = findViewById(R.id.qrDownloadText);
        if (downloadText != null) downloadText.setTextColor(dockBtnText);

        TextView shareText = findViewById(R.id.qrShareImageText);
        if (shareText != null) shareText.setTextColor(text);

        TextView copyText = findViewById(R.id.qrCopyText);
        if (copyText != null) copyText.setTextColor(text);

        // Action icons
        tintIcon(findViewById(R.id.qrDownloadIcon), dockBtnText);
        tintIcon(findViewById(R.id.qrShareImageIcon), text);
        tintIcon(findViewById(R.id.qrCopyIcon), text);
    }

    private void tintBackground(View view, int color) {
        if (view == null) return;
        android.graphics.drawable.Drawable d = view.getBackground();
        if (d != null) {
            android.graphics.drawable.Drawable mutated = DrawableCompat.wrap(d.mutate());
            DrawableCompat.setTint(mutated, color);
            view.setBackground(mutated);
        }
    }

    private void tintIcon(View view, int color) {
        if (view instanceof ImageView) {
            ((ImageView) view).setColorFilter(color);
        }
    }

    private void shareQrBitmap() {
        try {
            File dir = new File(getCacheDir(), "shared_qr");
            if (!dir.exists() && !dir.mkdirs()) {
                Log.w(TAG, "Failed to create shared_qr directory");
            }
            File file = new File(dir, "qr_share.png");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            }

            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.putExtra(Intent.EXTRA_TEXT, url);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share QR Code"));
        } catch (Exception e) {
            Log.e(TAG, "Failed to share QR image", e);
            Toast.makeText(this, "Could not share QR image", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean saveQrToStorage(String filename) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, filename + ".png");
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ChooseBrowser");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);

                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                        if (os != null) {
                            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                        }
                    }
                    values.clear();
                    values.put(MediaStore.Images.Media.IS_PENDING, 0);
                    getContentResolver().update(uri, values, null, null);
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
                    qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
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
}
