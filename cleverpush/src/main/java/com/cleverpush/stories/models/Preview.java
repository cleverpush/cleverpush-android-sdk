
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
    private Integer publisherLogoWidth;
    @SerializedName("publisherLogoHeight")
    @Expose
    private Integer publisherLogoHeight;
    @SerializedName("posterLandscapeSrc")
    @Expose
    private String posterLandscapeSrc;
    @SerializedName("posterSquareSrc")
    @Expose
    private String posterSquareSrc;

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getPublisherLogoSrc() {
        return publisherLogoSrc;
    }

    public void setPublisherLogoSrc(String publisherLogoSrc) {
        this.publisherLogoSrc = publisherLogoSrc;
    }

    public String getPosterPortraitSrc() {
        return posterPortraitSrc;
    }

    public void setPosterPortraitSrc(String posterPortraitSrc) {
        this.posterPortraitSrc = posterPortraitSrc;
    }

    public Integer getPublisherLogoWidth() {
        return publisherLogoWidth;
    }

    public void setPublisherLogoWidth(Integer publisherLogoWidth) {
        this.publisherLogoWidth = publisherLogoWidth;
    }

    public Integer getPublisherLogoHeight() {
        return publisherLogoHeight;
    }

    public void setPublisherLogoHeight(Integer publisherLogoHeight) {
        this.publisherLogoHeight = publisherLogoHeight;
    }

    public String getPosterLandscapeSrc() {
        return posterLandscapeSrc;
    }

    public void setPosterLandscapeSrc(String posterLandscapeSrc) {
        this.posterLandscapeSrc = posterLandscapeSrc;
    }

    public String getPosterSquareSrc() {
        return posterSquareSrc;
    }

    public void setPosterSquareSrc(String posterSquareSrc) {
        this.posterSquareSrc = posterSquareSrc;
    }

}
