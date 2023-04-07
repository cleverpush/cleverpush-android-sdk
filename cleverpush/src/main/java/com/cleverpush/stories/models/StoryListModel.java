
package com.cleverpush.stories.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

public class StoryListModel {

  public static final long serialVersionUID = -3003827553749596304L;

  @SerializedName("widget")
  @Expose
  private Widget widget;

  @SerializedName("stories")
  @Expose
  private List<Story> stories = Collections.emptyList();

  public Widget getWidget() {
    return this.widget;
  }

  public void setWidget(Widget widget) {
    this.widget = widget;
  }

  public List<Story> getStories() {
    return this.stories;
  }

  public void setStories(List<Story> stories) {
    this.stories = stories;
  }

}
