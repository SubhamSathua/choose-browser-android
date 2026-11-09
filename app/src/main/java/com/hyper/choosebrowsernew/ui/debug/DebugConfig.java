package com.hyper.choosebrowsernew.ui.debug;

import android.content.Context;
import android.content.SharedPreferences;

import com.hyper.choosebrowsernew.BuildConfig;

/**
 * Debug / build-time configuration flags and state management.
 */
public final class DebugConfig {

    public static final String PREFS_NAME = "debug_settings";
    public static final String KEY_DEBUG_UNLOCKED_AT = "debug_unlocked_timestamp";
    public static final long AUTO_HIDE_DURATION_MS = 24L * 60L * 60L * 1000L; // 24 hours

    /**
     * true  → normal behaviour: use cached JSON (max 6 h), fetch only when stale or missing.
     * false → always fetch a fresh copy from the server (useful when testing update.json changes).
     */
    public static final boolean CACHE_UPDATE_JSON = true;

    /**
     * true  → show the "Debug" card in Settings (update cache info, raw JSON, clear cache).
     * false → card is hidden; no debug UI is shown.
     */
    public static final boolean DEBUG_SCREEN = true;

    private DebugConfig() {}

    /**
     * Check whether the current build type is Alpha.
     */
    public static boolean isAlpha() {
        return "alpha".equals(BuildConfig.BUILD_TYPE);
    }

    /**
     * Check whether the debug menu is unlocked and accessible.
     * In Alpha: always unlocked.
     * In Stable: unlocked via 7-tap sequence and auto-hides after 24 hours.
     */
    public static boolean isDebugUnlocked(Context context) {
        if (isAlpha()) {
            return true;
        }
        if (context == null) return false;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long unlockedAt = prefs.getLong(KEY_DEBUG_UNLOCKED_AT, 0L);
        if (unlockedAt <= 0L) {
            return false;
        }

        long now = System.currentTimeMillis();
        long elapsed = now - unlockedAt;
        if (elapsed < 0L || elapsed >= AUTO_HIDE_DURATION_MS) {
            // Automatically hide after 24 hours
            prefs.edit().remove(KEY_DEBUG_UNLOCKED_AT).apply();
            return false;
        }
        return true;
    }

    /**
     * Unlock the debug menu in stable (records current timestamp).
     */
    public static void unlockDebug(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(KEY_DEBUG_UNLOCKED_AT, System.currentTimeMillis()).apply();
    }

    /**
     * Hide the debug menu immediately (stable only).
     */
    public static void hideDebug(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_DEBUG_UNLOCKED_AT).apply();
    }
}

