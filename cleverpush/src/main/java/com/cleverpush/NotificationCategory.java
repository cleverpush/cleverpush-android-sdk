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
	@SerializedName("badgesEnabled")
	Boolean badgesEnabled;
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
        return soundEnabled;
    }

	public String getSoundFilename() {
		return soundFilename;
	}

	public Boolean getVibrationEnabled() {
		return vibrationEnabled;
	}

	public String getVibrationPattern() {
		return vibrationPattern;
	}

	public Boolean getLedColorEnabled() {
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

	public Boolean getBadgesEnabled() {
		return badgesEnabled;
	}

	public String getBackgroundColor() {
		return backgroundColor;
	}

	public String getForegroundColor() {
		return foregroundColor;
	}
}
