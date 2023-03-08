package com.cleverpush.banner.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BannerTriggerCondition {
	private BannerTriggerConditionType type;
	private String event;
	private List<BannerTriggerConditionEventProperty> eventProperties;
	private String relation;
	private int sessions;
	private int seconds;

    private BannerTriggerCondition() {}

	public BannerTriggerConditionType getType() { return type; }

	public String getEvent() { return event; }

	public List<BannerTriggerConditionEventProperty> getEventProperties() { return eventProperties; }

	public int getSessions() { return sessions; }

	public int getSeconds() { return seconds; }

	public String getRelation() { return relation; }

    public static BannerTriggerCondition create(JSONObject json) {
        BannerTriggerCondition condition = new BannerTriggerCondition();

        if (json != null) {
			condition.type = BannerTriggerConditionType.fromString(json.optString("type"));
			condition.event = json.optString("event");
			condition.sessions = json.optInt("sessions");
			condition.seconds = json.optInt("seconds");
			condition.relation = json.optString("operator");

			condition.eventProperties = new ArrayList<>();
			JSONArray eventPropertiesArray = json.optJSONArray("eventProperties");
			if (eventPropertiesArray != null) {
				for (int i = 0; i < eventPropertiesArray.length(); ++i) {
					try {
						BannerTriggerConditionEventProperty property = BannerTriggerConditionEventProperty.create(eventPropertiesArray.getJSONObject(i));
						condition.eventProperties.add(property);
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}
		}

        return condition;
    }
}
