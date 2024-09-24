
package com.cleverpush.stories.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class Content implements Serializable {

  @SerializedName("version")
  @Expose
  private int version;

  @SerializedName("supportsLandscape")
  @Expose
  private boolean supportsLandscape;

  @SerializedName("title")
  @Expose
  private String title;

  @SerializedName("canonicalUrl")
  @Expose
  private String canonicalUrl;

  @SerializedName("preview")
  @Expose
  private Preview preview;

  @SerializedName("pages")
  @Expose
  private List<Object> pages;

  @SerializedName("subtitle")
  @Expose
  private String subtitle;

  public int getVersion() {
    return this.version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public boolean isSupportsLandscape() {
    return this.supportsLandscape;
  }

  public void setSupportsLandscape(boolean supportsLandscape) {
    this.supportsLandscape = supportsLandscape;
  }

  public String getTitle() {
    return this.title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getCanonicalUrl() {
    return this.canonicalUrl;
  }

  public void setCanonicalUrl(String canonicalUrl) {
    this.canonicalUrl = canonicalUrl;
  }

  public Preview getPreview() {
    return this.preview;
  }

  public void setPreview(Preview preview) {
    this.preview = preview;
  }

  public List<Object> getPages() {
    return pages;
  }

  public void setPages(List<Object> pages) {
    this.pages = pages;
  }

  public String getSubtitle() {
    return subtitle;
  }

  public void setSubtitle(String subtitle) {
    this.subtitle = subtitle;
  }
}
