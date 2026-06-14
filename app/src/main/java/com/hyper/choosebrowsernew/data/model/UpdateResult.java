package com.hyper.choosebrowsernew.data.model;

public class UpdateResult {

    public enum Priority { CRITICAL, WARNING, LATEST, UP_TO_DATE, ERROR }

    public final Priority priority;
    public final String shortMsg;
    public final String mdFileUrl;
    public final String latestVersion;

    public UpdateResult(Priority priority, String shortMsg, String mdFileUrl, String latestVersion) {
        this.priority = priority;
        this.shortMsg = shortMsg;
        this.mdFileUrl = mdFileUrl;
        this.latestVersion = latestVersion;
    }

    public static UpdateResult error() {
        return new UpdateResult(Priority.ERROR, null, null, null);
    }

    public static UpdateResult upToDate() {
        return new UpdateResult(Priority.UP_TO_DATE, null, null, null);
    }
}
