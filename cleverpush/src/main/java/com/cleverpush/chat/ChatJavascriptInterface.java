package com.cleverpush.chat;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.webkit.JavascriptInterface;

import com.cleverpush.CleverPush;
import com.cleverpush.listener.ChatSubscribeListener;
import com.cleverpush.listener.SubscribedCallbackListener;
import com.cleverpush.util.Logger;

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

      CleverPush.getInstance(context).subscribe(new SubscribedCallbackListener() {
        @Override
        public void onSuccess(String subscriptionId) {
          chatView.loadChat();
        }

        @Override
        public void onFailure(Throwable exception) {
          try {
            if (exception.getLocalizedMessage().equalsIgnoreCase("Can not subscribe because the notification permission has been denied by the user. " +
                    "You can call CleverPush.setIgnoreDisabledNotificationPermission(true) to still allow subscriptions, e.g. for silent pushes.")); {
              Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
              Uri uri = Uri.fromParts("package", context.getPackageName(), null);
              intent.setData(uri);
              intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
              context.startActivity(intent);
            }
          } catch (Exception e) {
            Logger.e("CleverPush", "Error while opening app settings: " + e.getMessage(), e);
          }
        }
      });
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