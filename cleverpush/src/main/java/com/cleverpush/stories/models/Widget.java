
package com.cleverpush.stories.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

public class Widget {

    public static final long serialVersionUID = -7102250436681274220L;

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
    private int maxStoriesNumber;

    @SerializedName("storyHeight")
    @Expose
    private int storyHeight;

    @SerializedName("position")
    @Expose
    private String position;

    @SerializedName("margin")
    @Expose
    private int margin;

    @SerializedName("display")
    @Expose
    private String display;

    @SerializedName("selectedStories")
    @Expose
    private List<Object> selectedStories = Collections.emptyList();

    @SerializedName("createdAt")
    @Expose
    private String createdAt;

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

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVariant() {
        return this.variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public int getMaxStoriesNumber() {
        return this.maxStoriesNumber;
    }

    public void setMaxStoriesNumber(int maxStoriesNumber) {
        this.maxStoriesNumber = maxStoriesNumber;
    }

    public int getStoryHeight() {
        return this.storyHeight;
    }

    public void setStoryHeight(int storyHeight) {
        this.storyHeight = storyHeight;
    }

    public String getPosition() {
        return this.position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public int getMargin() {
        return this.margin;
    }

    public void setMargin(int margin) {
        this.margin = margin;
    }

    public String getDisplay() {
        return this.display;
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
