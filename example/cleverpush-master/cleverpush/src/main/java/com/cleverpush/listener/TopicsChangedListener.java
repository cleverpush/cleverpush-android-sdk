package com.cleverpush.listener;

import com.cleverpush.ChannelTopic;

import java.util.Set;

public interface TopicsChangedListener {
    void changed(Set<String> topicIds);
}
