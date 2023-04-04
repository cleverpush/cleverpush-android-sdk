package com.cleverpush.listener;

public interface CompletionFailureListener {
  void onComplete();

  void onFailure(Exception exception);
}
