package com.hyper.choosebrowsernew.data.model;

import java.util.Collections;
import java.util.List;

public class UpdateResult {

    public enum Priority {
        KILL_SWITCH,
        CRITICAL,
        WARNING,
        LATEST,
        UP_TO_DATE,
        ERROR
    }

    public final Priority priority;
    public final String title;
    public final String shortMsg;
    public final String mdFileUrl;
    public final String latestVersion;
    public final int latestVersionCode;
    public final String releaseDate;
    public final List<String> changelog;
    public final String directApkUrl;
    public final String githubUrl;
    public final String obtainiumUrl;
    public final String fdroidUrl;
    public final String sha256;
    public final boolean isBlocking;

    public UpdateResult(Priority priority, String title, String shortMsg, String mdFileUrl,
                        String latestVersion, int latestVersionCode, String releaseDate,
                        List<String> changelog, String directApkUrl, String githubUrl,
                        String obtainiumUrl, String fdroidUrl, String sha256, boolean isBlocking) {
        this.priority = priority;
        this.title = title;
        this.shortMsg = shortMsg;
        this.mdFileUrl = mdFileUrl;
        this.latestVersion = latestVersion;
        this.latestVersionCode = latestVersionCode;
        this.releaseDate = releaseDate;
        this.changelog = changelog != null ? Collections.unmodifiableList(changelog) : Collections.emptyList();
        this.directApkUrl = directApkUrl;
        this.githubUrl = githubUrl;
        this.obtainiumUrl = obtainiumUrl;
        this.fdroidUrl = fdroidUrl;
        this.sha256 = sha256;
        this.isBlocking = isBlocking;
    }

    public UpdateResult(Priority priority, String shortMsg, String mdFileUrl, String latestVersion) {
        this(priority, null, shortMsg, mdFileUrl, latestVersion, 0, null, Collections.emptyList(),
                null, null, null, null, null, priority == Priority.CRITICAL || priority == Priority.KILL_SWITCH);
    }

    public static UpdateResult killSwitch(String title, String message) {
        return new UpdateResult(Priority.KILL_SWITCH, title, message, null, null, 0, null,
                Collections.emptyList(), null, null, null, null, null, true);
    }

    public static UpdateResult error() {
        return new UpdateResult(Priority.ERROR, null, null, null);
    }

    public static UpdateResult upToDate() {
        return new UpdateResult(Priority.UP_TO_DATE, null, null, null);
    }
}
