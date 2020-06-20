package com.cleverpush;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class NotificationCategoryGroup implements Serializable {
    @SerializedName("_id")
    String id;
    @SerializedName("name")
    String name;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
