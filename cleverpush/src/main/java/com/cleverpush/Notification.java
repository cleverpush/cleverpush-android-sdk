package com.cleverpush;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Notification implements Serializable {
    @SerializedName("_id")
    String id;
    @SerializedName("title")
    String title;
    @SerializedName("text")
    String text;
    @SerializedName("url")
    String url;
    @SerializedName("iconUrl")
    String iconUrl;
    @SerializedName("mediaUrl")
    String mediaUrl;
    @SerializedName("actions")
    NotificationAction[] actions;

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public String getUrl() {
        return url;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public NotificationAction[] getActions() {
        return actions;
    }
}
