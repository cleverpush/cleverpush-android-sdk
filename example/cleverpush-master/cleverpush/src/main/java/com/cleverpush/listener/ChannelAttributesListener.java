package com.cleverpush.listener;

import com.cleverpush.CustomAttribute;

import java.util.Set;

public interface ChannelAttributesListener {
    void ready(Set<CustomAttribute> attributes);
}
