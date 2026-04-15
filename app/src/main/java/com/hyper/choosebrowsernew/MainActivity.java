package com.hyper.choosebrowsernew;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_OVERLAY_PERMISSION = 1001;

    LinearLayout defBrowserBtn, overlayPermBtn;
    Button settingsBtn;
    ImageView perm1Check, perm2Check;

    // Update UI
    private CardView updateCard;
    private LinearLayout updateCardInner;
    private View updateDot;
    private TextView updateTitle, updateMsg;
    private UpdateChecker.Result lastUpdateResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ==== Status and Navbar color ====
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.backgroundPrimary));
            getWindow().setNavigationBarColor(getResources().getColor(R.color.backgroundPrimary));
        }


//        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handleIncomingIntent(getIntent());
//
//        Intent intent = getIntent();
//        Uri data = intent.getData();
//
//        if (Intent.ACTION_VIEW.equals(intent.getAction()) && data != null && data.isHierarchical()) {
//            String url = data.toString();
//
//            BrowserChooserBottomSheet chooser = BrowserChooserBottomSheet.newInstance(url);
//            chooser.show(getSupportFragmentManager(), "browser_chooser");
//        }


        defBrowserBtn = findViewById(R.id.defBrowser_btn);
        overlayPermBtn = findViewById(R.id.overlayPerm_btn);
        settingsBtn = findViewById(R.id.settingsBtn); // ✅ now declared
        perm1Check = findViewById(R.id.perm1_check);
        perm2Check = findViewById(R.id.perm2_check);

        defBrowserBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openDefaultAppSettings();
            }
        });

        LinearLayout listBrowsers = findViewById(R.id.listBrowsers);
        listBrowsers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, BrowserListActivity.class);
                startActivity(intent);
            }
        });



        overlayPermBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestOverlayPermission();
            }
        });

        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        // Update card
        updateCard = findViewById(R.id.updateCard);
        updateCardInner = findViewById(R.id.updateCardInner);
        updateDot = findViewById(R.id.updateDot);
        updateTitle = findViewById(R.id.updateTitle);
        updateMsg = findViewById(R.id.updateMsg);
        updateCard.setOnClickListener(v -> {
            if (lastUpdateResult != null) UpdateUiHelper.showInfoSheet(this, lastUpdateResult);
        });
        updateCardInner.setOnClickListener(v -> {
            if (lastUpdateResult != null) UpdateUiHelper.showInfoSheet(this, lastUpdateResult);
        });

        updatePermissionIcons(); // Call once on launch
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    private void handleIncomingIntent(Intent intent) {
        Uri data = intent.getData();
        if (data != null) {
            String url = data.toString();
            // Block popup if critical update is required — just keep user on MainActivity
            if (UpdateChecker.getCachedResult(this).priority == UpdateChecker.Priority.CRITICAL) return;
            BrowserChooserBottomSheet chooser = BrowserChooserBottomSheet.newInstance(url);
            chooser.show(getSupportFragmentManager(), "browser_chooser");
        }
    }

    private void openDefaultAppSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }

    private void requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
        } else {
            Toast.makeText(this, "Overlay permission already granted", Toast.LENGTH_SHORT).show();
        }
    }

    private void updatePermissionIcons() {
        boolean isDefaultBrowser = false;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://"));
        intent.addCategory(Intent.CATEGORY_BROWSABLE);

        PackageManager pm = getPackageManager();
        ComponentName defaultHandler = intent.resolveActivity(pm);

        if (defaultHandler != null && defaultHandler.getPackageName().equals(getPackageName())) {
            isDefaultBrowser = true;
        }

        perm1Check.setImageResource(isDefaultBrowser ? R.drawable.ix_permitted : R.drawable.ix_not_permitted);
        perm2Check.setImageResource(Settings.canDrawOverlays(this) ? R.drawable.ix_permitted : R.drawable.ix_not_permitted);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            Toast.makeText(this,
                    Settings.canDrawOverlays(this) ? "Overlay permission granted" : "Overlay permission NOT granted",
                    Toast.LENGTH_SHORT).show();
            updatePermissionIcons();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionIcons();
        checkForUpdates();
    }

    // ── Update Checker ──────────────────────────────────────────────

    private void checkForUpdates() {
        UpdateChecker.check(this, result -> {
            lastUpdateResult = result;
            applyUpdateCard(result);
        });
    }

    private void applyUpdateCard(UpdateChecker.Result result) {
        if (result.priority == UpdateChecker.Priority.UP_TO_DATE
                || result.priority == UpdateChecker.Priority.ERROR) {
            updateCard.setVisibility(View.GONE);
            return;
        }

        updateCard.setVisibility(View.VISIBLE);
        updateMsg.setText(result.shortMsg != null ? result.shortMsg : "");

        switch (result.priority) {
            case CRITICAL:
                updateTitle.setText("Critical Update Required");
                updateCardInner.setBackgroundResource(R.drawable.bg_update_card_critical);
                updateDot.setBackgroundResource(R.drawable.dot_critical);
                break;
            case WARNING:
                updateTitle.setText("Update Recommended");
                updateCardInner.setBackgroundResource(R.drawable.bg_update_card_warning);
                updateDot.setBackgroundResource(R.drawable.dot_warning);
                break;
            case LATEST:
                updateTitle.setText("Update Available");
                updateCardInner.setBackgroundResource(R.drawable.bg_update_card_latest);
                updateDot.setBackgroundResource(R.drawable.dot_blue);
                break;
        }
    }

    private void openStoreUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }
}
