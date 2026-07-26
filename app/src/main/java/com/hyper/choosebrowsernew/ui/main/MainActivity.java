package com.hyper.choosebrowsernew.ui.main;

import android.app.role.RoleManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
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
import androidx.lifecycle.ViewModelProvider;

import com.hyper.choosebrowsernew.R;
import com.hyper.choosebrowsernew.UpdateChecker;
import com.hyper.choosebrowsernew.data.model.UpdateResult;
import com.hyper.choosebrowsernew.ui.common.UpdateUiHelper;
import com.hyper.choosebrowsernew.ui.common.ViewModelFactory;
import com.hyper.choosebrowsernew.ui.preview.PreviewPageActivity;
import com.hyper.choosebrowsernew.ui.settings.BrowserListActivity;
import com.hyper.choosebrowsernew.ui.settings.SettingsActivity;
import com.hyper.choosebrowsernew.util.MotionUiHelper;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_OVERLAY_PERMISSION = 1001;
    private static final int REQUEST_DEFAULT_BROWSER = 1002;

    LinearLayout overlayPermBtn;
    Button settingsBtn;
    ImageView perm2Check;

    // Default Browser card views
    LinearLayout defaultBrowserBtn;
    ImageView defaultBrowserIcon, defaultBrowserCheck;
    TextView defaultBrowserTitle, defaultBrowserSub;

    // Update UI
    private CardView updateCard;
    private LinearLayout updateCardInner;
    private View updateDot;
    private TextView updateTitle, updateMsg;
    private MainViewModel viewModel;
    private UpdateResult currentUpdateResult;

    // Private Search capsule
    private CardView privateSearchCapsule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ==== Status and Navbar color ====
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.backgroundPrimary));
            getWindow().setNavigationBarColor(getResources().getColor(R.color.backgroundPrimary));
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this, new ViewModelFactory(this)).get(MainViewModel.class);

        overlayPermBtn = findViewById(R.id.overlayPerm_btn);
        settingsBtn = findViewById(R.id.settingsBtn);
        perm2Check = findViewById(R.id.perm2_check);
        CardView permissionCard2 = findViewById(R.id.cardView3);
        CardView chooseBrowserCard = findViewById(R.id.BrandContainer);

        // Default Browser card views
        defaultBrowserBtn = findViewById(R.id.defaultBrowserBtn);
        defaultBrowserIcon = findViewById(R.id.defaultBrowserIcon);
        defaultBrowserCheck = findViewById(R.id.defaultBrowserCheck);
        defaultBrowserTitle = findViewById(R.id.defaultBrowserTitle);
        defaultBrowserSub = findViewById(R.id.defaultBrowserSub);

        // Update card
        updateCard = findViewById(R.id.updateCard);
        updateCardInner = findViewById(R.id.updateCardInner);
        updateDot = findViewById(R.id.updateDot);
        updateTitle = findViewById(R.id.updateTitle);
        updateMsg = findViewById(R.id.updateMsg);

        // Private Search capsule
        privateSearchCapsule = findViewById(R.id.privateSearchCapsule);

        setupMotion(permissionCard2, chooseBrowserCard);

        // Default browser button click
        defaultBrowserBtn.setOnClickListener(view -> setAsDefaultBrowser());

        LinearLayout listBrowsers = findViewById(R.id.listBrowsers);
        listBrowsers.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BrowserListActivity.class);
            startActivity(intent);
        });

        overlayPermBtn.setOnClickListener(view -> requestOverlayPermission());

        settingsBtn.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
        
        View.OnClickListener updateClick = v -> {
            if (currentUpdateResult != null) UpdateUiHelper.showInfoSheet(this, convertToOldResult(currentUpdateResult));
        };
        updateCard.setOnClickListener(updateClick);
        updateCardInner.setOnClickListener(updateClick);

        // Private Search capsule click → open PreviewPage with index.html
        privateSearchCapsule.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, PreviewPageActivity.class);
            i.putExtra("url", "file:///android_asset/private_browser/index.html");
            startActivity(i);
        });
        // Private Search capsule long-click → create home screen shortcut
        privateSearchCapsule.setOnLongClickListener(v -> {
            requestPinPrivateSearchShortcut();
            return true;
        });
        MotionUiHelper.applyTapScale(privateSearchCapsule);

        setupObservers(); // Moved here
        updatePermissionIcons();
        viewModel.checkUpdate();
    }

    private void setupMotion(View p2, View cb) {
        MotionUiHelper.applyTapScale(p2);
        MotionUiHelper.applyTapScale(cb);
        MotionUiHelper.applyTapScale(settingsBtn);
    }

    private void setupObservers() {
        viewModel.updateResult.observe(this, result -> {
            currentUpdateResult = result;
            applyUpdateCard(result);
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
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

        perm2Check.setImageResource(Settings.canDrawOverlays(this) ? R.drawable.ix_permitted : R.drawable.ix_not_permitted);
        
        if (defaultBrowserCheck != null) {
            defaultBrowserCheck.setImageResource(isDefaultBrowser ? R.drawable.ix_permitted : R.drawable.ix_not_permitted);
        }
        if (defaultBrowserSub != null) {
            defaultBrowserSub.setText(isDefaultBrowser ? "Already default browser" : "Tap to set as default browser");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            Toast.makeText(this,
                    Settings.canDrawOverlays(this) ? "Overlay permission granted" : "Overlay permission NOT granted",
                    Toast.LENGTH_SHORT).show();
            updatePermissionIcons();
        } else if (requestCode == REQUEST_DEFAULT_BROWSER) {
            updatePermissionIcons();
        }
    }

    private void setAsDefaultBrowser() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            RoleManager roleManager = (RoleManager) getSystemService(Context.ROLE_SERVICE);
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER)) {
                if (roleManager.isRoleHeld(RoleManager.ROLE_BROWSER)) {
                    Toast.makeText(this, "Already set as default browser", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER);
                startActivityForResult(intent, REQUEST_DEFAULT_BROWSER);
                return;
            }
        }
        openDefaultAppSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionIcons();
        applyUpdateCard(viewModel.getCachedUpdateResult());
    }

    private void applyUpdateCard(UpdateResult result) {
        if (result == null || result.priority == UpdateResult.Priority.UP_TO_DATE || result.priority == UpdateResult.Priority.ERROR) {
            updateCard.setVisibility(View.GONE);
            return;
        }

        currentUpdateResult = result;
        updateCard.setVisibility(View.VISIBLE);
        updateMsg.setText(result.shortMsg != null ? result.shortMsg : "");

        UpdateUiHelper.applyUpdateCardStyle(this, updateCardInner, updateDot, updateTitle, updateMsg, convertPriority(result.priority));

        switch (result.priority) {
            case CRITICAL: updateTitle.setText("Critical Update Required"); break;
            case WARNING: updateTitle.setText("Update Recommended"); break;
            case LATEST: updateTitle.setText("Update Available"); break;
        }
    }

    private UpdateChecker.Priority convertPriority(UpdateResult.Priority p) {
        return UpdateChecker.Priority.valueOf(p.name());
    }

    private UpdateChecker.Result convertToOldResult(UpdateResult r) {
        return new UpdateChecker.Result(convertPriority(r.priority), r.shortMsg, r.mdFileUrl, r.latestVersion);
    }

    private void requestPinPrivateSearchShortcut() {
        Intent intent = new Intent(MainActivity.this, PreviewPageActivity.class);
        intent.putExtra("url", "file:///android_asset/private_browser/index.html");
        intent.setAction(Intent.ACTION_VIEW);

        ShortcutInfo shortcut = new ShortcutInfo.Builder(this, "pinned_private_search")
                .setShortLabel("Private Search")
                .setLongLabel("Open Private Search Browser")
                .setIcon(Icon.createWithResource(this, R.drawable.ic_preview_page))
                .setIntent(intent)
                .build();

        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
        if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported()) {
            shortcutManager.requestPinShortcut(shortcut, null);
        } else {
            Toast.makeText(this, "Pinning shortcuts is not supported on this device", Toast.LENGTH_SHORT).show();
        }
    }
}
