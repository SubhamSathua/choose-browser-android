package com.hyper.choosebrowsernew;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class BrowserChooserActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Transparent activity, no layout required
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(android.R.color.transparent));
            getWindow().setNavigationBarColor(getResources().getColor(android.R.color.transparent));
        }

        // Handle initial intent
        handleIncomingIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIncomingIntent(intent);
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) {
            finish();
            return;
        }

        String url = null;
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_VIEW.equals(action)) {
            Uri data = intent.getData();
            if (data != null) url = data.toString();
        } else if (Intent.ACTION_SEND.equals(action) && "text/plain".equals(type)) {
            url = intent.getStringExtra(Intent.EXTRA_TEXT);
        }

        if (!TextUtils.isEmpty(url)) {
            // Show the BottomSheet popup
            BrowserChooserBottomSheet chooser = BrowserChooserBottomSheet.newInstance(url);
            chooser.show(getSupportFragmentManager(), "browser_chooser");
        } else {
            Toast.makeText(this, "No URL to open", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
