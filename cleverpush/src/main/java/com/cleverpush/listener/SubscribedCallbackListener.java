package com.cleverpush.listener;

public interface SubscribedCallbackListener {
  void onSuccess(String subscriptionId);

  void onFailure(Throwable exception);
}
