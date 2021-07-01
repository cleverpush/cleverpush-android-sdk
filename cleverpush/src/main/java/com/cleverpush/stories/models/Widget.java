
package com.cleverpush.stories.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class  Widget {

    @SerializedName("_id")
    @Expose
    private String id;
    @SerializedName("channel")
    @Expose
    private String channel;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("variant")
    @Expose
    private String variant;
    @SerializedName("maxStoriesNumber")
    @Expose
    private Integer maxStoriesNumber;
    @SerializedName("storyHeight")
    @Expose
    private Integer storyHeight;
    @SerializedName("position")
    @Expose
    private String position;
    @SerializedName("margin")
    @Expose
    private Integer margin;
    @SerializedName("display")
    @Expose
    private String display;
    @SerializedName("selectedStories")
    @Expose
    private List<Object> selectedStories = null;
    @SerializedName("createdAt")
    @Expose
    private String createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public Integer getMaxStoriesNumber() {
        return maxStoriesNumber;
    }

    public void setMaxStoriesNumber(Integer maxStoriesNumber) {
        this.maxStoriesNumber = maxStoriesNumber;
    }

    public Integer getStoryHeight() {
        return storyHeight;
    }

    public void setStoryHeight(Integer storyHeight) {
        this.storyHeight = storyHeight;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public Integer getMargin() {
        return margin;
    }

    public void setMargin(Integer margin) {
        this.margin = margin;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public List<Object> getSelectedStories() {
        return selectedStories;
    }

    public void setSelectedStories(List<Object> selectedStories) {
        this.selectedStories = selectedStories;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

}
