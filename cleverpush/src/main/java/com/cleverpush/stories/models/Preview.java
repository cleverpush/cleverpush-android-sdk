package com.cleverpush.stories.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Preview implements Serializable {

  @SerializedName("publisher")
  @Expose
  private String publisher;

  @SerializedName("publisherLogoSrc")
  @Expose
  private String publisherLogoSrc;

  @SerializedName("posterPortraitSrc")
  @Expose
  private String posterPortraitSrc;

  @SerializedName("publisherLogoWidth")
  @Expose
  private int publisherLogoWidth;

  @SerializedName("publisherLogoHeight")
  @Expose
  private int publisherLogoHeight;

  @SerializedName("posterLandscapeSrc")
  @Expose
  private String posterLandscapeSrc;

  @SerializedName("posterSquareSrc")
  @Expose
  private String posterSquareSrc;

  @SerializedName("widgetSrc")
  @Expose
  private String widgetSrc;

  @SerializedName("widgetDarkSrc")
  @Expose
  private String widgetDarkSrc;

  public String getPublisher() {
    return this.publisher;
  }

  public void setPublisher(String publisher) {
    this.publisher = publisher;
  }

  public String getPublisherLogoSrc() {
    return this.publisherLogoSrc;
  }

  public void setPublisherLogoSrc(String publisherLogoSrc) {
    this.publisherLogoSrc = publisherLogoSrc;
  }

  public String getPosterPortraitSrc() {
    return this.posterPortraitSrc;
  }

  public void setPosterPortraitSrc(String posterPortraitSrc) {
    this.posterPortraitSrc = posterPortraitSrc;
  }

  public int getPublisherLogoWidth() {
    return this.publisherLogoWidth;
  }

  public void setPublisherLogoWidth(int publisherLogoWidth) {
    this.publisherLogoWidth = publisherLogoWidth;
  }

  public int getPublisherLogoHeight() {
    return this.publisherLogoHeight;
  }

  public void setPublisherLogoHeight(int publisherLogoHeight) {
    this.publisherLogoHeight = publisherLogoHeight;
  }

  public String getPosterLandscapeSrc() {
    return this.posterLandscapeSrc;
  }

  public void setPosterLandscapeSrc(String posterLandscapeSrc) {
    this.posterLandscapeSrc = posterLandscapeSrc;
  }

  public String getPosterSquareSrc() {
    return this.posterSquareSrc;
  }

  public void setPosterSquareSrc(String posterSquareSrc) {
    this.posterSquareSrc = posterSquareSrc;
  }

  public String getWidgetSrc() {
    return widgetSrc;
  }

  public void setWidgetSrc(String widgetSrc) {
    this.widgetSrc = widgetSrc;
  }

  public String getWidgetDarkSrc() {
    return widgetDarkSrc;
  }

  public void setWidgetDarkSrc(String widgetDarkSrc) {
    this.widgetDarkSrc = widgetDarkSrc;
  }
}
