package com.hyper.choosebrowsernew;

/**
 * Debug / build-time configuration flags.
 * Flip these manually during development; keep defaults sane for release.
 */
public final class DebugConfig {

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
}
