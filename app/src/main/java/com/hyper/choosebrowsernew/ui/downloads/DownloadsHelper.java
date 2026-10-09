package com.hyper.choosebrowsernew.ui.downloads;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DownloadsHelper {

    public static final String FOLDER_NAME = "ChooseBrowser";

    private static final Set<String> HARMFUL_EXTENSIONS = new HashSet<>(Arrays.asList(
            "apk", "xapk", "apkm", "apks", "aab", "jar", "dex", "so", "sh", "vbs", "bat", "msi", "cmd", "exe"
    ));

    public static File getDownloadsDir() {
        File publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File chooseBrowserDir = new File(publicDownloads, FOLDER_NAME);
        if (!chooseBrowserDir.exists()) {
            chooseBrowserDir.mkdirs();
        }
        return chooseBrowserDir;
    }

    public static boolean isHarmfulExtension(String fileName) {
        if (fileName == null) return false;
        String ext = getFileExtension(fileName).toLowerCase(Locale.ROOT);
        return HARMFUL_EXTENSIONS.contains(ext);
    }

    public static String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1);
        }
        return "";
    }

    public static String getBaseName(String fileName) {
        if (fileName == null) return "";
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(0, lastDot);
        }
        return fileName;
    }

    public static String formatFileSize(long bytes) {
        if (bytes <= 0) return "Unknown size";
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format(Locale.getDefault(), "%.1f %cB", bytes / Math.pow(1024, exp), pre);
    }

    public static String formatDate(long millis) {
        if (millis <= 0) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy", Locale.getDefault());
        return sdf.format(new Date(millis));
    }

    public static List<DownloadItem> loadDownloadFiles() {
        List<DownloadItem> items = new ArrayList<>();
        File dir = getDownloadsDir();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && !f.isHidden()) {
                    String ext = getFileExtension(f.getName()).toLowerCase(Locale.ROOT);
                    String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
                    items.add(new DownloadItem(f, mime));
                }
            }
        }
        return items;
    }

    public static void openFile(Context context, DownloadItem item) {
        try {
            File file = new File(item.getFilePath());
            if (!file.exists()) {
                Toast.makeText(context, "File does not exist", Toast.LENGTH_SHORT).show();
                return;
            }
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            String ext = getFileExtension(file.getName()).toLowerCase(Locale.ROOT);
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
            if (mime == null || mime.isEmpty()) {
                mime = "*/*";
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mime);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(Intent.createChooser(intent, "Open with"));
        } catch (Exception e) {
            Toast.makeText(context, "Cannot open this file", Toast.LENGTH_SHORT).show();
        }
    }

    public static void shareFile(Context context, DownloadItem item) {
        try {
            File file = new File(item.getFilePath());
            if (!file.exists()) {
                Toast.makeText(context, "File does not exist", Toast.LENGTH_SHORT).show();
                return;
            }
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            String ext = getFileExtension(file.getName()).toLowerCase(Locale.ROOT);
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
            if (mime == null || mime.isEmpty()) {
                mime = "*/*";
            }

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(mime);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(intent, "Share file"));
        } catch (Exception e) {
            Toast.makeText(context, "Cannot share this file", Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean deleteFile(DownloadItem item) {
        try {
            File file = new File(item.getFilePath());
            return file.delete();
        } catch (Exception e) {
            return false;
        }
    }

    public static String getCategory(String fileName) {
        String ext = getFileExtension(fileName).toLowerCase(Locale.ROOT);
        if (Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg").contains(ext)) {
            return "Images";
        }
        if ("pdf".equals(ext)) {
            return "PDF";
        }
        if (Arrays.asList("doc", "docx", "txt", "rtf", "xls", "xlsx", "ppt", "pptx", "csv").contains(ext)) {
            return "Documents";
        }
        return "Others";
    }
}
