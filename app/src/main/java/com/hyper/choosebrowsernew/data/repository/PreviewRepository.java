package com.hyper.choosebrowsernew.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.List;

public class PreviewRepository {

    private static final String PREFS_NAME = "preview_prefs";
    private static final String KEY_ALLOW_JS = "allow_js";
    private static final String KEY_AD_BLOCK_MODE = "ad_block_mode";

    private final Context context;

    private static final List<String> BLOCKED_DOMAINS = Arrays.asList(
            "doubleclick.net", "googlesyndication.com", "googleadservices.com",
            "google-analytics.com", "googletagmanager.com", "facebook.net",
            "facebook.com/tr", "connect.facebook.net", "analytics.twitter.com",
            "ads-twitter.com", "ad.doubleclick.net", "adservice.google.com",
            "pagead2.googlesyndication.com", "amazon-adsystem.com",
            "scorecardresearch.com", "quantserve.com", "outbrain.com",
            "taboola.com", "criteo.com", "hotjar.com", "mixpanel.com",
            "segment.io", "optimizely.com", "chartbeat.com", "newrelic.com",
            "nr-data.net", "omtrdc.net", "demdex.net", "moatads.com",
            "adnxs.com", "adsrvr.org", "rubiconproject.com", "pubmatic.com",
            "openx.net", "casalemedia.com", "contextweb.com", "advertising.com",
            "yieldmanager.com", "serving-sys.com", "mathtag.com", "turn.com"
    );

    private static final List<String> ADGUARD_EXTRA_DOMAINS = Arrays.asList(
            "adguard.net", "analytics.google.com", "stats.g.doubleclick.net",
            "mc.yandex.ru", "counter.yadro.ru", "rambler.ru",
            "adsystem.com", "ads.yahoo.com", "ads.linkedin.com",
            "pixel.facebook.com", "sp.analytics.yahoo.com",
            "bat.bing.com", "c.bing.com", "clarity.ms",
            "cdn.segment.com", "api.segment.io", "api.mixpanel.com",
            "track.adform.net", "match.adsrvr.org", "cm.g.doubleclick.net",
            "sdk.iad-01.braze.com", "appboy.com", "moengage.com",
            "clevertap.com", "webengage.com", "leanplum.com",
            "ads.pubmatic.com", "ssum.casalemedia.com", "sync.1rx.io"
    );

    public PreviewRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean isAllowJsEnabled() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_ALLOW_JS, false);
    }

    public void setAllowJsEnabled(boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_ALLOW_JS, enabled).apply();
    }

    public String getAdBlockMode() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_AD_BLOCK_MODE, "filter");
    }

    public void setAdBlockMode(String mode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_AD_BLOCK_MODE, mode).apply();
    }

    public boolean shouldBlock(String host, String currentMode) {
        if (host == null) return false;
        for (String domain : BLOCKED_DOMAINS) {
            if (host.contains(domain)) return true;
        }
        if ("dns".equals(currentMode)) {
            for (String domain : ADGUARD_EXTRA_DOMAINS) {
                if (host.contains(domain)) return true;
            }
        }
        return false;
    }
}
