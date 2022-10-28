package com.cleverpush.listener;

public interface UnsubscribedListener {
    void onSuccess();
    void onFailure(Throwable throwable);
}
