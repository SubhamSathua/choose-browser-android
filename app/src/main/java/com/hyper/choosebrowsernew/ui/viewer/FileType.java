package com.hyper.choosebrowsernew.ui.viewer;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Supported file types for previewing.
 */
public enum FileType {
    MARKDOWN("Markdown", "MD"),
    TEXT("Text", "TXT");

    private final String displayName;
    private final String badge;

    FileType(String displayName, String badge) {
        this.displayName = displayName;
        this.badge = badge;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadge() {
        return badge;
    }

    @NonNull
    public static FileType from(@Nullable String fileName, @Nullable String mimeType) {
        if (!TextUtils.isEmpty(mimeType)) {
            String mime = mimeType.toLowerCase(Locale.ROOT);
            if (mime.contains("markdown")) {
                return MARKDOWN;
            }
        }

        if (!TextUtils.isEmpty(fileName)) {
            String lower = fileName.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".md") || lower.endsWith(".markdown") || lower.endsWith(".mdown")) {
                return MARKDOWN;
            }
        }

        return TEXT;
    }
}
