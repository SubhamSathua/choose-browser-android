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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class UpdateRepository {

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
        return UpdateChecker.isOnline(context);
    }

    private String fetchJson() {
        try {
            return fetchHttp(AppConstantsDetails.UPDATE_JSON_URL);
        } catch (Exception e) {
            try {
                return fetchHttp(AppConstantsDetails.UPDATE_JSON_FALLBACK_URL);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private String fetchHttp(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("Accept", "application/json");
        try {
            int code = conn.getResponseCode();
            if (code != 200) throw new Exception("HTTP " + code);
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
        String appVersion = UpdateChecker.getAppVersion(context);
        int appVersionCode = UpdateChecker.getAppVersionCode(context);
        String channel = UpdateChecker.getSelectedChannel(context);
        UpdateChecker.Result r = UpdateChecker.evaluate(json, appVersion, appVersionCode, channel);
        return new UpdateResult(
                mapPriority(r.priority),
                r.title,
                r.shortMsg,
                r.mdFileUrl,
                r.latestVersion,
                r.latestVersionCode,
                r.releaseDate,
                r.changelog,
                r.directApkUrl,
                r.githubUrl,
                r.obtainiumUrl,
                r.fdroidUrl,
                r.sha256,
                r.isBlocking
        );
    }

    private UpdateResult.Priority mapPriority(UpdateChecker.Priority p) {
        return UpdateResult.Priority.valueOf(p.name());
    }
}
