package com.cleverpush;

import java.util.Map;

public class TriggeredEvent {
    private final String id;
    private final Map<String, Object> properties;

    public TriggeredEvent(String id, Map<String, Object> properties) {
        this.id = id;
        this.properties = properties;
    }

    public String getId() {
        return id;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}
