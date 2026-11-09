package com.hyper.choosebrowsernew.ui.debug;

import android.content.Intent;
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

import com.hyper.choosebrowsernew.R;
import com.hyper.choosebrowsernew.UpdateChecker;
import com.hyper.choosebrowsernew.ui.chooser.BrowserChooserBottomSheet;
import com.hyper.choosebrowsernew.util.ThemeHelper;

import android.view.LayoutInflater;
import androidx.core.content.ContextCompat;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DebugActivity extends AppCompatActivity {

    private static final String PREFS_NAME   = "update_checker_prefs";
    private static final String KEY_JSON     = "cached_json";
    private static final String KEY_FETCH_TS = "last_fetch_time";

    private TextView tvTime, tvJson, tvCacheFlag, tvTempStatus, tvFetchLog, tvJsonToggle, tvJsonLineCount;
    private Button clearTempBtn, refetchBtn;
    private boolean isJsonExpanded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.applySystemBars(this);

        setContentView(R.layout.debug_activity);

        findViewById(R.id.debugBackBtn).setOnClickListener(v -> finish());

        View hideBtn = findViewById(R.id.debugHideBtn);
        if (hideBtn != null) {
            if (!DebugConfig.isAlpha()) {
                hideBtn.setVisibility(View.VISIBLE);
                hideBtn.setOnClickListener(v -> {
                    DebugConfig.hideDebug(this);
                    Toast.makeText(this, "Debug menu hidden", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } else {
                hideBtn.setVisibility(View.GONE);
            }
        }

        View cardAppLinks = findViewById(R.id.cardDefaultAppLinks);
        View btnAppLinks = findViewById(R.id.debugOpenAppLinksBtn);
        View.OnClickListener openAppLinksListener = v ->
                startActivity(new Intent(this, DefaultAppLinksActivity.class));
        if (cardAppLinks != null) cardAppLinks.setOnClickListener(openAppLinksListener);
        if (btnAppLinks != null) btnAppLinks.setOnClickListener(openAppLinksListener);

        Button clearBtn   = findViewById(R.id.debugClearCacheBtn);
        tvTime            = findViewById(R.id.debugLastFetchTime);
        tvJson            = findViewById(R.id.debugRawJson);
        tvJsonToggle      = findViewById(R.id.debugJsonToggle);
        tvJsonLineCount   = findViewById(R.id.debugJsonLineCount);
        tvCacheFlag       = findViewById(R.id.debugCacheFlag);
        tvTempStatus      = findViewById(R.id.debugTempStatus);
        tvFetchLog        = findViewById(R.id.debugFetchLog);
        clearTempBtn      = findViewById(R.id.debugClearTempBtn);
        refetchBtn        = findViewById(R.id.debugRefetchBtn);
        Button editBtn    = findViewById(R.id.debugEditJsonBtn);

        TextView copyJsonBtn = findViewById(R.id.debugCopyJsonBtn);
        if (copyJsonBtn != null) {
            copyJsonBtn.setOnClickListener(v -> copyJsonToClipboard());
        }

        if (tvJsonToggle != null) {
            tvJsonToggle.setOnClickListener(v -> {
                isJsonExpanded = !isJsonExpanded;
                applyJsonCollapseState();
            });
        }

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

        setupSampleLinksTester();
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

        String rawContent = (UpdateChecker.tempJson != null)
                ? UpdateChecker.tempJson
                : json;
        String formatted = (rawContent != null) ? prettyJson(rawContent) : "No cached JSON";
        tvJson.setText(formatted);
        updateJsonCollapse(formatted);
    }

    private void copyJsonToClipboard() {
        String text = (tvJson != null) ? tvJson.getText().toString() : "";
        if (!text.isEmpty() && !"No cached JSON".equals(text)) {
            android.content.ClipboardManager cm =
                    (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(android.content.ClipData.newPlainText("JSON", text));
                Toast.makeText(this, "JSON copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateJsonCollapse(String text) {
        if (text == null || text.isEmpty() || "No cached JSON".equals(text)) {
            if (tvJsonToggle != null) tvJsonToggle.setVisibility(View.GONE);
            if (tvJsonLineCount != null) tvJsonLineCount.setVisibility(View.GONE);
            tvJson.setMaxLines(Integer.MAX_VALUE);
            tvJson.setEllipsize(null);
            return;
        }

        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') count++;
        }
        count++; // including last line

        if (tvJsonLineCount != null) {
            tvJsonLineCount.setVisibility(View.VISIBLE);
            tvJsonLineCount.setText("(" + count + " lines)");
        }

        if (count > 7) {
            if (tvJsonToggle != null) tvJsonToggle.setVisibility(View.VISIBLE);
            applyJsonCollapseState();
        } else {
            if (tvJsonToggle != null) tvJsonToggle.setVisibility(View.GONE);
            tvJson.setMaxLines(Integer.MAX_VALUE);
            tvJson.setEllipsize(null);
        }
    }

    private void applyJsonCollapseState() {
        if (tvJson == null) return;
        if (isJsonExpanded) {
            tvJson.setMaxLines(Integer.MAX_VALUE);
            tvJson.setEllipsize(null);
            if (tvJsonToggle != null) {
                tvJsonToggle.setText("Show less");
                tvJsonToggle.setCompoundDrawablesWithIntrinsicBounds(null, null,
                        ContextCompat.getDrawable(this, R.drawable.ic_expand_less), null);
            }
        } else {
            tvJson.setMaxLines(7);
            tvJson.setEllipsize(android.text.TextUtils.TruncateAt.END);
            if (tvJsonToggle != null) {
                tvJsonToggle.setText("Show more");
                tvJsonToggle.setCompoundDrawablesWithIntrinsicBounds(null, null,
                        ContextCompat.getDrawable(this, R.drawable.ic_expand_more), null);
            }
        }
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
        refreshUi();
    }

    private void showEditJsonDialog() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String current = UpdateChecker.tempJson != null
                ? UpdateChecker.tempJson
                : prefs.getString(KEY_JSON, "");

        String prefilled = prettyJson(current);

        View dialogView = LayoutInflater.from(this).inflate(R.layout.debug_dialog_edit_json, null);
        EditText input = dialogView.findViewById(R.id.dialogJsonInput);
        TextView errorBanner = dialogView.findViewById(R.id.dialogJsonErrorBanner);
        TextView status = dialogView.findViewById(R.id.dialogJsonStatus);
        TextView formatBtn = dialogView.findViewById(R.id.dialogJsonFormatBtn);
        TextView resetBtn = dialogView.findViewById(R.id.dialogJsonResetBtn);
        TextView cancelBtn = dialogView.findViewById(R.id.dialogJsonCancelBtn);
        View applyBtn = dialogView.findViewById(R.id.dialogJsonApplyBtn);

        input.setText(prefilled);
        if (status != null) {
            status.setText(UpdateChecker.tempJson != null ? "TEMP ACTIVE" : "CACHED");
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        formatBtn.setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            if (text.isEmpty()) return;
            try {
                String formatted = new JSONObject(text).toString(2).replace("\\/", "/");
                input.setText(formatted);
                input.setSelection(formatted.length());
                errorBanner.setVisibility(View.GONE);
                Toast.makeText(this, "Formatted JSON", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                errorBanner.setVisibility(View.VISIBLE);
                errorBanner.setText("Format error: " + e.getMessage());
            }
        });

        resetBtn.setOnClickListener(v -> {
            String cached = prefs.getString(KEY_JSON, "");
            String formatted = prettyJson(cached);
            input.setText(formatted);
            errorBanner.setVisibility(View.GONE);
            Toast.makeText(this, "Reset to cached JSON", Toast.LENGTH_SHORT).show();
        });

        cancelBtn.setOnClickListener(v -> dialog.dismiss());

        applyBtn.setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            if (text.isEmpty()) {
                errorBanner.setVisibility(View.VISIBLE);
                errorBanner.setText("JSON cannot be empty");
                return;
            }
            try {
                // Strict validation
                JSONObject parsed = new JSONObject(text);
                String clean = parsed.toString(2).replace("\\/", "/");
                UpdateChecker.tempJson = clean;
                refreshTempStatus();
                Toast.makeText(this, "Temp JSON applied", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } catch (Exception e) {
                errorBanner.setVisibility(View.VISIBLE);
                errorBanner.setText("Invalid JSON: " + e.getMessage());
            }
        });

        dialog.show();
    }

    /** Pretty-prints JSON with 2-space indentation and unescapes forward slashes. */
    private static String prettyJson(String json) {
        if (json == null || json.isEmpty()) return json;
        try {
            return new JSONObject(json).toString(2).replace("\\/", "/");
        } catch (Exception e) {
            return json.replace("\\/", "/");
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
    private void setupSampleLinksTester() {
        bindLinkTester(R.id.btnTestGoogleDrive, "https://drive.google.com");
        bindLinkTester(R.id.btnTestGoogleMaps, "https://maps.google.com");
        bindLinkTester(R.id.btnTestYouTubeMusic, "https://music.youtube.com");
        bindLinkTester(R.id.btnTestGmail, "mailto:support@example.com");
        bindLinkTester(R.id.btnTestYouTube, "https://www.youtube.com");

        EditText etCustom = findViewById(R.id.etDebugCustomUrl);
        View btnCustom = findViewById(R.id.btnDebugTestCustomUrl);
        if (btnCustom != null && etCustom != null) {
            btnCustom.setOnClickListener(v -> {
                String input = etCustom.getText() != null ? etCustom.getText().toString().trim() : "";
                if (!input.isEmpty()) {
                    triggerChooserPopup(input);
                } else {
                    Toast.makeText(this, "Enter a URL to test", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void bindLinkTester(int viewId, String url) {
        View view = findViewById(viewId);
        if (view != null) {
            view.setOnClickListener(v -> triggerChooserPopup(url));
        }
    }

    private void triggerChooserPopup(String url) {
        BrowserChooserBottomSheet chooser = BrowserChooserBottomSheet.newInstance(url);
        chooser.show(getSupportFragmentManager(), "browser_chooser");
    }
}
