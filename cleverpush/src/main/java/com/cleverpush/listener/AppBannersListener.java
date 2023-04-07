package com.cleverpush.listener;

import com.cleverpush.banner.models.Banner;

import java.util.Collection;

public interface AppBannersListener {
  void ready(Collection<Banner> banners);
}
