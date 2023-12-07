package com.cleverpush.listener;

public abstract class InitializeListener {
  public abstract void onInitialized();

  public void onFailure(Throwable throwable) {
    // Default implementation for onFailure
  }
}
