package com.hyper.choosebrowsernew.ui.settings;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.content.res.Configuration;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.hyper.choosebrowsernew.AppConstantsDetails;
import com.hyper.choosebrowsernew.R;
import com.hyper.choosebrowsernew.ui.webview.WebViewActivity;
import com.hyper.choosebrowsernew.util.ThemeHelper;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Status bar matches the dark hero; nav bar matches page background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            boolean isDark = isDarkModeEnabled();
            getWindow().setStatusBarColor(getResources().getColor(
                    isDark ? R.color.previewPage_primary : R.color.white));
            getWindow().setNavigationBarColor(getResources().getColor(R.color.backgroundPrimary));
        }

        setContentView(R.layout.activity_about);

        // Back
        findViewById(R.id.aboutBackBtn).setOnClickListener(v -> finish());

        // Version
        TextView tvVersion = findViewById(R.id.aboutVersion);
        TextView tvFooter = findViewById(R.id.aboutFooter);
        try {
            String ver = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            tvVersion.setText("Version " + ver);
        } catch (Exception e) {
            tvVersion.setText("Version 1.0");
        }

        tvFooter.setText(getString(R.string.cfg_about_footer));

        // Email
        findViewById(R.id.aboutEmailRow).setOnClickListener(v -> openEmailApps());

        // GitHub
        findViewById(R.id.aboutGithubRow).setOnClickListener(v ->
            WebViewActivity.openGitHub(this, AppConstantsDetails.GITHUB_URL));

        // Feedback
        findViewById(R.id.aboutFeedbackRow).setOnClickListener(v ->
                WebViewActivity.openFeedback(this));

        // Privacy Policy
        findViewById(R.id.aboutPrivacyRow).setOnClickListener(v ->
                WebViewActivity.openPrivacyPolicy(this));
    }

    private void openEmailApps() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + AppConstantsDetails.CONTACT_EMAIL));
        intent.putExtra(Intent.EXTRA_SUBJECT, AppConstantsDetails.EMAIL_SUBJECT);
        intent.putExtra(Intent.EXTRA_TEXT, "Source:Choose Browser");

        try {
            startActivity(Intent.createChooser(intent, "Send Email"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No mail app found", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isDarkModeEnabled() {
        int mode = ThemeHelper.getSavedThemeMode(this);
        if (mode == AppCompatDelegate.MODE_NIGHT_YES) return true;
        if (mode == AppCompatDelegate.MODE_NIGHT_NO) return false;
        int uiMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return uiMode == Configuration.UI_MODE_NIGHT_YES;
    }
}
