package com.cleverpush;

public class SubscriptionAttributeTagRequest {
    private String attributeId;
    private String value;
    private String[] values;
    private String[] attributeIds;
    private String tagId;
    private String[] tagIds;

    private SubscriptionAttributeTagRequest() {
        // private constructor
    }

    public static SubscriptionAttributeTagRequest forAttribute(String attributeId) {
        SubscriptionAttributeTagRequest request = new SubscriptionAttributeTagRequest();
        request.attributeId = attributeId;
        return request;
    }

    public static SubscriptionAttributeTagRequest forAttributes(String[] attributeIds) {
        SubscriptionAttributeTagRequest request = new SubscriptionAttributeTagRequest();
        request.attributeIds = attributeIds;
        return request;
    }

    public static SubscriptionAttributeTagRequest forAttributeValue(String attributeId, String value) {
        SubscriptionAttributeTagRequest request = new SubscriptionAttributeTagRequest();
        request.attributeId = attributeId;
        request.value = value;
        return request;
    }

    public static SubscriptionAttributeTagRequest forAttributeValues(String attributeId, String[] values) {
        SubscriptionAttributeTagRequest request = new SubscriptionAttributeTagRequest();
        request.attributeId = attributeId;
        request.values = values;
        return request;
    }

    public static SubscriptionAttributeTagRequest forTag(String tagId) {
        SubscriptionAttributeTagRequest request = new SubscriptionAttributeTagRequest();
        request.tagId = tagId;
        return request;
    }

    public static SubscriptionAttributeTagRequest forTags(String[] tagIds) {
        SubscriptionAttributeTagRequest request = new SubscriptionAttributeTagRequest();
        request.tagIds = tagIds;
        return request;
    }

    public String getAttributeId() {
        return attributeId;
    }

    public String[] getAttributeIds() {
        return attributeIds;
    }

    public String getValue() {
        return value;
    }

    public String[] getValues() {
        return values;
    }

    public String getTagId() {
        return tagId;
    }

    public String[] getTagIds() {
        return tagIds;
    }
}
