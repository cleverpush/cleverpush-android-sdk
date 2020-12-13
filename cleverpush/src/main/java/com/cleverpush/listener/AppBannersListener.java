package com.cleverpush.listener;

import com.cleverpush.banner.models.Banner;

import java.util.List;

public interface AppBannersListener {
    void ready(List<Banner> banners);
}
