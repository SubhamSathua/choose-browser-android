package com.hyper.choosebrowsernew.ui.licenses;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int bgColor = getResources().getColor(R.color.backgroundPrimary);
            getWindow().setStatusBarColor(bgColor);
            getWindow().setNavigationBarColor(bgColor);
        }

        setContentView(R.layout.activity_license_list);

        findViewById(R.id.licenseListBackBtn).setOnClickListener(v -> finish());

        licenseList = loadLicenses();
        Log.d(TAG, "loaded " + licenseList.size() + " licenses");

        ListView listView = findViewById(R.id.licenseListView);
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

    private String buildLicenseHtml(LicenseItem item) {
        boolean isDark = isDarkMode();
        Log.d(TAG, "buildLicenseHtml isDark=" + isDark);
        String bg = isDark ? "#1c1c1e" : "#f1f1f3";
        String card = isDark ? "#2c2c2e" : "#ffffff";
        String text = isDark ? "#f2f2f7" : "#000000";
        String sub = isDark ? "#aeaeb2" : "#575757";

        String content = readLicenseFile(item.fileName);
        Log.d(TAG, "readLicenseFile returned " + (content != null ? content.length() + " chars" : "null"));

        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\">" +
                "<style>" +
                "@font-face{font-family:'Inter';src:url('src/inter_regular.ttf');}" +
                "*{box-sizing:border-box;margin:0;padding:0;}" +
                "body{background:" + bg + ";color:" + text + ";font-family:'Inter',sans-serif;" +
                "font-size:15px;line-height:1.7;padding:20px 16px 40px;}" +
                "h1{font-size:22px;font-weight:600;color:" + text + ";margin-bottom:8px;}" +
                ".sub{color:" + sub + ";font-size:13px;margin-bottom:24px;}" +
                ".content{background:" + card + ";border-radius:15px;padding:20px;" +
                "white-space:pre-wrap;word-wrap:break-word;font-size:14px;line-height:1.6;}" +
                "</style></head><body>" +
                "<h1>" + escapeHtml(item.name) + "</h1>" +
                "<p class=\"sub\">License</p>" +
                "<div class=\"content\">" + escapeHtml(content) + "</div>" +
                "</body></html>";
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
            e.printStackTrace();
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
