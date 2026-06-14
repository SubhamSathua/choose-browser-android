package com.hyper.choosebrowsernew.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.hyper.choosebrowsernew.AppConstantsDetails;
import com.hyper.choosebrowsernew.ui.common.ViewModelFactory;
import com.hyper.choosebrowsernew.ui.main.MainActivity;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ==== Status and Navbar color ====
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.backgroundPrimary));
            getWindow().setNavigationBarColor(getResources().getColor(R.color.backgroundPrimary));
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

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

        if (DebugConfig.DEBUG_SCREEN) {
            debugCard.setVisibility(View.VISIBLE);
            debugCard.setOnClickListener(v -> startActivity(new Intent(this, DebugActivity.class)));
        }

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
        MotionUiHelper.applyTapScale(findViewById(R.id.backBtn));
    }

    private UpdateChecker.Result lastUpdateResult;

    @Override
    protected void onResume() {
        super.onResume();
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
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottomsheet_color_themes, null);
        bottomSheet.setContentView(sheetView);

        RadioGroup radioGroup = sheetView.findViewById(R.id.colorThemesRadioGroup);
        String saved = viewModel.colorThemeId.getValue();
        
        if (ThemeHelper.COLOR_THEME_ECLIPSE.equals(saved)) radioGroup.check(R.id.colorThemeRadioEclipse);
        else if (ThemeHelper.COLOR_THEME_NEURONIGHT.equals(saved)) radioGroup.check(R.id.colorThemeRadioNeuroNight);
        else if (ThemeHelper.COLOR_THEME_NEURAL_BLUE.equals(saved)) radioGroup.check(R.id.colorThemeRadioNeuralBlue);
        else if (ThemeHelper.COLOR_THEME_COSMIC.equals(saved)) radioGroup.check(R.id.colorThemeRadioCosmic);
        else if (ThemeHelper.COLOR_THEME_OBSIDIAN_PULSE.equals(saved)) radioGroup.check(R.id.colorThemeRadioObsidianPulse);
        else radioGroup.check(R.id.colorThemeRadioDefault);

        sheetView.findViewById(R.id.colorThemesCloseBtn).setOnClickListener(v -> bottomSheet.dismiss());

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String themeId = ThemeHelper.COLOR_THEME_DEFAULT;
            if (checkedId == R.id.colorThemeRadioEclipse) themeId = ThemeHelper.COLOR_THEME_ECLIPSE;
            else if (checkedId == R.id.colorThemeRadioNeuroNight) themeId = ThemeHelper.COLOR_THEME_NEURONIGHT;
            else if (checkedId == R.id.colorThemeRadioNeuralBlue) themeId = ThemeHelper.COLOR_THEME_NEURAL_BLUE;
            else if (checkedId == R.id.colorThemeRadioCosmic) themeId = ThemeHelper.COLOR_THEME_COSMIC;
            else if (checkedId == R.id.colorThemeRadioObsidianPulse) themeId = ThemeHelper.COLOR_THEME_OBSIDIAN_PULSE;

            viewModel.setColorThemeId(themeId);
            bottomSheet.dismiss();
        });
        bottomSheet.show();
    }

    private void updateColorThemeLabel(TextView label, String themeId) {
        int resId = R.string.color_theme_default;
        if (ThemeHelper.COLOR_THEME_ECLIPSE.equals(themeId)) resId = R.string.color_theme_eclipse;
        else if (ThemeHelper.COLOR_THEME_NEURONIGHT.equals(themeId)) resId = R.string.color_theme_neuronight;
        else if (ThemeHelper.COLOR_THEME_NEURAL_BLUE.equals(themeId)) resId = R.string.color_theme_neural_blue;
        else if (ThemeHelper.COLOR_THEME_COSMIC.equals(themeId)) resId = R.string.color_theme_cosmic;
        else if (ThemeHelper.COLOR_THEME_OBSIDIAN_PULSE.equals(themeId)) resId = R.string.color_theme_obsidian_pulse;
        label.setText(resId);
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
