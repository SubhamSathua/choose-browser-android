package com.hyper.choosebrowsernew.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.hyper.choosebrowsernew.AppConstantsDetails;
import com.hyper.choosebrowsernew.ui.common.ViewModelFactory;
import com.hyper.choosebrowsernew.ui.main.MainActivity;
import com.hyper.choosebrowsernew.BuildConfig;
import com.hyper.choosebrowsernew.R;
import com.hyper.choosebrowsernew.UpdateChecker;
import com.hyper.choosebrowsernew.ui.common.UpdateUiHelper;
import com.hyper.choosebrowsernew.ui.debug.DebugActivity;
import com.hyper.choosebrowsernew.ui.debug.DebugConfig;
import com.hyper.choosebrowsernew.ui.webview.WebViewActivity;
import com.hyper.choosebrowsernew.util.MotionUiHelper;
import com.hyper.choosebrowsernew.util.ThemeHelper;


public class SettingsActivity extends AppCompatActivity {
    CardView demoButton, aboutButton, feedbackButton, privacyPolicyButton;
    private CardView settingsUpdateCard;
    private LinearLayout settingsUpdateCardInner;
    private View settingsUpdateDot;
    private TextView settingsUpdateTitle, settingsUpdateMsg;
    private SettingsViewModel viewModel;

    private int debugTapCount = 0;
    private long lastDebugTapTime = 0L;
    private static final long DEBUG_TAP_TIMEOUT_MS = 2000L;
    private static final int TOTAL_DEBUG_TAPS = 7;
    private Toast debugToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.applySystemBars(this);
        setContentView(R.layout.settings_activity);

        // Block access if critical update is required
        UpdateChecker.Result cachedUpdate = UpdateChecker.getCachedResult(this);
        if (cachedUpdate.priority == UpdateChecker.Priority.CRITICAL || cachedUpdate.priority == UpdateChecker.Priority.KILL_SWITCH) {
            startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this, new ViewModelFactory(this)).get(SettingsViewModel.class);

        RadioGroup themeRadioGroup = findViewById(R.id.themeRadioGroup);
        demoButton = findViewById(R.id.demoButton);
        aboutButton = findViewById(R.id.aboutButton);
        feedbackButton = findViewById(R.id.feedbackButton);
        privacyPolicyButton = findViewById(R.id.privacyPolicyButton);
        CardView excludeBrowserCard = findViewById(R.id.excludeBrowserCard);
        CardView colorThemesCard = findViewById(R.id.colorThemesCard);
        CardView versionCard = findViewById(R.id.versionCard);
        CardView debugCard = findViewById(R.id.debugCard);
        View backBtn = findViewById(R.id.backBtn);
        TextView colorThemesValue = findViewById(R.id.colorThemesValue);
        TextView versionText = findViewById(R.id.versionText);

        setupMotion();

        // ── Observe theme changes ──
        viewModel.themeMode.observe(this, mode -> {
            // Temporarily remove listener to avoid loop during initial set or programmatic updates
            themeRadioGroup.setOnCheckedChangeListener(null);
            if (mode == AppCompatDelegate.MODE_NIGHT_NO) themeRadioGroup.check(R.id.radioLight);
            else if (mode == AppCompatDelegate.MODE_NIGHT_YES) themeRadioGroup.check(R.id.radioDark);
            else themeRadioGroup.check(R.id.radioSystem);
            
            // Re-attach listener
            themeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                int newMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                if (checkedId == R.id.radioLight) newMode = AppCompatDelegate.MODE_NIGHT_NO;
                else if (checkedId == R.id.radioDark) newMode = AppCompatDelegate.MODE_NIGHT_YES;
                
                if (viewModel.themeMode.getValue() != null && viewModel.themeMode.getValue() != newMode) {
                    viewModel.setThemeMode(newMode);
                    // AppCompatDelegate.setDefaultNightMode handles recreation automatically
                }
            });
        });

        viewModel.colorThemeId.observe(this, themeId -> updateColorThemeLabel(colorThemesValue, themeId));
        viewModel.appVersion.observe(this, versionText::setText);

        demoButton.setOnClickListener(v -> openLink(AppConstantsDetails.TEST_POPUP_URL));
        aboutButton.setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class)));
        feedbackButton.setOnClickListener(v -> WebViewActivity.openFeedback(this));
        privacyPolicyButton.setOnClickListener(v -> WebViewActivity.openPrivacyPolicy(this));
        excludeBrowserCard.setOnClickListener(v -> startActivity(new Intent(this, ExcludedBrowsersActivity.class)));
        colorThemesCard.setOnClickListener(v -> showColorThemePicker(colorThemesValue));
        backBtn.setOnClickListener(v -> finish());

        // Update card
        settingsUpdateCard = findViewById(R.id.settingsUpdateCard);
        settingsUpdateCardInner = findViewById(R.id.settingsUpdateCardInner);
        settingsUpdateDot = findViewById(R.id.settingsUpdateDot);
        settingsUpdateTitle = findViewById(R.id.settingsUpdateTitle);
        settingsUpdateMsg = findViewById(R.id.settingsUpdateMsg);
        
        View.OnClickListener updateClick = v -> {
            if (lastUpdateResult != null) UpdateUiHelper.showInfoSheet(this, lastUpdateResult);
        };
        settingsUpdateCard.setOnClickListener(updateClick);
        settingsUpdateCardInner.setOnClickListener(updateClick);

        // 7-tap sequence on version card/text to unlock debug in stable; alpha is always unhidden
        View.OnClickListener versionTapListener = v -> handleVersionTap();
        versionCard.setOnClickListener(versionTapListener);
        versionText.setOnClickListener(versionTapListener);

        updateDebugVisibility();

        checkForUpdates();
    }

    private void setupMotion() {
        MotionUiHelper.applyTapScale(demoButton);
        MotionUiHelper.applyTapScale(aboutButton);
        MotionUiHelper.applyTapScale(feedbackButton);
        MotionUiHelper.applyTapScale(privacyPolicyButton);
        MotionUiHelper.applyTapScale(findViewById(R.id.excludeBrowserCard));
        MotionUiHelper.applyTapScale(findViewById(R.id.colorThemesCard));
        MotionUiHelper.applyTapScale(findViewById(R.id.versionCard));
        MotionUiHelper.applyTapScale(findViewById(R.id.debugCard));
        MotionUiHelper.applyTapScale(findViewById(R.id.debugCardHideBtn));
        MotionUiHelper.applyTapScale(findViewById(R.id.backBtn));
    }

    private void handleVersionTap() {
        if (DebugConfig.isAlpha()) {
            showDebugToast("Debug menu is already available in Alpha");
            return;
        }

        if (DebugConfig.isDebugUnlocked(this)) {
            showDebugToast("Debug menu is already unhidden");
            return;
        }

        long now = SystemClock.uptimeMillis();
        if (now - lastDebugTapTime > DEBUG_TAP_TIMEOUT_MS) {
            debugTapCount = 0;
        }
        lastDebugTapTime = now;
        debugTapCount++;

        if (debugTapCount < 4) {
            // Taps 1 to 3: silent
            return;
        }

        int remaining = TOTAL_DEBUG_TAPS - debugTapCount;
        if (remaining > 0) {
            String stepWord = remaining == 1 ? "step" : "steps";
            showDebugToast("You are now " + remaining + " " + stepWord + " away from unhiding debug menu.");
        } else {
            debugTapCount = 0;
            DebugConfig.unlockDebug(this);
            showDebugToast("Debug menu unhidden!");
            updateDebugVisibility();
        }
    }

    private void showDebugToast(String message) {
        if (debugToast != null) {
            debugToast.cancel();
        }
        debugToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        debugToast.show();
    }

    private void updateDebugVisibility() {
        CardView debugCard = findViewById(R.id.debugCard);
        View debugCardHideBtn = findViewById(R.id.debugCardHideBtn);
        if (debugCard == null) return;

        boolean isUnlocked = DebugConfig.isDebugUnlocked(this);
        if (isUnlocked) {
            debugCard.setVisibility(View.VISIBLE);
            debugCard.setOnClickListener(v -> startActivity(new Intent(this, DebugActivity.class)));
            if (debugCardHideBtn != null) {
                if (!DebugConfig.isAlpha()) {
                    debugCardHideBtn.setVisibility(View.VISIBLE);
                    debugCardHideBtn.setOnClickListener(v -> {
                        DebugConfig.hideDebug(this);
                        showDebugToast("Debug menu hidden");
                        updateDebugVisibility();
                    });
                } else {
                    debugCardHideBtn.setVisibility(View.GONE);
                }
            }
        } else {
            debugCard.setVisibility(View.GONE);
            if (debugCardHideBtn != null) {
                debugCardHideBtn.setVisibility(View.GONE);
            }
        }
    }

    private UpdateChecker.Result lastUpdateResult;

    @Override
    protected void onResume() {
        super.onResume();
        ThemeHelper.applySystemBars(this);
        updateDebugVisibility();
    }

    private void checkForUpdates() {
        UpdateChecker.check(this, result -> {
            lastUpdateResult = result;
            if (result.priority == UpdateChecker.Priority.UP_TO_DATE || result.priority == UpdateChecker.Priority.ERROR) {
                settingsUpdateCard.setVisibility(View.GONE);
                return;
            }

            settingsUpdateCard.setVisibility(View.VISIBLE);
            settingsUpdateMsg.setText(result.shortMsg != null ? result.shortMsg : "");
            UpdateUiHelper.applyUpdateCardStyle(this, settingsUpdateCardInner, settingsUpdateDot, settingsUpdateTitle, settingsUpdateMsg, result.priority);

            switch (result.priority) {
                case KILL_SWITCH: settingsUpdateTitle.setText(result.title != null ? result.title : "Service Suspended"); break;
                case CRITICAL: settingsUpdateTitle.setText("Critical Update Required"); break;
                case WARNING: settingsUpdateTitle.setText("Update Recommended"); break;
                case LATEST: settingsUpdateTitle.setText("Update Available"); break;
            }
        });
    }

    private void openLink(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    private void showColorThemePicker(TextView label) {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(this, R.style.PreviewMoreBottomSheet);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.settings_sheet_themes, null);
        bottomSheet.setContentView(sheetView);

        RadioGroup radioGroup = sheetView.findViewById(R.id.colorThemesRadioGroup);
        String saved = viewModel.colorThemeId.getValue();
        
        if (ThemeHelper.COLOR_THEME_AMOLED.equals(saved)) radioGroup.check(R.id.colorThemeRadioAmoled);
        else if (ThemeHelper.COLOR_THEME_MATERIAL.equals(saved)) radioGroup.check(R.id.colorThemeRadioMaterial);
        else if (ThemeHelper.COLOR_THEME_CATPPUCCIN.equals(saved)) radioGroup.check(R.id.colorThemeRadioCatppuccin);
        else if (ThemeHelper.COLOR_THEME_CATPPUCCIN_RED.equals(saved)) radioGroup.check(R.id.colorThemeRadioCatppuccinRed);
        else if (ThemeHelper.COLOR_THEME_DRACULA.equals(saved)) radioGroup.check(R.id.colorThemeRadioDracula);
        else if (ThemeHelper.COLOR_THEME_CYBERPUNK_TERMINAL.equals(saved)) radioGroup.check(R.id.colorThemeRadioCyberpunkTerminal);
        else if (ThemeHelper.COLOR_THEME_CYBERPUNK.equals(saved)) radioGroup.check(R.id.colorThemeRadioCyberpunk);
        else if (ThemeHelper.COLOR_THEME_MONOKAI.equals(saved)) radioGroup.check(R.id.colorThemeRadioDracula);
        else radioGroup.check(R.id.colorThemeRadioDefault);

        sheetView.findViewById(R.id.colorThemesCloseBtn).setOnClickListener(v -> bottomSheet.dismiss());

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String themeId = ThemeHelper.COLOR_THEME_DEFAULT;
            if (checkedId == R.id.colorThemeRadioAmoled) themeId = ThemeHelper.COLOR_THEME_AMOLED;
            else if (checkedId == R.id.colorThemeRadioMaterial) themeId = ThemeHelper.COLOR_THEME_MATERIAL;
            else if (checkedId == R.id.colorThemeRadioCatppuccin) themeId = ThemeHelper.COLOR_THEME_CATPPUCCIN;
            else if (checkedId == R.id.colorThemeRadioCatppuccinRed) themeId = ThemeHelper.COLOR_THEME_CATPPUCCIN_RED;
            else if (checkedId == R.id.colorThemeRadioDracula) themeId = ThemeHelper.COLOR_THEME_DRACULA;
            else if (checkedId == R.id.colorThemeRadioCyberpunkTerminal) themeId = ThemeHelper.COLOR_THEME_CYBERPUNK_TERMINAL;
            else if (checkedId == R.id.colorThemeRadioCyberpunk) themeId = ThemeHelper.COLOR_THEME_CYBERPUNK;

            viewModel.setColorThemeId(themeId);
            bottomSheet.dismiss();
        });
        bottomSheet.show();
    }

    private void updateColorThemeLabel(TextView label, String themeId) {
        int resId = R.string.color_theme_default;
        if (ThemeHelper.COLOR_THEME_AMOLED.equals(themeId)) resId = R.string.color_theme_amoled;
        else if (ThemeHelper.COLOR_THEME_MATERIAL.equals(themeId)) resId = R.string.color_theme_material;
        else if (ThemeHelper.COLOR_THEME_CATPPUCCIN.equals(themeId)) resId = R.string.color_theme_catppuccin;
        else if (ThemeHelper.COLOR_THEME_CATPPUCCIN_RED.equals(themeId)) resId = R.string.color_theme_catppuccin_red;
        else if (ThemeHelper.COLOR_THEME_DRACULA.equals(themeId)) resId = R.string.color_theme_dracula;
        else if (ThemeHelper.COLOR_THEME_CYBERPUNK_TERMINAL.equals(themeId)) resId = R.string.color_theme_cyberpunk_terminal;
        else if (ThemeHelper.COLOR_THEME_CYBERPUNK.equals(themeId)) resId = R.string.color_theme_cyberpunk;
        else if (ThemeHelper.COLOR_THEME_MONOKAI.equals(themeId)) resId = R.string.color_theme_dracula;
        label.setText(resId);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
