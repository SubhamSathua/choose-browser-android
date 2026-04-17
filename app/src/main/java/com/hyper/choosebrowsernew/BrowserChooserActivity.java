package com.hyper.choosebrowsernew;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrowserChooserActivity extends AppCompatActivity {

    private static final Pattern URL_PATTERN = Pattern.compile(
        "(?i)\\b((?:https?://|www\\.)[^\\s<>()\\[\\]{}]+|"
            + "(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,}(?::\\d{2,5})?(?:[/?#][^\\s<>()\\[\\]{}]*)?|"
            + "(?:\\d{1,3}\\.){3}\\d{1,3}(?::\\d{2,5})?(?:[/?#][^\\s<>()\\[\\]{}]*)?|"
            + "localhost(?::\\d{2,5})?(?:[/?#][^\\s<>()\\[\\]{}]*)?)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Set<String> COMMON_TLDS = new LinkedHashSet<>(Arrays.asList(
        "com", "org", "net", "io", "app", "dev", "ai", "co", "edu", "gov",
        "me", "in", "uk", "us", "biz", "info"
    ));

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

        String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action)) {
            Uri data = intent.getData();
            if (data != null) {
                openChooserForUrl(data.toString());
                return;
            }
        } else if (Intent.ACTION_SEND.equals(action)) {
            List<String> foundLinks = extractUrls(intent.getStringExtra(Intent.EXTRA_TEXT));
            if (foundLinks.isEmpty()) {
                Toast.makeText(this, "Cant find a link", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            if (foundLinks.size() == 1) {
                openChooserForUrl(foundLinks.get(0));
            } else {
                showLinkPicker(foundLinks);
            }
            return;
        }

        Toast.makeText(this, "Cant find a link", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void openChooserForUrl(String candidate) {
        String url = normalizeForIntent(candidate);
        if (TextUtils.isEmpty(url)) {
            Toast.makeText(this, "Cant find a link", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        BrowserChooserBottomSheet chooser = BrowserChooserBottomSheet.newInstance(url);
        chooser.show(getSupportFragmentManager(), "browser_chooser");
    }

    private void showLinkPicker(List<String> links) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_link_picker, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setDimAmount(0.55f);
        }

        ImageView closeBtn = view.findViewById(R.id.linkPickerClose);
        TextView cancelBtn = view.findViewById(R.id.linkPickerCancel);
        ListView listView = view.findViewById(R.id.linkPickerList);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.item_link_picker,
                android.R.id.text1,
                links
        );
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, itemView, position, id) -> {
            dialog.dismiss();
            openChooserForUrl(links.get(position));
        });

        View.OnClickListener dismissAndFinish = v -> {
            dialog.dismiss();
            finish();
        };

        closeBtn.setOnClickListener(dismissAndFinish);
        cancelBtn.setOnClickListener(dismissAndFinish);
        dialog.setOnCancelListener(d -> finish());
        dialog.show();
    }

    private List<String> extractUrls(String text) {
        List<String> result = new ArrayList<>();
        if (TextUtils.isEmpty(text)) return result;

        LinkedHashSet<String> unique = new LinkedHashSet<>();
        Matcher matcher = URL_PATTERN.matcher(text.trim());
        while (matcher.find()) {
            String found = matcher.group(1);
            String cleaned = cleanMatchedUrl(found);
            if (!TextUtils.isEmpty(cleaned) && isAllowedDomain(cleaned)) {
                unique.add(cleaned);
            }
        }

        result.addAll(unique);
        return result;
    }

    private String cleanMatchedUrl(String found) {
        if (TextUtils.isEmpty(found)) return "";
        String cleaned = found.trim();
        cleaned = cleaned.replaceAll("^[\\(\\[\\{<\"']+", "");
        cleaned = cleaned.replaceAll("[\\)\\]\\}>,!;:\"']+$", "");
        cleaned = cleaned.replaceAll("\\.+$", "");
        cleaned = cleaned.replaceAll("\\?$", "");
        return cleaned;
    }

    private boolean isAllowedDomain(String candidate) {
        if (TextUtils.isEmpty(candidate)) return false;

        String test = candidate;
        if (!test.startsWith("http://") && !test.startsWith("https://")) {
            test = "https://" + test;
        }

        Uri uri = Uri.parse(test);
        String host = uri.getHost();
        if (TextUtils.isEmpty(host)) return false;

        host = host.toLowerCase(Locale.US);
        if ("localhost".equals(host)) return true;
        if (host.matches("^\\d{1,3}(\\.\\d{1,3}){3}$")) return true;

        int lastDot = host.lastIndexOf('.');
        if (lastDot < 0 || lastDot == host.length() - 1) return false;

        String tld = host.substring(lastDot + 1);
        if (tld.length() == 2) return true;
        return COMMON_TLDS.contains(tld);
    }

    private String normalizeForIntent(String candidate) {
        if (TextUtils.isEmpty(candidate)) return candidate;
        String trimmed = candidate.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        return "https://" + trimmed;
    }
}
