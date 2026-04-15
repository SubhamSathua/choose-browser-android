package com.hyper.choosebrowsernew;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Fetches update.json and determines the update priority for this app version.
 *
 * JSON schema:
 * {
 *   "latest":   { "latest_version": "2.25060812", "short_msg": "...", "md_file": "https://..." },
 *   "critical": { "below": "2.25050100", "short_msg": "...", "md_file": "https://..." },
 *   "warning":  { "below": "2.25060100", "short_msg": "...", "md_file": "https://..." }
 * }
 *
 * Caching: if online && cache < 6h → use cache; if online && cache expired → fetch new;
 *          if offline → use cache.
 *
 * Priority order: CRITICAL → WARNING → LATEST → UP_TO_DATE
 */
public class UpdateChecker {

    private static final String UPDATE_URL =
            "https://raw.githubusercontent.com/Subham-x/Choose-Browser-Android/refs/heads/main/updates/update.json";

    private static final String PREFS_NAME   = "update_checker_prefs";
    private static final String KEY_JSON     = "cached_json";
    private static final String KEY_FETCH_TS = "last_fetch_time";
    /** 6 hours in milliseconds */
    private static final long CACHE_MAX_AGE  = 6 * 60 * 60 * 1000L;

    public enum Priority { CRITICAL, WARNING, LATEST, UP_TO_DATE, ERROR }

    public static class Result {
        public final Priority priority;
        public final String shortMsg;
        public final String mdFileUrl;
        public final String latestVersion;

        Result(Priority priority, String shortMsg, String mdFileUrl, String latestVersion) {
            this.priority = priority;
            this.shortMsg = shortMsg;
            this.mdFileUrl = mdFileUrl;
            this.latestVersion = latestVersion;
        }
    }

    public interface Callback {
        void onResult(Result result);
    }

    private static final Executor EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler  MAIN     = new Handler(Looper.getMainLooper());

    /**
     * In-memory temporary JSON override (debug only).
     * Set from DebugActivity; cleared automatically when the process dies (app closed).
     */
    public static volatile String tempJson = null;

    // ── Public API ────────────────────────────────────────────────────

    /**
     * Synchronous cached result — safe to call on the main thread.
     * Returns UP_TO_DATE if no cache exists yet.
     */
    public static Result getCachedResult(Context ctx) {
        if (tempJson != null) return evaluate(tempJson, getAppVersion(ctx));
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_JSON, null);
        if (json == null) return new Result(Priority.UP_TO_DATE, null, null, null);
        return evaluate(json, getAppVersion(ctx));
    }

    /**
     * Async check that respects the 6-hour cache window.
     * Result delivered on the main thread.
     */
    public static void check(Context ctx, Callback callback) {
        final String appVersion = getAppVersion(ctx);

        EXECUTOR.execute(() -> {
            Result result;
            try {
                // Temp override takes priority — no cache/network needed
                if (tempJson != null) {
                    final Result r = evaluate(tempJson, appVersion);
                    MAIN.post(() -> callback.onResult(r));
                    return;
                }

                SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                String cachedJson = prefs.getString(KEY_JSON, null);
                long lastFetch    = prefs.getLong(KEY_FETCH_TS, 0);
                long cacheAge     = System.currentTimeMillis() - lastFetch;

                String json;
                if (!isOnline(ctx)) {
                    // Offline → use whatever is in cache
                    json = cachedJson;
                } else if (DebugConfig.CACHE_UPDATE_JSON && cachedJson != null && cacheAge < CACHE_MAX_AGE) {
                    // Online but cache is fresh (caching enabled)
                    json = cachedJson;
                } else {
                    // Online and: cache disabled, cache stale, or cache missing → fetch
                    json = fetchJson();
                    prefs.edit()
                            .putString(KEY_JSON, json)
                            .putLong(KEY_FETCH_TS, System.currentTimeMillis())
                            .apply();
                }

                result = (json != null) ? evaluate(json, appVersion)
                                        : new Result(Priority.ERROR, null, null, null);
            } catch (Exception e) {
                result = new Result(Priority.ERROR, null, null, null);
            }

            final Result finalResult = result;
            MAIN.post(() -> callback.onResult(finalResult));
        });
    }

    // ── Internal helpers ──────────────────────────────────────────────

    private static boolean isOnline(Context ctx) {
        try {
            ConnectivityManager cm =
                    (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            Network net = cm.getActiveNetwork();
            if (net == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(net);
            return caps != null
                    && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } catch (Exception e) {
            return false;
        }
    }

    private static String getAppVersion(Context ctx) {
        try {
            return ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "0";
        }
    }

    private static String fetchJson() throws Exception {
        URL url = new URL(UPDATE_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    static Result evaluate(String json, String appVersion) {
        try {
            JSONObject root = new JSONObject(json);

            // --- CRITICAL ---
            JSONObject critical = root.optJSONObject("critical");
            if (critical != null) {
                String below = critical.optString("below", null);
                if (below != null && compareVersions(appVersion, below) < 0) {
                    return new Result(Priority.CRITICAL,
                            critical.optString("short_msg", "Critical update required"),
                            critical.optString("md_file", null),
                            null);
                }
            }

            // --- WARNING ---
            JSONObject warning = root.optJSONObject("warning");
            if (warning != null) {
                String below = warning.optString("below", null);
                if (below != null && compareVersions(appVersion, below) < 0) {
                    return new Result(Priority.WARNING,
                            warning.optString("short_msg", "Update recommended"),
                            warning.optString("md_file", null),
                            null);
                }
            }

            // --- LATEST ---
            JSONObject latest = root.optJSONObject("latest");
            if (latest != null) {
                String latestVer = latest.optString("latest_version", null);
                if (latestVer != null && compareVersions(appVersion, latestVer) < 0) {
                    return new Result(Priority.LATEST,
                            latest.optString("short_msg", "New version available"),
                            latest.optString("md_file", null),
                            latestVer);
                }
            }

            return new Result(Priority.UP_TO_DATE, null, null, null);

        } catch (Exception e) {
            return new Result(Priority.ERROR, null, null, null);
        }
    }

    /**
     * Compare version strings like "2.25060812".
     * Returns <0 if v1 < v2, 0 if equal, >0 if v1 > v2.
     */
    static int compareVersions(String v1, String v2) {
        if (v1 == null) v1 = "0";
        if (v2 == null) v2 = "0";
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int len = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < len; i++) {
            long n1 = 0, n2 = 0;
            if (i < parts1.length) {
                try { n1 = Long.parseLong(parts1[i]); } catch (NumberFormatException ignored) {}
            }
            if (i < parts2.length) {
                try { n2 = Long.parseLong(parts2[i]); } catch (NumberFormatException ignored) {}
            }
            if (n1 != n2) return Long.compare(n1, n2);
        }
        return 0;
    }

    // ── Debug: forced fetch with full diagnostics ─────────────────────

    public interface FetchDiagnosticCallback {
        /** Called on main thread. log contains human-readable step-by-step info. */
        void onResult(boolean success, String json, String log);
    }

    /**
     * Always hits the network regardless of cache/flag settings.
     * Captures HTTP status, error body, and exception details for display in DebugActivity.
     * On success, also saves result into the normal cache.
     */
    public static void forceFetch(Context ctx, FetchDiagnosticCallback callback) {
        EXECUTOR.execute(() -> {
            StringBuilder log = new StringBuilder();
            boolean success = false;
            String resultJson = null;

            // Step 1 – connectivity
            boolean online = isOnline(ctx);
            log.append("[Network] ").append(online ? "ONLINE" : "OFFLINE").append("\n");
            if (!online) {
                log.append("[Error] No active network connection — cannot fetch.");
                final String l = log.toString();
                MAIN.post(() -> callback.onResult(false, null, l));
                return;
            }

            // Step 2 – HTTP request
            log.append("[URL] ").append(UPDATE_URL).append("\n");
            HttpURLConnection conn = null;
            try {
                URL url = new URL(UPDATE_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.connect();

                int code = conn.getResponseCode();
                log.append("[HTTP] ").append(code).append(" ").append(conn.getResponseMessage()).append("\n");

                if (code == 200) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();
                    resultJson = sb.toString();

                    // Step 3 – validate JSON
                    try {
                        new JSONObject(resultJson);
                        log.append("[JSON] Valid JSON, ").append(resultJson.length()).append(" chars\n");
                    } catch (Exception je) {
                        log.append("[JSON] Parse error: ").append(je.getMessage()).append("\n");
                        log.append("[Error] Response is not valid JSON.");
                        final String l = log.toString();
                        MAIN.post(() -> callback.onResult(false, null, l));
                        return;
                    }

                    // Step 4 – save to cache
                    ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            .edit()
                            .putString(KEY_JSON, resultJson)
                            .putLong(KEY_FETCH_TS, System.currentTimeMillis())
                            .apply();
                    log.append("[Cache] Saved to SharedPrefs\n");
                    log.append("[Result] SUCCESS");
                    success = true;

                } else {
                    // Read error stream for more details
                    try {
                        if (conn.getErrorStream() != null) {
                            BufferedReader errReader = new BufferedReader(
                                    new InputStreamReader(conn.getErrorStream()));
                            StringBuilder errSb = new StringBuilder();
                            String errLine;
                            while ((errLine = errReader.readLine()) != null) errSb.append(errLine);
                            errReader.close();
                            if (errSb.length() > 0) {
                                log.append("[Server Body] ").append(errSb).append("\n");
                            }
                        }
                    } catch (Exception ignored) {}
                    log.append("[Error] Server returned HTTP ").append(code);
                }

            } catch (Exception e) {
                log.append("[Exception] ").append(e.getClass().getSimpleName())
                   .append(": ").append(e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }

            final boolean fs = success;
            final String fj = resultJson;
            final String fl = log.toString();
            MAIN.post(() -> callback.onResult(fs, fj, fl));
        });
    }
}
