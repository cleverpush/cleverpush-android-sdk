
package com.cleverpush.stories.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Content implements Serializable {

    @SerializedName("version")
    @Expose
    private Integer version;
    @SerializedName("supportsLandscape")
    @Expose
    private Boolean supportsLandscape;
    @SerializedName("title")
    @Expose
    private String title;
    @SerializedName("canonicalUrl")
    @Expose
    private String canonicalUrl;
    @SerializedName("preview")
    @Expose
    private Preview preview;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Boolean getSupportsLandscape() {
        return supportsLandscape;
    }

    public void setSupportsLandscape(Boolean supportsLandscape) {
        this.supportsLandscape = supportsLandscape;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCanonicalUrl() {
        return canonicalUrl;
    }

    public void setCanonicalUrl(String canonicalUrl) {
        this.canonicalUrl = canonicalUrl;
    }

    public Preview getPreview() {
        return preview;
    }

    public void setPreview(Preview preview) {
        this.preview = preview;
    }

}
