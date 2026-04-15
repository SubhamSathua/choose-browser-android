package com.hyper.choosebrowsernew;

import android.net.Uri;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Strips known tracking / analytics query parameters from URLs.
 * The cleaned link still works — only tracker params are removed.
 */
public class LinkCleaner {

    /** Well-known tracking query-parameter names (lower-case). */
    private static final Set<String> TRACKING_PARAMS = new HashSet<>(Arrays.asList(
            // Google Analytics / Ads
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
            "utm_id", "utm_source_platform", "utm_creative_format", "utm_marketing_tactic",
            "gclid", "gclsrc", "dclid", "gbraid", "wbraid", "srsltid",
            "_ga", "_gl", "_gac",

            // Facebook / Meta
            "fbclid", "fb_action_ids", "fb_action_types", "fb_ref",
            "fb_source", "fb_comment_id",

            // Microsoft / Bing Ads
            "msclkid",

            // Twitter / X
            "twclid",

            // Instagram
            "igshid", "ig_rid", "ig_mid",

            // YouTube / Spotify
            "si", "feature", "pp",

            // Mailchimp
            "mc_cid", "mc_eid",

            // HubSpot
            "_hsenc", "_hsmi", "hsa_cam", "hsa_grp", "hsa_mt", "hsa_src",
            "hsa_ad", "hsa_acc", "hsa_net", "hsa_ver", "hsa_la", "hsa_ol",
            "hsa_kw", "hsa_tgt",

            // Yandex
            "yclid", "_openstat", "ymclid",

            // Mixpanel / Vero / drip
            "vero_id", "__s", "mkt_tok",

            // Adobe
            "s_cid",

            // Misc
            "zanpid", "irclickid", "ref_src", "ref_url",

            // Bing image-search viewer-specific (not needed for the actual image)
            "FORM", "ck"
    ));

    /**
     * Remove tracking parameters from the given URL string.
     * If parsing fails, the original URL is returned unchanged.
     */
    public static String clean(String url) {
        if (TextUtils.isEmpty(url)) return url;

        try {
            Uri uri = Uri.parse(url);
            Set<String> paramNames = uri.getQueryParameterNames();
            if (paramNames == null || paramNames.isEmpty()) return url;

            boolean hasTracker = false;
            for (String name : paramNames) {
                if (TRACKING_PARAMS.contains(name.toLowerCase())) {
                    hasTracker = true;
                    break;
                }
            }
            if (!hasTracker) return url;

            // Rebuild query without tracking params
            Uri.Builder builder = uri.buildUpon().clearQuery();
            for (String name : paramNames) {
                if (!TRACKING_PARAMS.contains(name.toLowerCase())) {
                    // Preserve all values for this parameter
                    for (String value : uri.getQueryParameters(name)) {
                        builder.appendQueryParameter(name, value);
                    }
                }
            }

            return builder.build().toString();
        } catch (Exception e) {
            // If anything goes wrong, return original — never break the link
            return url;
        }
    }
}
