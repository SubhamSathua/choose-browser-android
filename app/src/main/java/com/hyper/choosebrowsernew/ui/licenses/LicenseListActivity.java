package com.hyper.choosebrowsernew.ui.licenses;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.hyper.choosebrowsernew.R;
import com.hyper.choosebrowsernew.ui.webview.WebViewActivity;
import com.hyper.choosebrowsernew.util.ThemeHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class LicenseListActivity extends AppCompatActivity {

    private static final String TAG = "LicenseList";
    private ArrayList<LicenseItem> licenseList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        ThemeHelper.applySystemBars(this);

        setContentView(R.layout.about_activity_license);

        findViewById(R.id.licenseListBackBtn).setOnClickListener(v -> finish());

        licenseList = loadLicenses();

        ListView listView = findViewById(R.id.licenseListView);
        TextView emptyView = findViewById(R.id.licenseEmptyView);

        if (licenseList == null || licenseList.isEmpty()) {
            Log.e(TAG, "loaded 0 licenses — showing empty state");
            listView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            Log.d(TAG, "loaded " + licenseList.size() + " licenses");
            listView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            LicenseListAdapter adapter = new LicenseListAdapter(this, licenseList);
            listView.setAdapter(adapter);
            listView.setOnItemClickListener((parent, view, position, id) -> {
            Log.d(TAG, "item clicked position=" + position);
            LicenseItem item = licenseList.get(position);
            Log.d(TAG, "opening license: " + item.name + " file=" + item.fileName);
            String html = buildLicenseHtml(item);
            Log.d(TAG, "html length=" + html.length());
            Intent intent = new Intent(this, WebViewActivity.class);
            intent.putExtra(WebViewActivity.EXTRA_HTML_CONTENT, html);
            intent.putExtra(WebViewActivity.EXTRA_TITLE, item.name);
            startActivity(intent);
            Log.d(TAG, "WebViewActivity started");
        });
        }
    }

    private String buildLicenseHtml(LicenseItem item) {
        boolean isDark = isDarkMode();
        Log.d(TAG, "buildLicenseHtml isDark=" + isDark);

        String bg = isDark ? "#121214" : "#f1f1f3";
        String cardBg = isDark ? "#1c1c1f" : "#ffffff";
        String cardBorder = isDark ? "#2c2c30" : "#e2e2e8";
        String badgeBg = isDark ? "#22283a" : "#eef2ff";
        String badgeText = isDark ? "#7aa2f7" : "#2563eb";
        String titleColor = isDark ? "#ffffff" : "#111827";
        String subColor = isDark ? "#8e8e93" : "#6b7280";
        String bodyBg = isDark ? "#18181b" : "#ffffff";
        String bodyBorder = isDark ? "#27272a" : "#e2e2e8";
        String bodyTextColor = isDark ? "#d4d4d8" : "#374151";
        String termColor = isDark ? "#ffffff" : "#111827";
        String sectionColor = isDark ? "#e4e4e7" : "#1f2937";
        String dividerColor = isDark ? "#27272a" : "#e5e7eb";
        String disclaimerBg = isDark ? "#241e15" : "#fffbeb";
        String disclaimerBorder = isDark ? "#d97706" : "#b45309";
        String disclaimerText = isDark ? "#fcd34d" : "#92400e";
        String listBorder = isDark ? "#3f3f46" : "#d1d5db";
        String linkColor = isDark ? "#7aa2f7" : "#2563eb";

        String rawContent = readLicenseFile(item.fileName);
        String formattedBody = formatLicenseBody(rawContent);

        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0,maximum-scale=3.0,user-scalable=yes\">" +
                "<style>" +
                "@font-face{font-family:'Inter';src:url('src/inter_regular.ttf');}" +
                "*{box-sizing:border-box;margin:0;padding:0;}" +
                "body{background:" + bg + ";color:" + bodyTextColor + ";font-family:'Inter',sans-serif;" +
                "font-size:14px;line-height:1.65;padding:16px;-webkit-text-size-adjust:100%;}" +
                ".license-card{max-width:680px;margin:0 auto;}" +
                ".license-hero{background:" + cardBg + ";border:1px solid " + cardBorder + ";" +
                "border-radius:12px;padding:18px 20px;margin-bottom:16px;}" +
                ".license-badge{display:inline-block;padding:3px 9px;font-size:11px;font-weight:700;" +
                "text-transform:uppercase;letter-spacing:0.5px;border-radius:6px;background:" + badgeBg + ";" +
                "color:" + badgeText + ";margin-bottom:8px;}" +
                ".license-title{font-size:19px;font-weight:700;color:" + titleColor + ";margin-bottom:4px;}" +
                ".license-author{font-size:13px;color:" + subColor + ";}" +
                ".license-body{background:" + bodyBg + ";border:1px solid " + bodyBorder + ";" +
                "border-radius:12px;padding:20px;}" +
                ".license-p{margin-bottom:12px;color:" + bodyTextColor + ";word-break:break-word;}" +
                ".license-p:last-child{margin-bottom:0;}" +
                ".license-term{font-weight:700;color:" + termColor + ";}" +
                ".license-heading-primary{font-size:16px;font-weight:700;color:" + titleColor + ";" +
                "margin-top:20px;margin-bottom:10px;padding-bottom:6px;border-bottom:1px solid " + dividerColor + ";}" +
                ".license-heading-section{font-size:14.5px;font-weight:600;color:" + sectionColor + ";" +
                "margin-top:16px;margin-bottom:8px;}" +
                ".license-divider{border:0;border-top:1px solid " + dividerColor + ";margin:16px 0;}" +
                ".license-disclaimer{background:" + disclaimerBg + ";border-left:3px solid " + disclaimerBorder + ";" +
                "color:" + disclaimerText + ";padding:12px 14px;border-radius:0 8px 8px 0;font-size:12.5px;" +
                "line-height:1.55;margin:14px 0;}" +
                ".license-list{margin-bottom:12px;}" +
                ".license-list-item{margin-bottom:8px;padding-left:12px;border-left:2px solid " + listBorder + ";" +
                "color:" + bodyTextColor + ";}" +
                "a{color:" + linkColor + ";text-decoration:underline;}" +
                "</style></head><body>" +
                "<div class=\"license-card\">" +
                "<div class=\"license-hero\">" +
                "<span class=\"license-badge\">" + escapeHtml(item.type) + "</span>" +
                "<h1 class=\"license-title\">" + escapeHtml(item.name) + "</h1>" +
                "<div class=\"license-author\">By " + escapeHtml(item.author) + "</div>" +
                "</div>" +
                "<div class=\"license-body\">" +
                formattedBody +
                "</div>" +
                "</div>" +
                "</body></html>";
    }

    private String formatLicenseBody(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        String text = raw.replace("\r\n", "\n").replace("\r", "\n");
        String[] blocks = text.split("\n\\s*\n+");

        for (String block : blocks) {
            String trimmed = block.trim();
            if (trimmed.isEmpty()) continue;

            // Horizontal divider line e.g. "------..."
            if (trimmed.matches("^[-=_~*]{3,}$")) {
                sb.append("<hr class=\"license-divider\"/>");
                continue;
            }

            // Heading detection
            String[] lines = trimmed.split("\n");
            if (lines.length == 1 || (lines.length <= 2 && isHeadingBlock(trimmed))) {
                if (isPrimaryHeading(trimmed)) {
                    sb.append("<h2 class=\"license-heading-primary\">")
                            .append(escapeHtml(unwrapLines(trimmed)))
                            .append("</h2>");
                    continue;
                } else if (isSectionHeading(trimmed)) {
                    sb.append("<h3 class=\"license-heading-section\">")
                            .append(escapeHtml(unwrapLines(trimmed)))
                            .append("</h3>");
                    continue;
                }
            }

            // All-caps legal disclaimer block
            if (isAllCapsDisclaimer(trimmed)) {
                sb.append("<div class=\"license-disclaimer\">")
                        .append(escapeHtml(unwrapLines(trimmed)))
                        .append("</div>");
                continue;
            }

            // Sub-item list e.g. (a), (b), (c)
            if (hasSubItems(trimmed)) {
                sb.append("<div class=\"license-list\">");
                String[] subItems = splitSubItems(trimmed);
                for (String item : subItems) {
                    if (!item.trim().isEmpty()) {
                        sb.append("<div class=\"license-list-item\">")
                                .append(formatParagraphText(item))
                                .append("</div>");
                    }
                }
                sb.append("</div>");
                continue;
            }

            // Regular paragraph or definition
            sb.append("<p class=\"license-p\">")
                    .append(formatParagraphText(trimmed))
                    .append("</p>");
        }
        return sb.toString();
    }

    private boolean isPrimaryHeading(String block) {
        String trimmed = block.trim();
        return trimmed.equalsIgnoreCase("Apache License")
                || trimmed.startsWith("Apache License")
                || trimmed.startsWith("SIL OPEN FONT LICENSE")
                || trimmed.startsWith("MIT License")
                || trimmed.equals("TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION");
    }

    private boolean isSectionHeading(String block) {
        String trimmed = block.trim();
        if (trimmed.matches("^[0-9]+\\.\\s+[A-Za-z].*")) {
            return true;
        }
        if (trimmed.length() <= 55 && trimmed.matches("^[A-Z0-9 &:,.'\\-]{3,}$") && !trimmed.startsWith("\"")) {
            return true;
        }
        return false;
    }

    private boolean isHeadingBlock(String block) {
        return isPrimaryHeading(block) || isSectionHeading(block);
    }

    private boolean isAllCapsDisclaimer(String block) {
        String trimmed = block.trim();
        if (trimmed.length() > 80) {
            int upperCount = 0;
            int letterCount = 0;
            for (int i = 0; i < trimmed.length(); i++) {
                char c = trimmed.charAt(i);
                if (Character.isLetter(c)) {
                    letterCount++;
                    if (Character.isUpperCase(c)) upperCount++;
                }
            }
            if (letterCount > 50 && ((double) upperCount / letterCount) > 0.85) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSubItems(String block) {
        return block.matches("(?s).*\\n\\s*\\([a-z0-9]+\\)\\s+.*");
    }

    private String[] splitSubItems(String block) {
        return block.split("(?=\\n\\s*\\([a-z0-9]+\\)\\s+)");
    }

    private String unwrapLines(String block) {
        String[] lines = block.split("\n");
        StringBuilder unwrapped = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            if (unwrapped.length() > 0) unwrapped.append(" ");
            unwrapped.append(trimmed);
        }
        return unwrapped.toString();
    }

    private String formatParagraphText(String block) {
        String unwrapped = unwrapLines(block);
        String escaped = escapeHtml(unwrapped);

        // Linkify URLs
        escaped = escaped.replaceAll("(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)", "<a href=\"$1\">$1</a>");

        // Bold definitions in quotes: e.g. &quot;Licensor&quot;
        if (escaped.startsWith("&quot;")) {
            int close = escaped.indexOf("&quot;", 6);
            if (close != -1) {
                String term = escaped.substring(0, close + 6);
                String rest = escaped.substring(close + 6);
                return "<span class=\"license-term\">" + term + "</span>" + rest;
            }
        }
        return escaped;
    }

    private String readLicenseFile(String fileName) {
        try {
            Log.d(TAG, "reading file: licenses/" + fileName);
            InputStream is = getAssets().open("licenses/" + fileName);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            is.close();
            String result = baos.toString("UTF-8");
            Log.d(TAG, "file read OK, size=" + result.length());
            return result;
        } catch (Exception e) {
            Log.e(TAG, "failed to read license file", e);
            return "Could not load license file.";
        }
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private boolean isDarkMode() {
        int mode = ThemeHelper.getSavedThemeMode(this);
        if (mode == AppCompatDelegate.MODE_NIGHT_YES) return true;
        if (mode == AppCompatDelegate.MODE_NIGHT_NO) return false;
        int uiMode = getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    private ArrayList<LicenseItem> loadLicenses() {
        ArrayList<LicenseItem> list = new ArrayList<>();
        try {
            Log.d(TAG, "loading licenses.json");
            InputStream is = getAssets().open("licenses/licenses.json");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            is.close();
            String json = baos.toString("UTF-8");
            Log.d(TAG, "licenses.json loaded, size=" + json.length());
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                LicenseItem item = new LicenseItem();
                item.fileName = obj.getString("fileName");
                item.name = obj.getString("name");
                item.author = obj.getString("author");
                item.type = obj.getString("type");
                list.add(item);
            }
        } catch (Exception e) {
            Log.e(TAG, "failed to load licenses", e);
        }
        return list;
    }

    public static class LicenseItem {
        public String fileName;
        public String name;
        public String author;
        public String type;
    }
}
