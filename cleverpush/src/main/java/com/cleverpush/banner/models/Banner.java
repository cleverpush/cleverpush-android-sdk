package com.cleverpush.banner.models;

import android.util.Log;

import com.cleverpush.banner.models.blocks.BannerBackground;
import com.cleverpush.banner.models.blocks.BannerBlock;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class Banner {
    private static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    private String id;
    private String testId;
    private String channel;
    private String name;
    private BannerType type;
    private BannerStatus status;
    private boolean carouselEnabled;
    private List<BannerBlock> blocks;
    private List<BannerScreens> screens;
    private BannerBackground background;
    private Date startAt;
    private BannerDismissType dismissType;
    private int dismissTimeout;
    private BannerStopAtType stopAtType;
    private BannerTriggerType triggerType;
    private List<BannerTrigger> triggers;
    private Date stopAt;
    private BannerFrequency frequency;
    private Date createdAt;
    private int delaySeconds;
    private boolean scheduled;
    private String content;
    private String contentType;
    private String positionType;
    private BannerSubscribedType subscribedType;
    private List<String> tags;
    private List<String> excludeTags;
    private List<String> topics;
    private List<String> excludeTopics;
    private List<HashMap<String, String>> attributes;
    private boolean marginEnabled;
    private boolean closeButtonEnabled;
    private BannerAppVersionFilterRelation bannerAppVersionFilterRelation;
    private String appVersionFilterValue;
    private boolean enableMultipleScreens;

    private Banner() {
    }

    public String getId() {
        return id;
    }

    public String getTestId() {
        return testId;
    }

    public String getChannel() {
        return channel;
    }

    public String getName() {
        return name;
    }

    public BannerType getType() {
        return type;
    }

    public BannerStatus getStatus() {
        return status;
    }

    public boolean isCarouselEnabled() {
        return carouselEnabled;
    }

    public List<BannerBlock> getBlocks() {
        return blocks;
    }

    public List<BannerScreens> getScreens() {
        return screens;
    }

    public BannerBackground getBackground() {
        return background;
    }

    public Date getStartAt() {
        return startAt;
    }

    public BannerDismissType getDismissType() {
        return dismissType;
    }

    public BannerAppVersionFilterRelation getBannerAppVersionFilterRelation() {
        return bannerAppVersionFilterRelation;
    }

    public int getDismissTimeout() {
        return dismissTimeout;
    }

    public int getDelaySeconds() {
        return delaySeconds;
    }

    public void setDelaySeconds(int delaySeconds) {
        this.delaySeconds = delaySeconds;
    }

    public BannerStopAtType getStopAtType() {
        return stopAtType;
    }

    public BannerTriggerType getTriggerType() {
        return triggerType;
    }

    public List<BannerTrigger> getTriggers() {
        return triggers;
    }

    public Date getStopAt() {
        return stopAt;
    }

    public BannerFrequency getFrequency() {
        return frequency;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setScheduled() {
        scheduled = true;
    }

    public boolean isScheduled() {
        return scheduled;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getPositionType() {
        return positionType;
    }

    public String getAppVersionFilterValue() {
        return appVersionFilterValue;
    }

    public boolean getEnableMultipleScreens() {
        return enableMultipleScreens;
    }

    public void setAppVersionFilterValue(String appVersionFilterValue) {
        this.appVersionFilterValue = appVersionFilterValue;
    }

    public void setPositionType(String positionType) {
        this.positionType = positionType;
    }

    public void setSubscribedType(BannerSubscribedType subscribedType) {
        this.subscribedType = subscribedType;
    }

    public BannerSubscribedType getSubscribedType() {
        return subscribedType;
    }

    public List<String> getTags() {
        return tags;
    }

    public List<String> getExcludeTags() {
        return excludeTags;
    }

    public List<String> getTopics() {
        return topics;
    }

    public List<String> getExcludeTopics() {
        return excludeTopics;
    }

    public List<HashMap<String, String>> getAttributes() {
        return attributes;
    }


    public boolean isMarginEnabled() {
        return marginEnabled;
    }

    public boolean isCloseButtonEnabled() {
        return closeButtonEnabled;
    }

    public static Banner create(JSONObject json) throws JSONException {
        Banner banner = new Banner();

        banner.id = json.getString("_id");
        if (json.has("testId")) {
            banner.testId = json.getString("testId");
        }
        banner.channel = json.getString("channel");
        banner.name = json.getString("name");
        banner.type = BannerType.fromString(json.optString("type"));
        banner.status = BannerStatus.fromString(json.optString("status"));
        banner.blocks = new LinkedList<>();
        banner.screens = new LinkedList<>();
        banner.content = json.optString("content");
        banner.contentType = json.optString("contentType");


        Log.e("Tag 2",""+json);

        JSONArray blockArray = json.getJSONArray("blocks");
        for (int i = 0; i < blockArray.length(); ++i) {
            banner.blocks.add(BannerBlock.create(blockArray.getJSONObject(i)));
        }

        if (json.has("screens")) {
            JSONArray screens = json.getJSONArray("screens");
            for (int i = 0; i < screens.length(); ++i) {
                banner.screens.add(BannerScreens.create(screens.getJSONObject(i)));
            }
        }

        banner.background = BannerBackground.create(json.optJSONObject("background"));

        try {
            SimpleDateFormat format = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                format = new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT, Locale.US);
            }
            banner.startAt = format.parse(json.optString("startAt"));
        } catch (ParseException e) {
            banner.startAt = new Date();
        }

        banner.dismissType = BannerDismissType.fromString(json.optString("dismissType"));
        banner.bannerAppVersionFilterRelation = BannerAppVersionFilterRelation.fromString(json.optString("appVersionFilterRelation"));
        banner.dismissTimeout = json.getInt("dismissTimeout");
        banner.stopAtType = BannerStopAtType.fromString(json.optString("stopAtType"));

        banner.triggerType = BannerTriggerType.fromString(json.optString("triggerType"));

        banner.triggers = new LinkedList<>();
        JSONArray triggersArray = json.optJSONArray("triggers");
        if (triggersArray != null) {
            for (int i = 0; i < triggersArray.length(); ++i) {
                banner.triggers.add(BannerTrigger.create(triggersArray.getJSONObject(i)));
            }
        }

        try {
            SimpleDateFormat format = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                format = new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT, Locale.US);
            }
            banner.stopAt = json.isNull("stopAt") ? null : format.parse(json.optString("stopAt"));
        } catch (ParseException e) {
            banner.stopAt = null;
        }

        try {
            SimpleDateFormat format = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                format = new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT, Locale.US);
            }
            banner.createdAt = json.isNull("createdAt") ? null : format.parse(json.optString("createdAt"));
        } catch (ParseException e) {
            banner.createdAt = null;
        }

        banner.frequency = BannerFrequency.fromString(json.optString("frequency"));
        banner.positionType = json.optString("type");
        banner.appVersionFilterValue = json.optString("appVersionFilterValue");

        banner.subscribedType = BannerSubscribedType.fromString(json.optString("subscribedType"));

        JSONArray tagsArray = json.optJSONArray("tags");
        if (tagsArray != null) {
            banner.tags = new ArrayList<>();
            for (int i = 0; i < tagsArray.length(); ++i) {
                String tag = tagsArray.optString(i);
                if (tag != null) {
                    banner.tags.add(tag);
                }
            }
        }

        JSONArray excludeTagsArray = json.optJSONArray("excludeTags");
        if (excludeTagsArray != null) {
            banner.excludeTags = new ArrayList<>();
            for (int i = 0; i < excludeTagsArray.length(); ++i) {
                String tag = excludeTagsArray.optString(i);
                if (tag != null) {
                    banner.excludeTags.add(tag);
                }
            }
        }

        JSONArray topicsArray = json.optJSONArray("topics");
        if (topicsArray != null) {
            banner.topics = new ArrayList<>();
            for (int i = 0; i < topicsArray.length(); ++i) {
                String topic = topicsArray.optString(i);
                if (topic != null) {
                    banner.topics.add(topic);
                }
            }
        }

        JSONArray excludeTopicsArray = json.optJSONArray("excludeTopics");
        if (excludeTopicsArray != null) {
            banner.excludeTopics = new ArrayList<>();
            for (int i = 0; i < excludeTopicsArray.length(); ++i) {
                String topic = excludeTopicsArray.optString(i);
                if (topic != null) {
                    banner.excludeTopics.add(topic);
                }
            }
        }

        JSONArray attributesArray = json.optJSONArray("attributes");
        if (attributesArray != null) {
            banner.attributes = new ArrayList<>();
            for (int i = 0; i < attributesArray.length(); ++i) {
                JSONObject attribute = attributesArray.optJSONObject(i);
                if (attribute != null) {
                    String attributeId = attribute.optString("id");
                    String attributeValue = attribute.optString("value");
                    if (attributeId != null && attributeValue != null) {
                        HashMap<String, String> attributeMap = new HashMap<>();
                        attributeMap.put("id", attributeId);
                        attributeMap.put("value", attributeValue);
                        banner.attributes.add(attributeMap);
                    }
                }
            }
        }


        if (json.has("marginEnabled")) {
            banner.marginEnabled = json.optBoolean("marginEnabled");
        }

        if (json.has("closeButtonEnabled")) {
            banner.closeButtonEnabled = json.optBoolean("closeButtonEnabled");
        }

        if (json.has("enableMultipleScreens")){
            banner.enableMultipleScreens = json.optBoolean("enableMultipleScreens");
        }

        if (json.has("carouselEnabled")){
            banner.carouselEnabled = json.optBoolean("carouselEnabled");
        }


        return banner;
    }
}
