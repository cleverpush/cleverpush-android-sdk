package com.cleverpush.listener;

import com.cleverpush.ChannelTopic;

import java.util.Set;

public interface ChannelTopicsListener {
    void ready(Set<ChannelTopic> topics);
}
