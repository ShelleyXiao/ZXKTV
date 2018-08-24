package com.zx.zxktv.presentation;

import android.net.Uri;

import java.io.Serializable;

public class VideoBean implements Serializable {

    private String displayName;

    private Uri uri;
    private String nextVideoName;

    public VideoBean() {
    }

    public VideoBean(String displayName, String nextVideoName, Uri uri) {
        this.displayName = displayName;
        this.nextVideoName = nextVideoName;
        this.uri = uri;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public void setNextVideoName(String nextVideoName) {
        this.nextVideoName = nextVideoName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Uri getUri() {
        return uri;
    }

    public String getNextVideoName() {
        return nextVideoName;
    }
}
