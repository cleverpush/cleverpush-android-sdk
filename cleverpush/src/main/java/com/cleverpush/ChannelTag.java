package com.cleverpush;

public class ChannelTag {
    private String id;
    private String name;

    public ChannelTag(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return this.getId();
    }
}
