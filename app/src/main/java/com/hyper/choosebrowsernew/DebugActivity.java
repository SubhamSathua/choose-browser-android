package com.hyper.choosebrowsernew;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DebugActivity extends AppCompatActivity {

    private static final String PREFS_NAME   = "update_checker_prefs";
    private static final String KEY_JSON     = "cached_json";
    private static final String KEY_FETCH_TS = "last_fetch_time";

    private TextView tvTime, tvJson, tvCacheFlag, tvTempStatus, tvFetchLog;
    private Button clearTempBtn, refetchBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.backgroundPrimary));
            getWindow().setNavigationBarColor(getResources().getColor(R.color.backgroundPrimary));
        }

        setContentView(R.layout.activity_debug);

        findViewById(R.id.debugBackBtn).setOnClickListener(v -> finish());

        Button clearBtn   = findViewById(R.id.debugClearCacheBtn);
        tvTime            = findViewById(R.id.debugLastFetchTime);
        tvJson            = findViewById(R.id.debugRawJson);
        tvCacheFlag       = findViewById(R.id.debugCacheFlag);
        tvTempStatus      = findViewById(R.id.debugTempStatus);
        tvFetchLog        = findViewById(R.id.debugFetchLog);
        clearTempBtn      = findViewById(R.id.debugClearTempBtn);
        refetchBtn        = findViewById(R.id.debugRefetchBtn);
        Button editBtn    = findViewById(R.id.debugEditJsonBtn);

        tvCacheFlag.setText(String.valueOf(DebugConfig.CACHE_UPDATE_JSON));
        tvCacheFlag.setTextColor(DebugConfig.CACHE_UPDATE_JSON
                ? 0xFF4CAF50   // green
                : 0xFFFF9800); // orange

        refreshUi();
        refreshTempStatus();

        clearBtn.setOnClickListener(v -> {
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .remove(KEY_JSON)
                    .remove(KEY_FETCH_TS)
                    .apply();
            Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show();
            refreshUi();
        });

        refetchBtn.setOnClickListener(v -> {
            refetchBtn.setEnabled(false);
            refetchBtn.setText("Fetching…");
            tvFetchLog.setTextColor(0xFFAAAAAA);
            tvFetchLog.setText("Fetching…");
            UpdateChecker.forceFetch(this, (success, json, log) -> {
                tvFetchLog.setText(log);
                tvFetchLog.setTextColor(success ? 0xFF4CAF50 : 0xFFFF5252);
                refetchBtn.setEnabled(true);
                refetchBtn.setText("Re-fetch");
                if (success) refreshUi();
            });
        });

        editBtn.setOnClickListener(v -> showEditJsonDialog());

        clearTempBtn.setOnClickListener(v -> {
            UpdateChecker.tempJson = null;
            refreshTempStatus();
            Toast.makeText(this, "Temp JSON cleared", Toast.LENGTH_SHORT).show();
        });
    }

    private void refreshUi() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String json    = prefs.getString(KEY_JSON, null);
        long fetchTs   = prefs.getLong(KEY_FETCH_TS, 0);

        if (fetchTs > 0) {
            String formatted = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(new Date(fetchTs));
            tvTime.setText(formatted);
        } else {
            tvTime.setText("Never");
        }

        tvJson.setText(json != null ? prettyJson(json) : "No cached JSON");
    }

    private void refreshTempStatus() {
        if (UpdateChecker.tempJson != null) {
            tvTempStatus.setText("Active");
            tvTempStatus.setTextColor(0xFF4CAF50); // green
            clearTempBtn.setVisibility(View.VISIBLE);
        } else {
            tvTempStatus.setText("Inactive");
            tvTempStatus.setTextColor(0xFFFF9800); // orange
            clearTempBtn.setVisibility(View.GONE);
        }
    }

    private void showEditJsonDialog() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String current = UpdateChecker.tempJson != null
                ? UpdateChecker.tempJson
                : prefs.getString(KEY_JSON, "");

        // Pre-fill with pretty-printed JSON so it's readable while editing
        String prefilled = prettyJson(current);

        EditText editText = new EditText(this);
        editText.setText(prefilled);
        editText.setSingleLine(false);
        editText.setMinLines(14);
        editText.setMaxLines(30);
        editText.setVerticalScrollBarEnabled(true);
        editText.setBackgroundColor(0xFF1A1A2E);
        editText.setTextColor(0xFFE0E0E0);
        editText.setTypeface(android.graphics.Typeface.MONOSPACE);
        editText.setTextSize(12f);
        int dp = (int)(getResources().getDisplayMetrics().density * 10);
        editText.setPadding(dp, dp, dp, dp);

        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        sv.addView(editText);
        sv.setBackgroundColor(0xFF1A1A2E);

        new AlertDialog.Builder(this)
                .setTitle("Edit JSON (Temp)")
                .setMessage("Applied immediately. Cleared when app is closed.")
                .setView(sv)
                .setPositiveButton("Apply", (d, w) -> {
                    String newJson = editText.getText().toString().trim();
                    if (newJson.isEmpty()) {
                        Toast.makeText(this, "JSON is empty — not applied", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    UpdateChecker.tempJson = newJson;
                    refreshTempStatus();
                    Toast.makeText(this, "Temp JSON applied", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Pretty-prints JSON with 2-space indentation. Returns original string on parse error. */
    private static String prettyJson(String json) {
        if (json == null || json.isEmpty()) return json;
        try {
            return new JSONObject(json).toString(2);
        } catch (Exception e) {
            return json; // not valid JSON, show as-is
        }
    }

    /** Kept for potential future use — unused now. */
    @SuppressWarnings("unused")
    private static String addLineNumbers(String text) {
        String[] lines = text.split("\n", -1);
        int digits = String.valueOf(lines.length).length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            sb.append(String.format("%" + digits + "d  %s", i + 1, lines[i]));
            if (i < lines.length - 1) sb.append('\n');
        }
        return sb.toString();
    }
}
