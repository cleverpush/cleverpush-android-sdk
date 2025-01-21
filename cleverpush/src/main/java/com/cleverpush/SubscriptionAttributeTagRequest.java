package com.cleverpush;

public class SubscriptionAttributeTagRequest {
    private String attributeId;
    private String value;
    private String[] values;
    private String tagId;
    private String[] tagIds;

    public SubscriptionAttributeTagRequest(String attributeId, String value) {
        this.attributeId = attributeId;
        this.value = value;
    }

    public SubscriptionAttributeTagRequest(String attributeId, String[] values) {
        this.attributeId = attributeId;
        this.values = values;
    }

    public SubscriptionAttributeTagRequest(String tagId) {
        this.tagId = tagId;
    }

    public SubscriptionAttributeTagRequest(String[] tagIds) {
        this.tagIds = tagIds;
    }

    public String getAttributeId() {
        return attributeId;
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
