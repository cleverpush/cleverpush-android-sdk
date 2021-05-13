package com.cleverpush;

import java.util.Map;

public class  ChannelTopic {
    private String id;
    private String name;
    private String parentTopicId;
    private Boolean defaultUnchecked;
    private String fcmBroadcastTopic;
	private String externalId;
	private Map<String, String> customData;

    public ChannelTopic(String id, String name, String parentTopicId, Boolean defaultUnchecked, String fcmBroadcastTopic, String externalId, Map<String, String> customData) {
        this.id = id;
        this.name = name;
        this.parentTopicId = parentTopicId;
        this.defaultUnchecked = defaultUnchecked;
		this.fcmBroadcastTopic = fcmBroadcastTopic;
		this.externalId = externalId;
		this.customData = customData;
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

	public String getExternalId() {
		return this.externalId;
	}

	public Map getCustomData() {
    	return this.customData;
	}

    public String toString() {
        return this.getId();
    }
}
