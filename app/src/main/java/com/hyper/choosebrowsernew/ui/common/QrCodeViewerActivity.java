package com.hyper.choosebrowsernew.ui.common;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Transparent overlay activity launched when the user taps "QR Code"
 * in Android's system share sheet (via ChooserAction).
 *
 * Immediately displays the scannable QR code dialog and finishes when dismissed.
 */
public class QrCodeViewerActivity extends AppCompatActivity {

    public static final String EXTRA_URL = "extra_qr_url";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String url = getIntent().getStringExtra(EXTRA_URL);
        if (TextUtils.isEmpty(url)) {
            url = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        }

        if (TextUtils.isEmpty(url)) {
            finish();
            return;
        }

        ShareDialogHelper.showQr(this, url, this::finish);
    }
}
