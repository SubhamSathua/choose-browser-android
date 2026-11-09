package com.hyper.choosebrowsernew.util;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * High-performance URL cleaning engine implementing the 8-step pipeline
 * from Dev-LAB/REF/clean-algo.md.
 *
 * Rules are sourced from Brave Software's Query Filter (Mozilla Public License 2.0)
 * located at assets/rules/tracker_rules.json with zero runtime dependencies.
 */
public class LinkCleaner {

    private static final String TAG = "LinkCleaner";

    // Global simple trackers (O(1) lookup)
    private static final Set<String> GLOBAL_TRACKERS = new HashSet<>(Arrays.asList(
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
            "utm_id", "utm_source_platform", "utm_creative_format", "utm_marketing_tactic",
            "gclid", "gclsrc", "dclid", "gbraid", "wbraid", "srsltid",
            "fbclid", "fb_action_ids", "fb_action_types", "fb_ref", "fb_source",
            "msclkid", "twclid", "igshid", "mc_cid", "mc_eid",
            "_ga", "_gl", "_gac", "_hsenc", "_hsmi",
            "hsa_cam", "hsa_grp", "hsa_mt", "hsa_src", "hsa_ad", "hsa_acc", "hsa_net", "hsa_ver", "hsa_la", "hsa_ol", "hsa_kw", "hsa_tgt",
            "yclid", "ymclid", "_openstat", "mkt_tok", "vero_id", "zanpid", "irclickid"
    ));

    // Scoped / domain-specific trackers
    private static final Map<String, List<Pattern>> SCOPED_PATTERNS = new HashMap<>();

    // Redirect unwrappers: e.g. google.com/url?q=...
    private static class RedirectRule {
        String hostKeyword;
        String path;
        String queryParam;

        RedirectRule(String hostKeyword, String path, String queryParam) {
            this.hostKeyword = hostKeyword;
            this.path = path;
            this.queryParam = queryParam;
        }
    }

    private static final List<RedirectRule> REDIRECT_RULES = new ArrayList<>();
    private static boolean isInitialized = false;

    static {
        // Initialize default compiled scoped patterns
        addDefaultScopedPatterns();
    }

    private static void addDefaultScopedPatterns() {
        addScopedPattern("amazon", "ref_.*");
        addScopedPattern("amazon", "tag");
        addScopedPattern("amazon", "linkCode");
        addScopedPattern("amazon", "camp");
        addScopedPattern("amazon", "creative");
        addScopedPattern("amazon", "pd_rd_.*");
        addScopedPattern("amazon", "pf_rd_.*");

        addScopedPattern("youtube.com", "si");
        addScopedPattern("youtube.com", "feature");
        addScopedPattern("youtube.com", "pp");
        addScopedPattern("youtu.be", "si");
        addScopedPattern("youtu.be", "feature");

        addScopedPattern("x.com", "t");
        addScopedPattern("x.com", "s");
        addScopedPattern("x.com", "ref_src");
        addScopedPattern("x.com", "ref_url");

        addScopedPattern("twitter.com", "t");
        addScopedPattern("twitter.com", "s");
        addScopedPattern("twitter.com", "ref_src");
        addScopedPattern("twitter.com", "ref_url");

        addScopedPattern("reddit.com", "ref");
        addScopedPattern("reddit.com", "ref_source");
        addScopedPattern("reddit.com", "share_id");

        addScopedPattern("tiktok.com", "_r");
        addScopedPattern("tiktok.com", "_t");
        addScopedPattern("tiktok.com", "checksum");
        addScopedPattern("tiktok.com", "is_copy_url");
        addScopedPattern("tiktok.com", "is_from_webapp");

        addScopedPattern("instagram.com", "igsh");
        addScopedPattern("instagram.com", "ig_rid");
        addScopedPattern("instagram.com", "ig_mid");

        addScopedPattern("linkedin.com", "trackingId");
        addScopedPattern("linkedin.com", "refId");
        addScopedPattern("linkedin.com", "trk");

        addScopedPattern("spotify.com", "si");
        addScopedPattern("spotify.com", "context");

        addScopedPattern("aliexpress.com", "spm");
        addScopedPattern("aliexpress.com", "scm");
        addScopedPattern("aliexpress.com", "pvid");
        addScopedPattern("aliexpress.com", "algo_pvid");
        addScopedPattern("aliexpress.com", "algo_exp_id");

        REDIRECT_RULES.add(new RedirectRule("google.", "/url", "q"));
        REDIRECT_RULES.add(new RedirectRule("l.facebook.com", "/l.php", "u"));
        REDIRECT_RULES.add(new RedirectRule("out.reddit.com", "", "url"));
    }

    private static void addScopedPattern(String domain, String pattern) {
        List<Pattern> list = SCOPED_PATTERNS.computeIfAbsent(domain, k -> new ArrayList<>());
        try {
            list.add(Pattern.compile("^" + pattern + "$", Pattern.CASE_INSENSITIVE));
        } catch (Exception e) {
            Log.w(TAG, "Invalid pattern: " + pattern, e);
        }
    }

    /**
     * Lazily load dynamic tracker rules from assets/rules/tracker_rules.json if present.
     */
    public static synchronized void init(Context context) {
        if (isInitialized || context == null) return;
        try (InputStream is = context.getAssets().open("rules/tracker_rules.json")) {
            byte[] buffer = new byte[is.available()];
            int read = is.read(buffer);
            if (read > 0) {
                String jsonStr = new String(buffer, StandardCharsets.UTF_8);
                JSONObject root = new JSONObject(jsonStr);

                if (root.has("simple")) {
                    JSONArray simpleArr = root.getJSONArray("simple");
                    for (int i = 0; i < simpleArr.length(); i++) {
                        GLOBAL_TRACKERS.add(simpleArr.getString(i).toLowerCase(Locale.ROOT));
                    }
                }

                if (root.has("scoped")) {
                    JSONObject scopedObj = root.getJSONObject("scoped");
                    Iterator<String> keys = scopedObj.keys();
                    while (keys.hasNext()) {
                        String domain = keys.next();
                        JSONArray arr = scopedObj.getJSONArray(domain);
                        for (int i = 0; i < arr.length(); i++) {
                            addScopedPattern(domain, arr.getString(i));
                        }
                    }
                }

                if (root.has("redirects")) {
                    JSONArray redArr = root.getJSONArray("redirects");
                    for (int i = 0; i < redArr.length(); i++) {
                        JSONObject r = redArr.getJSONObject(i);
                        REDIRECT_RULES.add(new RedirectRule(
                                r.getString("host"),
                                r.optString("path", ""),
                                r.getString("param")
                        ));
                    }
                }
            }
            isInitialized = true;
        } catch (Exception e) {
            Log.d(TAG, "Using embedded tracker rules: " + e.getMessage());
        }
    }

    public static String clean(Context context, String url) {
        if (!isInitialized && context != null) {
            init(context);
        }
        return clean(url);
    }

    /**
     * Execute the 8-step cleaning pipeline on the provided URL.
     */
    public static String clean(String url) {
        if (TextUtils.isEmpty(url)) return url;

        // Step 1: EXTRACT (trim whitespace)
        String trimmed = url.trim();

        // Step 2: VALIDATE (parse using Uri, accept http/https)
        Uri uri;
        try {
            uri = Uri.parse(trimmed);
            String scheme = uri.getScheme();
            if (scheme == null) return url;
            String lowerScheme = scheme.toLowerCase(Locale.ROOT);
            if (!lowerScheme.equals("http") && !lowerScheme.equals("https")) {
                return url; // Preserve non-web URLs unchanged
            }
        } catch (Exception e) {
            return url; // Step 8: Return original on critical failure
        }

        try {
            // Step 3 & 4: IDENTIFY PROVIDER & UNWRAP REDIRECTS
            String host = uri.getHost();
            if (host != null) {
                String unwrapped = unwrapRedirect(uri, host);
                if (unwrapped != null && !unwrapped.equals(trimmed)) {
                    // Recurse to clean the unwrapped destination URL
                    return clean(unwrapped);
                }
            }

            // Step 5: APPLY RULES
            Set<String> paramNames = uri.getQueryParameterNames();
            if (paramNames == null || paramNames.isEmpty()) {
                return trimmed; // No query parameters to clean
            }

            List<Pattern> domainPatterns = findScopedPatternsForHost(host);
            boolean modified = false;

            for (String param : paramNames) {
                if (isTrackerParam(param, domainPatterns)) {
                    modified = true;
                    break;
                }
            }

            if (!modified) {
                return trimmed; // Clean already
            }

            // Step 6: RECONSTRUCT
            Uri.Builder builder = uri.buildUpon().clearQuery();
            for (String param : paramNames) {
                if (!isTrackerParam(param, domainPatterns)) {
                    // Preserve all non-tracking parameters and values
                    for (String val : uri.getQueryParameters(param)) {
                        builder.appendQueryParameter(param, val);
                    }
                }
            }

            String result = builder.build().toString();

            // Step 7: VALIDATE RESULT
            Uri resultUri = Uri.parse(result);
            if (resultUri.getHost() == null || !resultUri.getHost().equalsIgnoreCase(uri.getHost())) {
                return trimmed; // Integrity check failed: host changed
            }
            if (!TextUtils.equals(resultUri.getPath(), uri.getPath())) {
                return trimmed; // Integrity check failed: path changed
            }

            return result;
        } catch (Exception e) {
            // Step 8: Fallback to original URL on any unexpected failure
            return trimmed;
        }
    }

    private static String unwrapRedirect(Uri uri, String host) {
        String lowerHost = host.toLowerCase(Locale.ROOT);
        String path = uri.getPath() != null ? uri.getPath() : "";

        for (RedirectRule rule : REDIRECT_RULES) {
            if (lowerHost.contains(rule.hostKeyword)) {
                if (rule.path.isEmpty() || path.equalsIgnoreCase(rule.path)) {
                    String target = uri.getQueryParameter(rule.queryParam);
                    if (!TextUtils.isEmpty(target)) {
                        try {
                            String decoded = URLDecoder.decode(target, StandardCharsets.UTF_8.name());
                            if (decoded.startsWith("http://") || decoded.startsWith("https://")) {
                                return decoded;
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
        return null;
    }

    private static List<Pattern> findScopedPatternsForHost(String host) {
        if (host == null) return null;
        String lowerHost = host.toLowerCase(Locale.ROOT);
        List<Pattern> matched = null;

        for (Map.Entry<String, List<Pattern>> entry : SCOPED_PATTERNS.entrySet()) {
            if (lowerHost.contains(entry.getKey())) {
                if (matched == null) matched = new ArrayList<>();
                matched.addAll(entry.getValue());
            }
        }
        return matched;
    }

    private static boolean isTrackerParam(String paramName, List<Pattern> domainPatterns) {
        if (paramName == null) return false;
        String lower = paramName.toLowerCase(Locale.ROOT);

        // Functional parameters safeguard: never strip critical parameters
        if (lower.equals("v") || lower.equals("q") || lower.equals("search") || lower.equals("id") || lower.equals("page")) {
            return false;
        }

        // Global check (O(1))
        if (GLOBAL_TRACKERS.contains(lower)) {
            return true;
        }

        // Prefix match for common campaign tags
        if (lower.startsWith("utm_") || lower.startsWith("pd_rd_") || lower.startsWith("pf_rd_")) {
            return true;
        }

        // Scoped domain patterns check
        if (domainPatterns != null) {
            for (Pattern p : domainPatterns) {
                if (p.matcher(paramName).matches()) {
                    return true;
                }
            }
        }

        return false;
    }
}
