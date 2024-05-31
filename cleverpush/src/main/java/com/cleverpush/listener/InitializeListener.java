package com.cleverpush.listener;

public interface InitializeListener {
  void onInitialized();
  default void onInitializationSuccess() { // optional method
    // Default implementation (do nothing)
  }

  default void onInitializationFailure(Throwable throwable) {  // optional method
    // Default implementation (do nothing)
  }
}
