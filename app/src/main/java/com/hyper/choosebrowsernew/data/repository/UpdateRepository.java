package com.hyper.choosebrowsernew.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import com.hyper.choosebrowsernew.AppConstantsDetails;
import com.hyper.choosebrowsernew.UpdateChecker;
import com.hyper.choosebrowsernew.data.model.UpdateResult;
import com.hyper.choosebrowsernew.ui.debug.DebugConfig;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class UpdateRepository {

    private static final String UPDATE_URL = AppConstantsDetails.UPDATE_JSON_URL;
    private static final String PREFS_NAME = "update_checker_prefs";
    private static final String KEY_JSON = "cached_json";
    private static final String KEY_FETCH_TS = "last_fetch_time";
    private static final long CACHE_MAX_AGE = 6 * 60 * 60 * 1000L;

    private final Context context;
    private final Executor executor = Executors.newSingleThreadExecutor();

    public UpdateRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public interface UpdateCallback {
        void onResult(UpdateResult result);
    }

    public UpdateResult getCachedResult() {
        if (UpdateChecker.tempJson != null) return evaluate(UpdateChecker.tempJson);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_JSON, null);
        if (json == null) return UpdateResult.upToDate();
        return evaluate(json);
    }

    public void checkUpdate(UpdateCallback callback) {
        executor.execute(() -> {
            UpdateResult result;
            try {
                if (UpdateChecker.tempJson != null) {
                    result = evaluate(UpdateChecker.tempJson);
                } else {
                    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    String cachedJson = prefs.getString(KEY_JSON, null);
                    long lastFetch = prefs.getLong(KEY_FETCH_TS, 0);
                    long cacheAge = System.currentTimeMillis() - lastFetch;

                    String json;
                    if (!isOnline()) {
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
                    result = (json != null) ? evaluate(json) : UpdateResult.error();
                }
            } catch (Exception e) {
                result = UpdateResult.error();
            }
            callback.onResult(result);
        });
    }

    private boolean isOnline() {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            Network net = cm.getActiveNetwork();
            if (net == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(net);
            return caps != null && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } catch (Exception e) {
            return false;
        }
    }

    private String fetchJson() throws Exception {
        URL url = new URL(UPDATE_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        try {
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

    private UpdateResult evaluate(String json) {
        String appVersion = getAppVersion();
        UpdateChecker.Result r = UpdateChecker.evaluate(json, appVersion);
        return new UpdateResult(mapPriority(r.priority), r.shortMsg, r.mdFileUrl, r.latestVersion);
    }

    private UpdateResult.Priority mapPriority(UpdateChecker.Priority p) {
        return UpdateResult.Priority.valueOf(p.name());
    }

    private String getAppVersion() {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "0";
        }
    }

    private int compareVersions(String v1, String v2) {
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
}
