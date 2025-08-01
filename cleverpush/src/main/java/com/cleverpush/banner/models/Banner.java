package com.cleverpush.banner.models;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Build;

import com.cleverpush.banner.models.blocks.BannerBackground;
import com.cleverpush.banner.models.blocks.BannerBlock;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class Banner {
  private static String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

  public static final String CONTENT_TYPE_HTML = "html";

  private String id;
  private String testId;
  private String channel;
  private String name;
  private String group;
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
  private List<String> languages;
  private List<HashMap<String, String>> attributes;
  private boolean marginEnabled;
  private boolean closeButtonEnabled;
  private boolean closeButtonPositionStaticEnabled;
  private CheckFilterRelation bannerAppVersionFilterRelation;
  private String appVersionFilterValue;
  private String fromVersion;
  private String toVersion;
  private boolean enableMultipleScreens;
  private boolean darkModeEnabled;
  private List<String> connectedBanners;
  private String title;
  private String description;
  private String mediaUrl;
  private List<BannerTargetEvent> eventFilters;
  private NotificationPermission notificationPermission;
  private int everyXDays;
  private BannerAttributesLogicType attributesLogic = BannerAttributesLogicType.And;
  private List<BannerOSTarget> osTarget;

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

  public String getGroup() {
    return group;
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

  public CheckFilterRelation getBannerAppVersionFilterRelation() {
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

  public String getFromVersion() {
    return fromVersion;
  }

  public String getToVersion() {
    return toVersion;
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

  public List<String> getLanguages() {
    return languages;
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

  public boolean isCloseButtonPositionStaticEnabled() {
    return closeButtonPositionStaticEnabled;
  }

  public boolean isDarkModeEnabled() {
    return darkModeEnabled;
  }

  public List<String> getConnectedBanners() {
    return connectedBanners;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public String getMediaUrl() {
    return mediaUrl;
  }

  public List<BannerTargetEvent> getEventFilters() {
    return eventFilters;
  }

  public NotificationPermission getNotificationPermission() {
    return notificationPermission;
  }

  public int getEveryXDays() {
    return everyXDays;
  }

  public BannerAttributesLogicType getAttributesLogic() {
    return attributesLogic;
  }

  public List<BannerOSTarget> getOsTarget() {
    return osTarget;
  }

  public static Banner create(JSONObject json) throws JSONException {
    Banner banner = new Banner();

    banner.id = json.getString("_id");
    if (json.has("testId")) {
      banner.testId = json.getString("testId");
    }
    banner.channel = json.getString("channel");
    banner.name = json.getString("name");
    if (json.has("group")) {
      banner.group = json.getString("group");
    }
    banner.type = BannerType.fromString(json.optString("type"));
    banner.status = BannerStatus.fromString(json.optString("status"));
    banner.blocks = new LinkedList<>();
    banner.screens = new LinkedList<>();
    banner.eventFilters = new LinkedList<>();
    banner.osTarget = new LinkedList<>();
    banner.content = json.optString("content");
    banner.contentType = json.optString("contentType");

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
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
        DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
      }
      format = new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT, Locale.US);
      banner.startAt = format.parse(json.optString("startAt"));
    } catch (Exception e) {
      banner.startAt = new Date();
    }

    banner.dismissType = BannerDismissType.fromString(json.optString("dismissType"));
    banner.bannerAppVersionFilterRelation = CheckFilterRelation.fromString(json.optString("appVersionFilterRelation"));
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
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
        DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
      }
      format = new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT, Locale.US);
      banner.stopAt = json.isNull("stopAt") ? null : format.parse(json.optString("stopAt"));
    } catch (Exception e) {
      banner.stopAt = null;
    }

    try {
      SimpleDateFormat format = null;
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
        DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
      }
      format = new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT, Locale.US);
      banner.createdAt = json.isNull("createdAt") ? null : format.parse(json.optString("createdAt"));
    } catch (Exception e) {
      banner.createdAt = null;
    }

    banner.frequency = BannerFrequency.fromString(json.optString("frequency"));
    banner.positionType = json.optString("type");
    banner.appVersionFilterValue = json.optString("appVersionFilterValue");
    banner.fromVersion = json.optString("fromVersion");
    banner.toVersion = json.optString("toVersion");

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

    JSONArray languages = json.optJSONArray("languages");
    if (languages != null) {
      banner.languages = new ArrayList<>();
      for (int i = 0; i < languages.length(); ++i) {
        String language = languages.optString(i);
        if (language != null) {
          banner.languages.add(language);
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
          String relation = attribute.optString("relation");
          if (attributeId != null && attributeValue != null) {
            HashMap<String, String> attributeMap = new HashMap<>();
            attributeMap.put("id", attributeId);
            attributeMap.put("value", attributeValue);
            if (relation != null) {
              attributeMap.put("relation", relation);
            }
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

    if (json.has("closeButtonPositionStaticEnabled")) {
      banner.closeButtonPositionStaticEnabled = json.optBoolean("closeButtonPositionStaticEnabled");
    }

    if (json.has("enableMultipleScreens")) {
      banner.enableMultipleScreens = json.optBoolean("enableMultipleScreens");
    }

    if (json.has("carouselEnabled")) {
      banner.carouselEnabled = json.optBoolean("carouselEnabled");
    }

    if (json.has("darkModeEnabled")) {
      banner.darkModeEnabled = json.optBoolean("darkModeEnabled");
    }

    if (json.optBoolean("connectedBannersEnabled")) {
      JSONArray connectedBannersArray = json.optJSONArray("connectedBanners");
      if (connectedBannersArray != null) {
        banner.connectedBanners = new ArrayList<>();
        for (int i = 0; i < connectedBannersArray.length(); ++i) {
          String connectedBannerId = connectedBannersArray.optString(i);
          if (connectedBannerId != null) {
            banner.connectedBanners.add(connectedBannerId);
          }
        }
      }
    }

    banner.title = json.optString("title");
    banner.description = json.optString("description");
    banner.mediaUrl = json.optString("mediaUrl");

    if (json.has("eventFilters")) {
      JSONArray eventFilters = json.getJSONArray("eventFilters");
      for (int i = 0; i < eventFilters.length(); ++i) {
        banner.eventFilters.add(BannerTargetEvent.create(eventFilters.getJSONObject(i)));
      }
    }

    if (json.has("notificationPermission")) {
      banner.notificationPermission = NotificationPermission.fromString(json.optString("notificationPermission"));
    }

    banner.everyXDays = json.optInt("everyXDays");

    if (json.has("attributesLogic")) {
      banner.attributesLogic = BannerAttributesLogicType.fromString(json.optString("attributesLogic"));
    }

    if (json.has("osTarget")) {
      JSONArray osTarget = json.getJSONArray("osTarget");
      for (int i = 0; i < osTarget.length(); ++i) {
        banner.osTarget.add(BannerOSTarget.create(osTarget.getJSONObject(i)));
      }
    }

    return banner;
  }

  public boolean isDarkModeEnabled(Activity activity) {
    int nightModeFlags = activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
    return this.darkModeEnabled && nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
  }
}
