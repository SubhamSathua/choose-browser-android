package com.hyper.choosebrowsernew.ui.main;

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
import androidx.lifecycle.ViewModelProvider;

import com.hyper.choosebrowsernew.R;
import com.hyper.choosebrowsernew.UpdateChecker;
import com.hyper.choosebrowsernew.data.model.UpdateResult;
import com.hyper.choosebrowsernew.ui.chooser.BrowserChooserBottomSheet;
import com.hyper.choosebrowsernew.ui.common.UpdateUiHelper;
import com.hyper.choosebrowsernew.ui.common.ViewModelFactory;
import com.hyper.choosebrowsernew.ui.settings.BrowserListActivity;
import com.hyper.choosebrowsernew.ui.settings.SettingsActivity;
import com.hyper.choosebrowsernew.util.MotionUiHelper;

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
    private MainViewModel viewModel;

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

        handleIncomingIntent(getIntent());

        defBrowserBtn = findViewById(R.id.defBrowser_btn);
        overlayPermBtn = findViewById(R.id.overlayPerm_btn);
        settingsBtn = findViewById(R.id.settingsBtn);
        perm1Check = findViewById(R.id.perm1_check);
        perm2Check = findViewById(R.id.perm2_check);
        CardView permissionCard1 = findViewById(R.id.cardView2);
        CardView permissionCard2 = findViewById(R.id.cardView3);
        CardView chooseBrowserCard = findViewById(R.id.BrandContainer);

        // Update card
        updateCard = findViewById(R.id.updateCard);
        updateCardInner = findViewById(R.id.updateCardInner);
        updateDot = findViewById(R.id.updateDot);
        updateTitle = findViewById(R.id.updateTitle);
        updateMsg = findViewById(R.id.updateMsg);

        setupMotion(permissionCard1, permissionCard2, chooseBrowserCard);

        defBrowserBtn.setOnClickListener(view -> openDefaultAppSettings());

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
            UpdateResult res = viewModel.updateResult.getValue();
            if (res != null) UpdateUiHelper.showInfoSheet(this, convertToOldResult(res));
        };
        updateCard.setOnClickListener(updateClick);
        updateCardInner.setOnClickListener(updateClick);

        setupObservers(); // Moved here
        updatePermissionIcons();
        viewModel.checkUpdate();
    }

    private void setupMotion(View p1, View p2, View cb) {
        MotionUiHelper.applyTapScale(p1);
        MotionUiHelper.applyTapScale(p2);
        MotionUiHelper.applyTapScale(cb);
        MotionUiHelper.applyTapScale(settingsBtn);
    }

    private void setupObservers() {
        viewModel.updateResult.observe(this, this::applyUpdateCard);
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
            if (viewModel.getCachedUpdateResult().priority == UpdateResult.Priority.CRITICAL) return;
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
        applyUpdateCard(viewModel.getCachedUpdateResult());
    }

    private void applyUpdateCard(UpdateResult result) {
        if (result == null || result.priority == UpdateResult.Priority.UP_TO_DATE || result.priority == UpdateResult.Priority.ERROR) {
            updateCard.setVisibility(View.GONE);
            return;
        }

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
}
