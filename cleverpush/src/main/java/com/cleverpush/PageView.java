package com.cleverpush;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Map;

public class PageView implements Serializable {
    String url;
    Map<String, ?> params;

    PageView(String url, Map<String, ?> params) {
        this.url = url;
        this.params = params;
    }

    public String getUrl() {
        return url;
    }

    public Map<String, ?> getParams() {
        return params;
    }
}
