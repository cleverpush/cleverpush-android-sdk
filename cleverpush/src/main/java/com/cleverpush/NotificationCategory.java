package com.cleverpush;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class NotificationCategory implements Serializable {
    @SerializedName("_id")
    String id;
    @SerializedName("name")
    String name;
    @SerializedName("description")
    String description;
    @SerializedName("group")
    NotificationCategoryGroup group;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public NotificationCategoryGroup getGroup() {
        return group;
    }
}
