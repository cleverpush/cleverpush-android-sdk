package com.cleverpush.chat;

import android.content.Context;
import android.webkit.JavascriptInterface;

import com.cleverpush.CleverPush;
import com.cleverpush.listener.ChatSubscribeListener;

public class ChatJavascriptInterface {
  private Context context;
  private ChatView chatView;

  ChatJavascriptInterface(Context context, ChatView chatView) {
    this.context = context;
    this.chatView = chatView;
  }

  @JavascriptInterface
  public void subscribe() {
    new Thread(() -> {
      ChatSubscribeListener subscribeListener = CleverPush.getInstance(context).getChatSubscribeListener();
      if (subscribeListener != null) {
        subscribeListener.subscribe();
        return;
      }

      CleverPush.getInstance(context).subscribe();
      chatView.loadChat();
    }).start();
  }

  @JavascriptInterface
  public void reload() {
    new Thread(() -> {
      if (chatView.getLastSubscriptionId() != null) {
        chatView.loadChat(chatView.getLastSubscriptionId());
      } else {
        chatView.loadChat();
      }
    }).start();
  }
}