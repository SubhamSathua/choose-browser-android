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
        String textColor = isDark ? "#d4d4d8" : "#222226";
        String linkColor = isDark ? "#7aa2f7" : "#2563eb";
        String hrColor = isDark ? "#2c2c30" : "#d8d8de";

        String rawContent = readLicenseFile(item.fileName);
        String formatted = formatCleanText(rawContent);

        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0,maximum-scale=3.0,user-scalable=yes\">" +
                "<style>" +
                "@font-face{font-family:'Inter';src:url('src/inter_regular.ttf');}" +
                "*{box-sizing:border-box;margin:0;padding:0;}" +
                "body{background:" + bg + ";color:" + textColor + ";font-family:'Inter',sans-serif;" +
                "font-size:14px;line-height:1.65;padding:16px 18px;-webkit-text-size-adjust:100%;word-break:break-word;}" +
                ".content-wrap{max-width:700px;margin:0 auto;}" +
                "p{margin-bottom:14px;text-align:justify;text-justify:inter-word;-webkit-hyphens:auto;hyphens:auto;}" +
                "p:last-child{margin-bottom:0;}" +
                "a{color:" + linkColor + ";text-decoration:underline;}" +
                "hr{border:0;border-top:1px solid " + hrColor + ";margin:16px 0;}" +
                "</style></head><body>" +
                "<div class=\"content-wrap\">" +
                formatted +
                "</div></body></html>";
    }

    private String formatCleanText(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        String text = raw.replace("\r\n", "\n").replace("\r", "\n");
        String[] blocks = text.split("\n\\s*\n+");
        StringBuilder sb = new StringBuilder();

        for (String block : blocks) {
            String trimmed = block.trim();
            if (trimmed.isEmpty()) continue;

            // Horizontal divider line
            if (trimmed.matches("^[-=_~*]{3,}$")) {
                sb.append("<hr/>");
                continue;
            }

            String[] lines = trimmed.split("\n");
            if (lines.length == 1) {
                sb.append("<p>").append(escapeAndLinkify(trimmed)).append("</p>");
            } else {
                // Check if short header lines (e.g. title block)
                boolean isHeaderLines = true;
                for (String line : lines) {
                    if (line.trim().length() > 50) {
                        isHeaderLines = false;
                        break;
                    }
                }

                if (isHeaderLines) {
                    StringBuilder p = new StringBuilder();
                    for (int i = 0; i < lines.length; i++) {
                        if (i > 0) p.append("<br/>");
                        p.append(escapeAndLinkify(lines[i].trim()));
                    }
                    sb.append("<p>").append(p).append("</p>");
                } else {
                    // Reflowable paragraph: unwrap mid-sentence line breaks
                    StringBuilder p = new StringBuilder();
                    for (String line : lines) {
                        String lineTrim = line.trim();
                        if (lineTrim.isEmpty()) continue;

                        if (lineTrim.matches("^(\\([a-z0-9]+\\)|[0-9]+\\.|[*\\-])\\s+.*") && p.length() > 0) {
                            p.append("<br/><br/>").append(escapeAndLinkify(lineTrim));
                        } else {
                            if (p.length() > 0 && !p.toString().endsWith("<br/>") && !p.toString().endsWith("<br/><br/>")) {
                                p.append(" ");
                            }
                            p.append(escapeAndLinkify(lineTrim));
                        }
                    }
                    sb.append("<p>").append(p).append("</p>");
                }
            }
        }
        return sb.toString();
    }

    private String escapeAndLinkify(String s) {
        if (s == null) return "";
        String escaped = s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
        return escaped.replaceAll("(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)", "<a href=\"$1\">$1</a>");
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
