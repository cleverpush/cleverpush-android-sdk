package com.cleverpush;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Subscription implements Serializable {
    @SerializedName("_id")
    String id;

    String rawPayload;

    public String getId() {
        return id;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }
}
