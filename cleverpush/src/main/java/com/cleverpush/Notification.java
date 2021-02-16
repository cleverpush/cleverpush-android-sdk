package com.cleverpush;

import androidx.core.app.NotificationCompat;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
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
    @SerializedName("soundFilename")
    String soundFilename;
    @SerializedName("createdAt")
    String createdAt;

    transient NotificationCompat.Extender extender;
	String rawPayload;

    public String getId() {
        return id;
    }

    public String getTag() {
        if (tag == null) {
            return id;
        }
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

    public String getSoundFilename() {
        if (soundFilename != null && !soundFilename.isEmpty()) {
            return soundFilename;
        }
        if (this.getCategory() != null && this.getCategory().getSoundFilename() != null && !this.getCategory().getSoundFilename().isEmpty()) {
            return this.getCategory().getSoundFilename();
        }
        return null;
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

    public int getCreatedAtTime() {
        if (this.getCreatedAt() != null) {
            try {
                Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.US).parse(this.getCreatedAt());
                return (int) (date.getTime() / 1000);
            } catch (Exception err) {

            }
        }
        return 0;
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

    public NotificationCompat.Extender getExtender() {
    	return extender;
	}

	public void setExtender(NotificationCompat.Extender extender) {
		this.extender = extender;
	}

	public String getRawPayload() {
		return rawPayload;
	}

	public void setRawPayload(String rawPayload) {
    	this.rawPayload = rawPayload;
	}
}
