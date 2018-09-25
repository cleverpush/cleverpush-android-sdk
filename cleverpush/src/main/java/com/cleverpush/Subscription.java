package com.cleverpush;

import com.google.gson.annotations.SerializedName;

public class Subscription {
    @SerializedName("_id")
    String id;

    public String getId() {
        return id;
    }
}
