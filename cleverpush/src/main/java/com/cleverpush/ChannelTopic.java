package com.cleverpush;

public class ChannelTopic {
    private String id;
    private String name;

    public ChannelTopic(String id, String name) {
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
