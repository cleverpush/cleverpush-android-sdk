package com.cleverpush;

public class ChannelTopic {
    private String id;
    private String name;
    private String parentTopicId;
    private Boolean defaultUnchecked;
    private String fcmBroadcastTopic;

    public ChannelTopic(String id, String name, String parentTopicId, Boolean defaultUnchecked, String fcmBroadcastTopic) {
        this.id = id;
        this.name = name;
        this.parentTopicId = parentTopicId;
        this.defaultUnchecked = defaultUnchecked;
        this.fcmBroadcastTopic = fcmBroadcastTopic;
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

    public Boolean getDefaultUnchecked() {
        return this.defaultUnchecked;
    }

    public String getFcmBroadcastTopic() {
        return this.fcmBroadcastTopic;
    }

    public String toString() {
        return this.getId();
    }
}
