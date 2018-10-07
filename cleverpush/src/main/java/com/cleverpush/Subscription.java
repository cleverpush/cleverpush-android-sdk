package com.cleverpush;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Subscription implements Serializable {
    @SerializedName("_id")
    String id;

    public String getId() {
        return id;
    }
}
