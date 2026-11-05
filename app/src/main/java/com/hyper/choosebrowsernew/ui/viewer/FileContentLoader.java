package com.hyper.choosebrowsernew.ui.viewer;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Modular asynchronous file content reader for zero-permission URI streams.
 */
public class FileContentLoader {

    private static final int MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB preview ceiling
    private static final int BUFFER_SIZE = 8192;

    public interface Callback {
        void onLoaded(@NonNull String fileName, @NonNull FileType fileType, @NonNull String content);
        void onError(@NonNull Throwable error);
    }

    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public FileContentLoader(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    public void loadUri(@NonNull Uri uri, @Nullable String explicitMimeType, @NonNull Callback callback) {
        executor.execute(() -> {
            try {
                String fileName = resolveFileName(context, uri);
                String resolvedMime = resolveMimeType(context, uri, explicitMimeType);
                FileType fileType = FileType.from(fileName, resolvedMime);

                String content = readStreamToString(context, uri);

                mainHandler.post(() -> callback.onLoaded(fileName, fileType, content));
            } catch (Throwable t) {
                mainHandler.post(() -> callback.onError(t));
            }
        });
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    @NonNull
    public static String resolveFileName(@NonNull Context context, @NonNull Uri uri) {
        String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(scheme)) {
            try (Cursor cursor = context.getContentResolver().query(uri,
                    new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        String name = cursor.getString(index);
                        if (!TextUtils.isEmpty(name)) {
                            return name;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        } else if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(scheme)) {
            String path = uri.getPath();
            if (path != null) {
                File file = new File(path);
                return file.getName();
            }
        }

        String lastSegment = uri.getLastPathSegment();
        if (!TextUtils.isEmpty(lastSegment)) {
            return lastSegment;
        }

        return "document";
    }

    @Nullable
    public static String resolveMimeType(@NonNull Context context, @NonNull Uri uri, @Nullable String fallbackMime) {
        if (!TextUtils.isEmpty(fallbackMime) && !"*/*".equals(fallbackMime)) {
            return fallbackMime;
        }
        try {
            String type = context.getContentResolver().getType(uri);
            if (!TextUtils.isEmpty(type)) {
                return type;
            }
        } catch (Exception ignored) {
        }
        return fallbackMime;
    }

    @NonNull
    private static String readStreamToString(@NonNull Context context, @NonNull Uri uri) throws Exception {
        InputStream is = null;
        try {
            if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(uri.getScheme())) {
                String path = uri.getPath();
                if (path != null) {
                    is = new FileInputStream(new File(path));
                }
            }
            if (is == null) {
                is = context.getContentResolver().openInputStream(uri);
            }
            if (is == null) {
                throw new IllegalStateException("Unable to open input stream for: " + uri);
            }

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[BUFFER_SIZE];
            int nRead;
            int total = 0;
            boolean truncated = false;

            while ((nRead = is.read(data, 0, data.length)) != -1) {
                if (total + nRead > MAX_FILE_SIZE_BYTES) {
                    int allowed = MAX_FILE_SIZE_BYTES - total;
                    if (allowed > 0) {
                        buffer.write(data, 0, allowed);
                    }
                    truncated = true;
                    break;
                }
                buffer.write(data, 0, nRead);
                total += nRead;
            }

            String text = buffer.toString(StandardCharsets.UTF_8.name());
            if (truncated) {
                text += "\n\n[--- File truncated: exceeds 10 MB preview limit ---]";
            }
            return text;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
