package com.cleverpush.listener;

public interface StopCampaignListener {
  void onSuccess();

  void onFailure(Throwable throwable);
}
