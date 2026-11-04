package com.hyper.choosebrowsernew;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;

import com.hyper.choosebrowsernew.ui.debug.DebugConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Fetches update.json and determines the update priority according to RFC-042 and multi-ring architecture.
 *
 * Supports:
 * - Emergency Kill-Switches (global and ring-specific: alpha, beta, stable).
 * - Multi-ring distribution (stable, beta, alpha).
 * - Minimum supported build floor enforcement (minSupportedVersionCode / minVersion).
 * - Direct APK, GitHub, Obtainium, and F-Droid download sources.
 * - SHA-256 cryptographic verification.
 * - Changelog lists and markdown links.
 */
public class UpdateChecker {

    public static final String PRIMARY_URL = AppConstantsDetails.UPDATE_JSON_URL;
    public static final String FALLBACK_URL = AppConstantsDetails.UPDATE_JSON_FALLBACK_URL;

    private static final String PREFS_NAME = "update_checker_prefs";
    private static final String KEY_JSON = "cached_json";
    private static final String KEY_FETCH_TS = "last_fetch_time";
    private static final String KEY_CHANNEL = "update_channel"; // "stable", "beta", "alpha"

    /** 6 hours in milliseconds */
    private static final long CACHE_MAX_AGE = 6 * 60 * 60 * 1000L;

    public enum Priority {
        KILL_SWITCH,
        CRITICAL,
        WARNING,
        LATEST,
        UP_TO_DATE,
        ERROR
    }

    public static class Result {
        public final Priority priority;
        public final String title;
        public final String shortMsg;
        public final String mdFileUrl;
        public final String latestVersion;
        public final int latestVersionCode;
        public final String releaseDate;
        public final List<String> changelog;
        public final String directApkUrl;
        public final String githubUrl;
        public final String obtainiumUrl;
        public final String fdroidUrl;
        public final String sha256;
        public final boolean isBlocking;

        public Result(Priority priority, String title, String shortMsg, String mdFileUrl,
                      String latestVersion, int latestVersionCode, String releaseDate,
                      List<String> changelog, String directApkUrl, String githubUrl,
                      String obtainiumUrl, String fdroidUrl, String sha256, boolean isBlocking) {
            this.priority = priority;
            this.title = title;
            this.shortMsg = shortMsg;
            this.mdFileUrl = mdFileUrl;
            this.latestVersion = latestVersion;
            this.latestVersionCode = latestVersionCode;
            this.releaseDate = releaseDate;
            this.changelog = changelog != null ? Collections.unmodifiableList(changelog) : Collections.emptyList();
            this.directApkUrl = directApkUrl;
            this.githubUrl = githubUrl;
            this.obtainiumUrl = obtainiumUrl;
            this.fdroidUrl = fdroidUrl;
            this.sha256 = sha256;
            this.isBlocking = isBlocking;
        }

        public Result(Priority priority, String shortMsg, String mdFileUrl, String latestVersion) {
            this(priority, null, shortMsg, mdFileUrl, latestVersion, 0, null, Collections.emptyList(),
                    null, null, null, null, null, priority == Priority.CRITICAL || priority == Priority.KILL_SWITCH);
        }
    }

    public interface Callback {
        void onResult(Result result);
    }

    private static final Executor EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    /**
     * In-memory temporary JSON override (debug only).
     */
    public static volatile String tempJson = null;

    // ── Public API ────────────────────────────────────────────────────

    public static String getSelectedChannel(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_CHANNEL, "stable");
    }

    public static void setSelectedChannel(Context ctx, String channel) {
        if (channel == null || (!channel.equals("stable") && !channel.equals("beta") && !channel.equals("alpha"))) {
            channel = "stable";
        }
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CHANNEL, channel)
                .apply();
    }

    public static Result getCachedResult(Context ctx) {
        String appVersion = getAppVersion(ctx);
        int appVersionCode = getAppVersionCode(ctx);
        String channel = getSelectedChannel(ctx);

        if (tempJson != null) return evaluate(tempJson, appVersion, appVersionCode, channel);
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_JSON, null);
        if (json == null) return new Result(Priority.UP_TO_DATE, "Up to Date", null, null, appVersion, appVersionCode, null, null, null, null, null, null, null, false);
        return evaluate(json, appVersion, appVersionCode, channel);
    }

    public static void check(Context ctx, Callback callback) {
        final String appVersion = getAppVersion(ctx);
        final int appVersionCode = getAppVersionCode(ctx);
        final String channel = getSelectedChannel(ctx);

        EXECUTOR.execute(() -> {
            Result result;
            try {
                if (tempJson != null) {
                    final Result r = evaluate(tempJson, appVersion, appVersionCode, channel);
                    MAIN.post(() -> callback.onResult(r));
                    return;
                }

                SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                String cachedJson = prefs.getString(KEY_JSON, null);
                long lastFetch = prefs.getLong(KEY_FETCH_TS, 0);
                long cacheAge = System.currentTimeMillis() - lastFetch;

                String json;
                if (!isOnline(ctx)) {
                    json = cachedJson;
                } else if (DebugConfig.CACHE_UPDATE_JSON && cachedJson != null && cacheAge < CACHE_MAX_AGE) {
                    json = cachedJson;
                } else {
                    json = fetchJson();
                    if (json != null) {
                        prefs.edit()
                                .putString(KEY_JSON, json)
                                .putLong(KEY_FETCH_TS, System.currentTimeMillis())
                                .apply();
                    }
                }

                result = (json != null) ? evaluate(json, appVersion, appVersionCode, channel)
                        : new Result(Priority.ERROR, null, null, null);
            } catch (Exception e) {
                result = new Result(Priority.ERROR, null, null, null);
            }

            final Result finalResult = result;
            MAIN.post(() -> callback.onResult(finalResult));
        });
    }

    // ── Internal helpers ──────────────────────────────────────────────

    public static boolean isOnline(Context ctx) {
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

    public static String getAppVersion(Context ctx) {
        try {
            return ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "0";
        }
    }

    public static int getAppVersionCode(Context ctx) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                return (int) ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).getLongVersionCode();
            } else {
                return ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionCode;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    private static String fetchJson() {
        // Try Primary URL (GitHub Pages)
        try {
            String primary = fetchHttp(PRIMARY_URL);
            if (primary != null && !primary.trim().isEmpty()) {
                return primary;
            }
        } catch (Exception ignored) {
        }

        // Fallback to Raw GitHub URL
        try {
            return fetchHttp(FALLBACK_URL);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String fetchHttp(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("Accept", "application/json");
        try {
            int code = conn.getResponseCode();
            if (code != 200) {
                throw new Exception("HTTP " + code);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Backward compatible evaluate with version string.
     */
    public static Result evaluate(String json, String appVersion) {
        return evaluate(json, appVersion, 0, "stable");
    }

    /**
     * Complete RFC-042 multi-ring evaluation.
     */
    public static Result evaluate(String json, String appVersion, int appVersionCode, String channel) {
        if (channel == null || channel.isEmpty()) channel = "stable";
        android.util.Log.d("UpdateChecker", "Evaluating update. App: " + appVersion + " (" + appVersionCode + "), Channel: " + channel);

        try {
            JSONObject root = new JSONObject(json);

            // ── 1. Kill-Switch Hierarchy ──────────────────────────────────
            JSONObject killSwitch = root.optJSONObject("killSwitch");
            if (killSwitch != null) {
                // Global Kill-Switch
                JSONObject globalKs = killSwitch.optJSONObject("global");
                if (globalKs != null && globalKs.optBoolean("enabled", false)) {
                    String title = globalKs.optString("title", "Service Temporarily Suspended");
                    String msg = globalKs.optString("message", "All app operations are temporarily paused for maintenance.");
                    android.util.Log.w("UpdateChecker", "Global kill-switch active: " + title);
                    return new Result(Priority.KILL_SWITCH, title, msg, null, appVersion, appVersionCode, null, null, null, null, null, null, null, true);
                }

                // Channel Kill-Switch
                JSONObject channelKs = killSwitch.optJSONObject(channel);
                if (channelKs != null && channelKs.optBoolean("enabled", false)) {
                    String title = channelKs.optString("title", "Channel Temporarily Paused");
                    String msg = channelKs.optString("message", "This release ring is temporarily paused while an update is prepared.");
                    android.util.Log.w("UpdateChecker", "Channel kill-switch active for " + channel + ": " + title);
                    return new Result(Priority.KILL_SWITCH, title, msg, null, appVersion, appVersionCode, null, null, null, null, null, null, null, true);
                }
            }

            // ── 2. Ring Resolution ────────────────────────────────────────
            JSONObject ring = root.optJSONObject(channel);
            if (ring == null && !"stable".equals(channel)) {
                ring = root.optJSONObject("stable");
            }

            if (ring == null) {
                // Check if old legacy schema
                if (root.has("latest") || root.has("critical") || root.has("warning")) {
                    return evaluateLegacy(root, appVersion);
                }
                return new Result(Priority.UP_TO_DATE, "Up to Date", null, null, appVersion, appVersionCode, null, null, null, null, null, null, null, false);
            }

            int remoteVersionCode = ring.optInt("versionCode", 0);
            String remoteVersionName = ring.optString("versionName", ring.optString("version", null));
            int minSupportedCode = ring.optInt("minSupportedVersionCode", ring.optInt("minVersionCode", 0));
            String minVersionName = ring.optString("minVersion", null);
            String releaseDate = ring.optString("releaseDate", null);
            String updateType = ring.optString("updateType", "OPTIONAL").toUpperCase();
            String deprecationMsg = ring.optString("deprecationMessage", "Your installed version has reached end-of-life. Please update to continue.");
            String mdFileUrl = ring.optString("mdFileUrl", ring.optString("md_file", "changes.md"));
            String sha256 = ring.optString("sha256", null);

            List<String> changelog = new ArrayList<>();
            JSONArray changelogArr = ring.optJSONArray("changelog");
            if (changelogArr != null) {
                for (int i = 0; i < changelogArr.length(); i++) {
                    changelog.add(changelogArr.optString(i));
                }
            }

            JSONObject downloads = ring.optJSONObject("downloads");
            String directApk = downloads != null ? downloads.optString("directApk", null) : null;
            String github = downloads != null ? downloads.optString("github", null) : null;
            String obtainium = downloads != null ? downloads.optString("obtainium", null) : null;
            String fdroid = downloads != null ? downloads.optString("fdroid", null) : null;

            // ── 3. Minimum Supported Floor Deprecation ─────────────────────
            boolean isFloorDeprecated = false;
            if (minSupportedCode > 0 && appVersionCode > 0) {
                isFloorDeprecated = (appVersionCode < minSupportedCode);
            } else if (minVersionName != null && appVersion != null) {
                isFloorDeprecated = (compareVersions(appVersion, minVersionName) < 0);
            }

            if (isFloorDeprecated) {
                android.util.Log.w("UpdateChecker", "App is below minimum supported floor: " + minSupportedCode + " / " + minVersionName);
                return new Result(Priority.CRITICAL, "Critical Update Required", deprecationMsg, mdFileUrl,
                        remoteVersionName, remoteVersionCode, releaseDate, changelog,
                        directApk, github, obtainium, fdroid, sha256, true);
            }

            // ── 4. Remote Version Comparison ──────────────────────────────
            boolean isNewerAvailable = false;
            if (remoteVersionCode > 0 && appVersionCode > 0) {
                isNewerAvailable = (remoteVersionCode > appVersionCode);
            } else if (remoteVersionName != null && appVersion != null) {
                isNewerAvailable = (compareVersions(appVersion, remoteVersionName) < 0);
            }

            if (isNewerAvailable) {
                Priority p;
                String title;
                boolean blocking = false;

                if ("CRITICAL".equals(updateType)) {
                    p = Priority.CRITICAL;
                    title = "Critical Update Required";
                    blocking = true;
                } else if ("RECOMMENDED".equals(updateType)) {
                    p = Priority.WARNING;
                    title = "Update Recommended";
                } else {
                    p = Priority.LATEST;
                    title = "Update Available";
                }

                String displayMsg = (!changelog.isEmpty()) ? changelog.get(0) : "New version available";
                return new Result(p, title, displayMsg, mdFileUrl, remoteVersionName, remoteVersionCode,
                        releaseDate, changelog, directApk, github, obtainium, fdroid, sha256, blocking);
            }

            // ── 5. Up to Date ─────────────────────────────────────────────
            return new Result(Priority.UP_TO_DATE, "Up to Date", "You have the latest version", mdFileUrl,
                    remoteVersionName, remoteVersionCode, releaseDate, changelog,
                    directApk, github, obtainium, fdroid, sha256, false);

        } catch (Exception e) {
            android.util.Log.e("UpdateChecker", "Evaluation failed", e);
            return new Result(Priority.ERROR, null, null, null);
        }
    }

    private static Result evaluateLegacy(JSONObject root, String appVersion) {
        JSONObject latest = root.optJSONObject("latest");
        String latestMsg = null;
        String latestMd = null;
        String latestVer = null;
        if (latest != null) {
            latestMsg = latest.optString("short_msg", null);
            latestMd = latest.optString("md_file", null);
            latestVer = latest.optString("latest_version", null);
        }

        JSONObject critical = root.optJSONObject("critical");
        if (critical != null) {
            String below = critical.optString("below", null);
            if (below != null && compareVersions(appVersion, below) < 0) {
                String msg = critical.optString("short_msg", latestMsg);
                String md = critical.optString("md_file", latestMd);
                return new Result(Priority.CRITICAL, "Critical Update Required", msg, md, latestVer, 0, null, null, null, null, null, null, null, true);
            }
        }

        JSONObject warning = root.optJSONObject("warning");
        if (warning != null) {
            String below = warning.optString("below", null);
            if (below != null && compareVersions(appVersion, below) < 0) {
                String msg = warning.optString("short_msg", latestMsg);
                String md = warning.optString("md_file", latestMd);
                return new Result(Priority.WARNING, "Update Recommended", msg, md, latestVer, 0, null, null, null, null, null, null, null, false);
            }
        }

        if (latestVer != null) {
            int cmp = compareVersions(appVersion, latestVer);
            if (cmp < 0 || (!DebugConfig.CACHE_UPDATE_JSON && cmp == 0)) {
                return new Result(Priority.LATEST, "Update Available", latestMsg != null ? latestMsg : "New version available", latestMd, latestVer, 0, null, null, null, null, null, null, null, false);
            }
        }

        return new Result(Priority.UP_TO_DATE, "Up to Date", latestMsg, latestMd, latestVer, 0, null, null, null, null, null, null, null, false);
    }

    /**
     * Compare semantic version strings like "4.28.14".
     */
    public static int compareVersions(String v1, String v2) {
        if (v1 == null) v1 = "0";
        if (v2 == null) v2 = "0";
        String cleanV1 = v1.replaceAll("-.*$", "").trim();
        String cleanV2 = v2.replaceAll("-.*$", "").trim();
        String[] parts1 = cleanV1.split("\\.");
        String[] parts2 = cleanV2.split("\\.");
        int len = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < len; i++) {
            long n1 = 0, n2 = 0;
            if (i < parts1.length) {
                try { n1 = Long.parseLong(parts1[i].replaceAll("[^0-9]", "")); } catch (NumberFormatException ignored) {}
            }
            if (i < parts2.length) {
                try { n2 = Long.parseLong(parts2[i].replaceAll("[^0-9]", "")); } catch (NumberFormatException ignored) {}
            }
            if (n1 != n2) return Long.compare(n1, n2);
        }
        return 0;
    }

    // ── Debug: forced fetch with full diagnostics ─────────────────────

    public interface FetchDiagnosticCallback {
        void onResult(boolean success, String json, String log);
    }

    public static void forceFetch(Context ctx, FetchDiagnosticCallback callback) {
        EXECUTOR.execute(() -> {
            StringBuilder log = new StringBuilder();
            boolean success = false;
            String resultJson = null;

            boolean online = isOnline(ctx);
            log.append("[Network] ").append(online ? "ONLINE" : "OFFLINE").append("\n");
            if (!online) {
                log.append("[Error] No active network connection — cannot fetch.");
                final String l = log.toString();
                MAIN.post(() -> callback.onResult(false, null, l));
                return;
            }

            log.append("[Primary URL] ").append(PRIMARY_URL).append("\n");
            try {
                resultJson = fetchHttp(PRIMARY_URL);
                log.append("[HTTP] Primary 200 OK\n");
            } catch (Exception e) {
                log.append("[Primary Failed] ").append(e.getMessage()).append("\n");
                log.append("[Fallback URL] ").append(FALLBACK_URL).append("\n");
                try {
                    resultJson = fetchHttp(FALLBACK_URL);
                    log.append("[HTTP] Fallback 200 OK\n");
                } catch (Exception fe) {
                    log.append("[Fallback Failed] ").append(fe.getMessage()).append("\n");
                }
            }

            if (resultJson != null) {
                try {
                    new JSONObject(resultJson);
                    log.append("[JSON] Valid JSON, ").append(resultJson.length()).append(" chars\n");
                    ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            .edit()
                            .putString(KEY_JSON, resultJson)
                            .putLong(KEY_FETCH_TS, System.currentTimeMillis())
                            .apply();
                    log.append("[Cache] Saved to SharedPrefs\n");
                    log.append("[Result] SUCCESS");
                    success = true;
                } catch (Exception je) {
                    log.append("[JSON] Parse error: ").append(je.getMessage()).append("\n");
                }
            }

            final boolean fs = success;
            final String fj = resultJson;
            final String fl = log.toString();
            MAIN.post(() -> callback.onResult(fs, fj, fl));
        });
    }
}
