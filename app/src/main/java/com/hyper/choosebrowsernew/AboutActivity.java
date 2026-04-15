package com.hyper.choosebrowsernew;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Status bar matches the dark hero; nav bar matches page background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.previewPage_primary));
            getWindow().setNavigationBarColor(getResources().getColor(R.color.backgroundPrimary));
        }

        setContentView(R.layout.activity_about);

        // Back
        findViewById(R.id.aboutBackBtn).setOnClickListener(v -> finish());

        // Version
        TextView tvVersion = findViewById(R.id.aboutVersion);
        try {
            String ver = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            tvVersion.setText("Version " + ver);
        } catch (Exception e) {
            tvVersion.setText("Version 1.0");
        }

        // Email
        findViewById(R.id.aboutEmail).setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO,
                    Uri.parse("mailto:hyper.devstudio@protonmail.com"));
            startActivity(Intent.createChooser(emailIntent, "Send Email"));
        });

        // GitHub
        findViewById(R.id.aboutGithubRow).setOnClickListener(v ->
                WebViewActivity.openGitHub(this, "https://github.com/Subham-x/Choose-Browser-Android"));

        // Feedback
        findViewById(R.id.aboutFeedbackRow).setOnClickListener(v ->
                WebViewActivity.openFeedback(this));

        // Privacy Policy
        findViewById(R.id.aboutPrivacyRow).setOnClickListener(v ->
                WebViewActivity.openPrivacyPolicy(this));
    }
}
