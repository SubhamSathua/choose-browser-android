package com.hyper.choosebrowsernew.ui.downloads;

import java.io.File;

public class DownloadItem {
    private final String fileName;
    private final String filePath;
    private final long fileSize;
    private final long dateModified;
    private final String mimeType;
    private boolean selected;

    public DownloadItem(File file, String mimeType) {
        this.fileName = file.getName();
        this.filePath = file.getAbsolutePath();
        this.fileSize = file.length();
        this.dateModified = file.lastModified();
        this.mimeType = mimeType != null ? mimeType : "";
        this.selected = false;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public long getDateModified() {
        return dateModified;
    }

    public String getMimeType() {
        return mimeType;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
