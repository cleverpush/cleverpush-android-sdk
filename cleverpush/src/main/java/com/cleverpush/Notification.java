package com.cleverpush;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Map;

public class Notification implements Serializable {
    @SerializedName("_id")
    String id;
    @SerializedName("tag")
    String tag;
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
    @SerializedName("customData")
    Map customData;
    @SerializedName("chatNotification")
    Boolean chatNotification;
    @SerializedName("carouselEnabled")
    Boolean carouselEnabled;
    @SerializedName("carouselItems")
    NotificationCarouselItem[] carouselItems;
    @SerializedName("category")
    NotificationCategory category;
    @SerializedName("createdAt")
    String createdAt;

    public String getId() {
        return id;
    }

    public String getTag() {
        return tag;
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

    public NotificationCategory getCategory() {
        return category;
    }

    public NotificationAction[] getActions() {
        if (actions == null) {
            return new NotificationAction[0];
        }
        return actions;
    }

    public Map getCustomData() {
        return customData;
    }

    public Boolean isChatNotification() {
        return chatNotification != null && chatNotification;
    }

    public Boolean isCarouselEnabled() {
        return carouselEnabled != null && carouselEnabled;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public NotificationCarouselItem[] getCarouselItems() {
        if (carouselItems == null) {
            return new NotificationCarouselItem[0];
        }
        return carouselItems;
    }

    public int getCarouselLength() {
        int length = 0;

        if (carouselItems != null) {
            length = carouselItems.length;
        }

        return length;
    }

    public int getNextCarouselIndex(int currentIndex) {
        int index = currentIndex;
        int maxValue = getCarouselLength() - 1;

        if (index >= maxValue) {
            index = 0;
        } else {
            index++;
        }

        return index;
    }

    public int getPreviousCarouselIndex(int currentIndex) {
        int index = currentIndex;

        if (index <= 0) {
            index = getCarouselLength() - 1;
        } else {
            index--;
        }

        return index;
    }
}
