package com.hyper.choosebrowsernew;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        PackageManager pm = getPackageManager();
        Map<String, Intent> appIntents = new LinkedHashMap<>();

        Intent probeSendTo = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
        List<ResolveInfo> sendToApps = pm.queryIntentActivities(probeSendTo, 0);
        for (ResolveInfo info : sendToApps) {
            String pkg = info.activityInfo.packageName;
            Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + AppConstantsDetails.CONTACT_EMAIL));
            i.putExtra(Intent.EXTRA_SUBJECT, AppConstantsDetails.EMAIL_SUBJECT);
            i.putExtra(Intent.EXTRA_TEXT, AppConstantsDetails.EMAIL_BODY_TEMPLATE);
            i.setPackage(pkg);
            appIntents.put(pkg, i);
        }

        Intent probeSend = new Intent(Intent.ACTION_SEND);
        probeSend.setType("message/rfc822");
        List<ResolveInfo> sendApps = pm.queryIntentActivities(probeSend, 0);
        for (ResolveInfo info : sendApps) {
            String pkg = info.activityInfo.packageName;
            if (appIntents.containsKey(pkg)) continue;

            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("message/rfc822");
            i.putExtra(Intent.EXTRA_EMAIL, new String[]{AppConstantsDetails.CONTACT_EMAIL});
            i.putExtra(Intent.EXTRA_SUBJECT, AppConstantsDetails.EMAIL_SUBJECT);
            i.putExtra(Intent.EXTRA_TEXT, AppConstantsDetails.EMAIL_BODY_TEMPLATE);
            i.setPackage(pkg);
            appIntents.put(pkg, i);
        }

        if (appIntents.isEmpty()) {
            Toast.makeText(this, "No mail app found", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<Intent> intents = new ArrayList<>(appIntents.values());
        Intent primary = intents.remove(0);
        Intent chooser = Intent.createChooser(primary, "Send Email");
        if (!intents.isEmpty()) {
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toArray(new Parcelable[0]));
        }

        try {
            startActivity(chooser);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No mail app found", Toast.LENGTH_SHORT).show();
        }
    }
}
