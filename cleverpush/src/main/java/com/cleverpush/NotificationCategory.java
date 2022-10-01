package com.cleverpush;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class NotificationCategory implements Serializable {
    @SerializedName("_id")
    String id;
    @SerializedName("group")
    NotificationCategoryGroup group;
    @SerializedName("name")
    String name;
    @SerializedName("description")
    String description;
    @SerializedName("soundEnabled")
    Boolean soundEnabled;
    @SerializedName("soundFilename")
    String soundFilename;
    @SerializedName("vibrationEnabled")
    Boolean vibrationEnabled;
    @SerializedName("vibrationPattern")
    String vibrationPattern;
    @SerializedName("ledColorEnabled")
    Boolean ledColorEnabled;
    @SerializedName("ledColor")
    String ledColor;
    @SerializedName("lockScreen")
    String lockScreen;
    @SerializedName("importance")
    String importance;
    @SerializedName("badgeDisabled")
    Boolean badgeDisabled;
    @SerializedName("backgroundColor")
    String backgroundColor;
    @SerializedName("foregroundColor")
    String foregroundColor;

    public String getId() {
        return id;
    }

    public NotificationCategoryGroup getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getSoundEnabled() {
        if (soundEnabled == null) {
            return false;
        }
        return soundEnabled;
    }

    public String getSoundFilename() {
        return soundFilename;
    }

    public Boolean getVibrationEnabled() {
        if (vibrationEnabled == null) {
            return false;
        }
        return vibrationEnabled;
    }

    public String getVibrationPattern() {
        return vibrationPattern;
    }

    public Boolean getLedColorEnabled() {
        if (ledColorEnabled == null) {
            return false;
        }
        return ledColorEnabled;
    }

    public String getLedColor() {
        return ledColor;
    }

    public String getLockScreen() {
        return lockScreen;
    }

    public String getImportance() {
        return importance;
    }

    public Boolean getBadgeDisabled() {
        if (badgeDisabled == null) {
            return false;
        }
        return badgeDisabled;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public String getForegroundColor() {
        return foregroundColor;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setGroup(NotificationCategoryGroup group) {
        this.group = group;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setSoundEnabled(Boolean soundEnabled) {
        this.soundEnabled = soundEnabled;
    }

    public void setSoundFilename(String soundFilename) {
        this.soundFilename = soundFilename;
    }

    public void setVibrationEnabled(Boolean vibrationEnabled) {
        this.vibrationEnabled = vibrationEnabled;
    }

    public void setVibrationPattern(String vibrationPattern) {
        this.vibrationPattern = vibrationPattern;
    }

    public void setLedColorEnabled(Boolean ledColorEnabled) {
        this.ledColorEnabled = ledColorEnabled;
    }

    public void setLedColor(String ledColor) {
        this.ledColor = ledColor;
    }

    public void setLockScreen(String lockScreen) {
        this.lockScreen = lockScreen;
    }

    public void setImportance(String importance) {
        this.importance = importance;
    }

    public void setBadgeDisabled(Boolean badgeDisabled) {
        this.badgeDisabled = badgeDisabled;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setForegroundColor(String foregroundColor) {
        this.foregroundColor = foregroundColor;
    }
}
