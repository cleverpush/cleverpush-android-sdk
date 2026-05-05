package com.cleverpush;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.json.JSONObject;

public class ChannelTopic {
  private String id;
  private String name;
  private String parentTopicId;
  private Boolean defaultUnchecked;
  private String fcmBroadcastTopic;
  private String externalId;
  private Map<String, String> customData;
  private Boolean nameTranslationEnabled;
  private Map<String, String> nameTranslation;

  public ChannelTopic(String id, String name, String parentTopicId, Boolean defaultUnchecked, String fcmBroadcastTopic,
                      String externalId, Map<String, String> customData, Boolean nameTranslationEnabled, Map<String, String> nameTranslation) {
    this.id = id;
    this.name = name;
    this.parentTopicId = parentTopicId;
    this.defaultUnchecked = defaultUnchecked;
    this.fcmBroadcastTopic = fcmBroadcastTopic;
    this.externalId = externalId;
    this.customData = customData;
    this.nameTranslationEnabled = nameTranslationEnabled;
    this.nameTranslation = nameTranslation;
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

  public Boolean getNameTranslationEnabled() {
    return nameTranslationEnabled;
  }

  public Map<String, String> getNameTranslation() {
    return nameTranslation;
  }

  /**
   * Parses {@code nameTranslation} from channel topic JSON into a map, or returns null if absent or invalid.
   */
  public static Map<String, String> parseNameTranslation(JSONObject topicObject) {
    if (topicObject == null || !topicObject.has("nameTranslation")) {
      return null;
    }
    try {
      JSONObject translationObj = topicObject.optJSONObject("nameTranslation");
      if (translationObj == null) {
        return null;
      }
      Map<String, String> nameTranslationMap = new HashMap<>();
      Iterator<String> keys = translationObj.keys();
      while (keys.hasNext()) {
        String key = keys.next();
        nameTranslationMap.put(key, translationObj.optString(key));
      }
      return nameTranslationMap;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Resolves the user-visible topic name: when {@code nameTranslationEnabled} is true, uses the device
   * locale's language code to look up {@code nameTranslation}; otherwise or if no translation exists, uses {@code name}.
   */
  public static String resolveLocalizedDisplayName(JSONObject topicObject) {
    if (topicObject == null) {
      return "";
    }
    String name = topicObject.optString("name");
    if (!topicObject.optBoolean("nameTranslationEnabled", false)) {
      return name;
    }
    Map<String, String> translations = parseNameTranslation(topicObject);
    if (translations == null || translations.isEmpty()) {
      return name;
    }
    String language = Locale.getDefault().getLanguage();
    String translated = translations.get(language);
    if (translated != null && !translated.isEmpty()) {
      return translated;
    }
    return name;
  }
}
