package com.cleverpush.banner.models;

import org.json.JSONObject;

public class BannerTriggerCondition {
	private BannerTriggerConditionType type;
	private String key;
	private String value;
	private String relation;
	private int sessions;
	private int seconds;

    private BannerTriggerCondition() {}

	public BannerTriggerConditionType getType() { return type; }

	public String getKey() { return key; }

	public String getValue() { return value; }

	public int getSessions() { return sessions; }

	public int getSeconds() { return seconds; }

	public String getRelation() { return relation; }

    public static BannerTriggerCondition create(JSONObject json) {
        BannerTriggerCondition banner = new BannerTriggerCondition();

        if (json != null) {
			banner.type = BannerTriggerConditionType.fromString(json.optString("type"));
			banner.key = json.optString("key");
			banner.value = json.optString("value");
			banner.sessions = json.optInt("sessions");
			banner.seconds = json.optInt("seconds");
			banner.relation = json.optString("operator");
		}

        return banner;
    }
}
