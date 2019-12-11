package com.cleverpush.listener;

import org.json.JSONObject;

public interface ChannelConfigListener {
    void ready(JSONObject channelConfig);
}
