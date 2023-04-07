
package com.cleverpush.stories.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Story implements Serializable {

  public static final long serialVersionUID = -2893386129178459307L;

  @SerializedName("_id")
  @Expose
  private String id;

  @SerializedName("channel")
  @Expose
  private String channel;

  @SerializedName("title")
  @Expose
  private String title;

  @SerializedName("content")
  @Expose
  private Content content;

  private boolean opened = false;

  public String getId() {
    return this.id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getChannel() {
    return this.channel;
  }

  public void setChannel(String channel) {
    this.channel = channel;
  }

  public String getTitle() {
    return this.title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Content getContent() {
    return this.content;
  }

  public void setContent(Content content) {
    this.content = content;
  }

  public boolean isOpened() {
    return this.opened;
  }

  public void setOpened(boolean opened) {
    this.opened = opened;
  }
}
