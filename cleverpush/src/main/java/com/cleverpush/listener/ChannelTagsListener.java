package com.cleverpush.listener;

import com.cleverpush.ChannelTag;

import java.util.Set;

public interface ChannelTagsListener {
    void ready(Set<ChannelTag> tags);
}
