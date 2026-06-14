package com.hyper.choosebrowsernew;

import android.app.Dialog;
import android.content.Context;
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
        Context themedContext = ThemeHelper.wrapWithColorThemeOverlay(this);
        final Dialog dialog = new Dialog(themedContext);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);

        View view = LayoutInflater.from(themedContext).inflate(R.layout.dialog_link_picker, null);
        dialog.setContentView(view);

        // Resolve all relevant theme colors
        int surface = ThemeHelper.resolveThemeColor(themedContext, R.attr.colorPopupSurface, R.color.backgroundSecondary);
        int dockBg = ThemeHelper.resolveThemeColor(themedContext, R.attr.colorPopupDock, R.color.PopUpCardDockBg);
        int textCol = ThemeHelper.resolveThemeColor(themedContext, R.attr.colorPopupText, R.color.text);
        int actionBg = ThemeHelper.resolveThemeColor(themedContext, R.attr.colorPopupAction, R.color.PopUpCard_ActionBtnBg);

        View card = view.findViewById(R.id.linkPickerCard);
        if (card instanceof androidx.cardview.widget.CardView) {
            ((androidx.cardview.widget.CardView) card).setCardBackgroundColor(surface);
        }

        TextView title = view.findViewById(R.id.linkPickerTitle);
        if (title != null) title.setTextColor(textCol);

        // Tint sub-title as well (it's the second child in the layout)
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup vg = (android.view.ViewGroup) ((android.view.ViewGroup) view).getChildAt(0);
            if (vg != null && vg.getChildAt(1) instanceof TextView) {
                ((TextView) vg.getChildAt(1)).setTextColor(textCol);
                ((TextView) vg.getChildAt(1)).setAlpha(0.7f); // Slightly transparent for sub-text
            }
        }

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setDimAmount(0.55f);
        }

        ImageView closeBtn = view.findViewById(R.id.linkPickerClose);
        if (closeBtn != null) closeBtn.setColorFilter(textCol);

        TextView cancelBtn = view.findViewById(R.id.linkPickerCancel);
        if (cancelBtn != null) {
            cancelBtn.setTextColor(textCol);
            // Apply the dockBg color to the cancel button background to match the privacy button style
            if (cancelBtn.getBackground() != null) {
                cancelBtn.getBackground().setTint(dockBg);
            }
        }

        ListView listView = view.findViewById(R.id.linkPickerList);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                themedContext,
                R.layout.item_link_picker,
                android.R.id.text1,
                links
        ) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                if (v instanceof androidx.cardview.widget.CardView) {
                    ((androidx.cardview.widget.CardView) v).setCardBackgroundColor(dockBg);
                }
                TextView tv = v.findViewById(android.R.id.text1);
                if (tv != null) tv.setTextColor(textCol);
                return v;
            }
        };
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, itemView, position, id) -> {
            dialog.dismiss();
            openChooserForUrl(links.get(position));
        });

        View.OnClickListener dismissAndFinish = v -> {
            dialog.dismiss();
            finish();
        };

        if (closeBtn != null) closeBtn.setOnClickListener(dismissAndFinish);
        if (cancelBtn != null) cancelBtn.setOnClickListener(dismissAndFinish);
        dialog.setOnCancelListener(d -> finish());
        dialog.show();
    }

    private List<String> extractUrls(String text) {
        List<String> result = new ArrayList<>();
        if (TextUtils.isEmpty(text)) return result;

        LinkedHashSet<String> unique = new LinkedHashSet<>();
        Matcher matcher = URL_PATTERN.matcher(text);
        while (matcher.find()) {
            String found = matcher.group(1);
            String cleaned = cleanMatchedUrl(found);
            if (!TextUtils.isEmpty(cleaned) && isProbablyAUrl(cleaned)) {
                unique.add(cleaned);
            }
        }

        result.addAll(unique);
        return result;
    }

    private String cleanMatchedUrl(String found) {
        if (TextUtils.isEmpty(found)) return "";
        String cleaned = found.trim();
        // Remove surrounding brackets or quotes
        cleaned = cleaned.replaceAll("^[\\(\\[\\{<\"']+", "");
        cleaned = cleaned.replaceAll("[\\)\\]\\}>,!;:\"']+$", "");
        // Remove trailing punctuation often captured by loose regex
        cleaned = cleaned.replaceAll("[\\.\\?\\!]+$", "");
        return cleaned;
    }

    private boolean isProbablyAUrl(String candidate) {
        if (TextUtils.isEmpty(candidate)) return false;

        // If it starts with protocol or www, it's almost certainly a URL
        String lower = candidate.toLowerCase(Locale.US);
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("www.")) {
            return true;
        }

        // Otherwise, check for a valid-ish domain structure
        try {
            String test = candidate;
            if (!test.contains("://")) test = "https://" + test;
            Uri uri = Uri.parse(test);
            String host = uri.getHost();
            if (TextUtils.isEmpty(host)) return false;

            host = host.toLowerCase(Locale.US);
            if ("localhost".equals(host)) return true;
            if (host.matches("^\\d{1,3}(\\.\\d{1,3}){3}$")) return true; // IP address

            int lastDot = host.lastIndexOf('.');
            if (lastDot < 0 || lastDot == host.length() - 1) return false;

            String tld = host.substring(lastDot + 1);
            // Allow any TLD that is at least 2 letters long (covers all country codes and gTLDs)
            return tld.length() >= 2 && tld.matches("[a-z0-9]+");
        } catch (Exception e) {
            return false;
        }
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
