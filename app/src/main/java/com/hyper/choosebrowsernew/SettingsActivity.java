package com.hyper.choosebrowsernew;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomsheet.BottomSheetDialog;


public class SettingsActivity extends AppCompatActivity {
    CardView demoButton, aboutButton, feedbackButton, privacyPolicyButton;
    private CardView settingsUpdateCard;
    private LinearLayout settingsUpdateCardInner;
    private View settingsUpdateDot;
    private TextView settingsUpdateTitle, settingsUpdateMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ==== Status and Navbar color ====
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.backgroundPrimary));
            getWindow().setNavigationBarColor(getResources().getColor(R.color.backgroundPrimary));
        }
        // Apply saved theme before setting content view
        // ThemeHelper.applySavedTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings); // your settings layout

        // Now everything is inside onCreate:
        RadioGroup themeRadioGroup = findViewById(R.id.themeRadioGroup);
        demoButton = findViewById(R.id.demoButton);
        aboutButton = findViewById(R.id.aboutButton);
        feedbackButton = findViewById(R.id.feedbackButton);
        privacyPolicyButton = findViewById(R.id.privacyPolicyButton);
        CardView versionCard = findViewById(R.id.versionCard);
        CardView debugCard = findViewById(R.id.debugCard);
        View backBtn = findViewById(R.id.backBtn);
        TextView versionText = findViewById(R.id.versionText);

        MotionUiHelper.applyTapScale(demoButton);
        MotionUiHelper.applyTapScale(aboutButton);
        MotionUiHelper.applyTapScale(feedbackButton);
        MotionUiHelper.applyTapScale(privacyPolicyButton);
        MotionUiHelper.applyTapScale(versionCard);
        MotionUiHelper.applyTapScale(debugCard);
        MotionUiHelper.applyTapScale(backBtn);

        // Set current checked button based on saved mode
        int savedMode = ThemeHelper.getSavedThemeMode(this);
        if (savedMode == AppCompatDelegate.MODE_NIGHT_NO) {
            themeRadioGroup.check(R.id.radioLight);
        } else if (savedMode == AppCompatDelegate.MODE_NIGHT_YES) {
            themeRadioGroup.check(R.id.radioDark);
        } else {
            themeRadioGroup.check(R.id.radioSystem);
        }

        try {
            String versionName = getPackageManager()
                    .getPackageInfo(getPackageName(), 0)
                    .versionName;

            versionText.setText("Version " + versionName);
        } catch (Exception e) {
            versionText.setText("Version N/A");
            e.printStackTrace();
        }


        // Handle changes
        themeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioLight) {
                ThemeHelper.saveTheme(this, AppCompatDelegate.MODE_NIGHT_NO);
            } else if (checkedId == R.id.radioDark) {
                ThemeHelper.saveTheme(this, AppCompatDelegate.MODE_NIGHT_YES);
            } else if (checkedId == R.id.radioSystem) {
                ThemeHelper.saveTheme(this, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            }
            recreate(); // Refresh activity to apply theme immediately
        });


        demoButton.setOnClickListener(v ->
                openLink("https://example.com"));

        aboutButton.setOnClickListener(v ->
                startActivity(new Intent(this, AboutActivity.class)));

        feedbackButton.setOnClickListener(v ->
                WebViewActivity.openFeedback(this));

        privacyPolicyButton.setOnClickListener(v ->
            WebViewActivity.openPrivacyPolicy(this));

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());

        // Update card
        settingsUpdateCard = findViewById(R.id.settingsUpdateCard);
        settingsUpdateCardInner = findViewById(R.id.settingsUpdateCardInner);
        settingsUpdateDot = findViewById(R.id.settingsUpdateDot);
        settingsUpdateTitle = findViewById(R.id.settingsUpdateTitle);
        settingsUpdateMsg = findViewById(R.id.settingsUpdateMsg);
        settingsUpdateCard.setOnClickListener(v -> {
            if (lastUpdateResult != null) UpdateUiHelper.showInfoSheet(this, lastUpdateResult);
        });
        settingsUpdateCardInner.setOnClickListener(v -> {
            if (lastUpdateResult != null) UpdateUiHelper.showInfoSheet(this, lastUpdateResult);
        });

        // Debug card (only visible when DEBUG_SCREEN flag is true)
        if (DebugConfig.DEBUG_SCREEN) {
            debugCard.setVisibility(View.VISIBLE);
            debugCard.setOnClickListener(v ->
                    startActivity(new Intent(this, DebugActivity.class)));
        }

        checkForUpdates();
    }

    private UpdateChecker.Result lastUpdateResult;

    @Override
    protected void onResume() {
        super.onResume();
        // Enforce critical update block
        if (UpdateChecker.getCachedResult(this).priority == UpdateChecker.Priority.CRITICAL) {
            startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            finish();
        }
    }

    private void checkForUpdates() {
        UpdateChecker.check(this, result -> {
            lastUpdateResult = result;
            if (result.priority == UpdateChecker.Priority.CRITICAL) {
                // Redirect if freshly detected
                startActivity(new Intent(this, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                finish();
                return;
            }
            if (result.priority == UpdateChecker.Priority.UP_TO_DATE
                    || result.priority == UpdateChecker.Priority.ERROR) {
                settingsUpdateCard.setVisibility(View.GONE);
                return;
            }

            settingsUpdateCard.setVisibility(View.VISIBLE);
            settingsUpdateMsg.setText(result.shortMsg != null ? result.shortMsg : "");

            switch (result.priority) {
                case WARNING:
                    settingsUpdateTitle.setText("Update Recommended");
                    settingsUpdateCardInner.setBackgroundResource(R.drawable.bg_update_card_warning);
                    settingsUpdateDot.setBackgroundResource(R.drawable.dot_warning);
                    break;
                case LATEST:
                    settingsUpdateTitle.setText("Update Available");
                    settingsUpdateCardInner.setBackgroundResource(R.drawable.bg_update_card_latest);
                    settingsUpdateDot.setBackgroundResource(R.drawable.dot_blue);
                    break;
            }
        });
    }

    private void openLink(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
