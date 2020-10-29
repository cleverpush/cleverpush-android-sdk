package com.cleverpush;

public class ChannelTopic {
    private String id;
    private String name;
    private String parentTopicId;

    public ChannelTopic(String id, String name, String parentTopicId) {
        this.id = id;
        this.name = name;
        this.parentTopicId = parentTopicId;
    }

    public String getId() {
        return id;
    }

    public String getParentTopicId() {
        return parentTopicId;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return this.getId();
    }
}
