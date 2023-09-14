package com.cleverpush.chat;

import static com.cleverpush.Constants.LOG_TAG;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.cleverpush.CleverPush;
import com.cleverpush.listener.ChatUrlOpenedListener;
import com.cleverpush.util.Logger;

public class ChatView extends WebView {
  private String lastSubscriptionId;
  private Handler handler;
  String chatBackgroundColor;
  String chatSenderBubbleTextColor;
  String chatSenderBubbleBackgroundColor;
  String chatSendButtonBackgroundColor;
  String chatInputTextColor;
  String chatInputBackgroundColor;
  String chatReceiverBubbleBackgroundColor;
  String chatReceiverBubbleTextColor;
  String chatInputContainerBackgroundColor;
  String chatTimestampTextColor;

  public String getChatBackgroundColor() {
    return chatBackgroundColor;
  }

  public void setChatBackgroundColor(String chatBackgroundColor) {
    this.chatBackgroundColor = chatBackgroundColor;
  }

  public String getChatSenderBubbleTextColor() {
    return chatSenderBubbleTextColor;
  }

  public void setChatSenderBubbleTextColor(String chatSenderBubbleTextColor) {
    this.chatSenderBubbleTextColor = chatSenderBubbleTextColor;
  }

  public String getChatSenderBubbleBackgroundColor() {
    return chatSenderBubbleBackgroundColor;
  }

  public void setChatSenderBubbleBackgroundColor(String chatSenderBubbleBackgroundColor) {
    this.chatSenderBubbleBackgroundColor = chatSenderBubbleBackgroundColor;
  }

  public String getChatSendButtonBackgroundColor() {
    return chatSendButtonBackgroundColor;
  }

  public void setChatSendButtonBackgroundColor(String chatSendButtonBackgroundColor) {
    this.chatSendButtonBackgroundColor = chatSendButtonBackgroundColor;
  }

  public String getChatInputTextColor() {
    return chatInputTextColor;
  }

  public void setChatInputTextColor(String chatInputTextColor) {
    this.chatInputTextColor = chatInputTextColor;
  }

  public String getChatInputBackgroundColor() {
    return chatInputBackgroundColor;
  }

  public void setChatInputBackgroundColor(String chatInputBackgroundColor) {
    this.chatInputBackgroundColor = chatInputBackgroundColor;
  }

  public String getChatReceiverBubbleBackgroundColor() {
    return chatReceiverBubbleBackgroundColor;
  }

  public void setChatReceiverBubbleBackgroundColor(String chatReceiverBubbleBackgroundColor) {
    this.chatReceiverBubbleBackgroundColor = chatReceiverBubbleBackgroundColor;
  }

  public String getChatReceiverBubbleTextColor() {
    return chatReceiverBubbleTextColor;
  }

  public void setChatReceiverBubbleTextColor(String chatReceiverBubbleTextColor) {
    this.chatReceiverBubbleTextColor = chatReceiverBubbleTextColor;
  }

  public String getChatInputContainerBackgroundColor() {
    return chatInputContainerBackgroundColor;
  }

  public void setChatInputContainerBackgroundColor(String chatInputContainerBackgroundColor) {
    this.chatInputContainerBackgroundColor = chatInputContainerBackgroundColor;
  }

  public String getChatTimestampTextColor() {
    return chatTimestampTextColor;
  }

  public void setChatTimestampTextColor(String chatTimestampTextColor) {
    this.chatTimestampTextColor = chatTimestampTextColor;
  }

  public ChatView(Context context) {
    super(context);
    this.init();
  }

  public ChatView(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.init();
  }

  public void loadChat() {
    if (getCleverPushInstance().isSubscribed()) {
      getCleverPushInstance().getSubscriptionId(subscriptionId -> {
        this.loadChat(subscriptionId);
      });
    } else {
      Logger.d(LOG_TAG, "loadChat: There is no subscription for CleverPush SDK.");
      this.loadChat("preview");
    }
  }

  public void lockChat() {
    loadChat("preview");
  }

  public void loadChat(String subscriptionId) {
    lastSubscriptionId = subscriptionId;

    Context context = this.getContext();
    WebView webView = this;

    boolean isSubscriptionChanged = CleverPush.getInstance(CleverPush.context).isSubscriptionChanged();
    if (isSubscriptionChanged
            || !CleverPush.getInstance(CleverPush.context).isSubscribed()) {
      CleverPush.getInstance(CleverPush.context).setSubscriptionChanged(false);
      WebStorage.getInstance().deleteAllData();
    }

    if (this.handler == null) {
      this.handler = new Handler();
    }

    new Thread(() -> {
      getCleverPushInstance().getChannelConfig(config -> {
        String configJson = config != null ? config.toString() : "null";
        String brandingColorStr = "", backgroundColor = "", chatSenderBubbleTextColor = "", chatSenderBubbleBackgroundColor = "", chatSendButtonBackgroundColor = "", chatReceiverBubbleBackgroundColor ="",
                chatReceiverBubbleTextColor = "", chatInputContainerBackgroundColor = "", chatTimestampTextColor = "", chatInputTextColor = "", chatInputBackgroundColor = "";
        int brandingColor = getCleverPushInstance().getBrandingColor();
        if (brandingColor != 0) {
          brandingColorStr = "#" + Integer.toHexString(brandingColor).substring(2);
        }
        if (getChatBackgroundColor() != null && getChatBackgroundColor().length() > 0) {
          backgroundColor = getChatBackgroundColor();
        }
        if (getChatSenderBubbleTextColor() != null && getChatSenderBubbleTextColor().length() > 0) {
          chatSenderBubbleTextColor = getChatSenderBubbleTextColor();
        }
        if (getChatSenderBubbleBackgroundColor() != null && getChatSenderBubbleBackgroundColor().length() > 0) {
          chatSenderBubbleBackgroundColor = getChatSenderBubbleBackgroundColor();
        }
        if (getChatSendButtonBackgroundColor() != null && getChatSendButtonBackgroundColor().length() > 0) {
          chatSendButtonBackgroundColor = getChatSendButtonBackgroundColor();
        }
        if (getChatReceiverBubbleBackgroundColor() != null && getChatReceiverBubbleBackgroundColor().length() > 0) {
          chatReceiverBubbleBackgroundColor = getChatReceiverBubbleBackgroundColor();
        }
        if (getChatReceiverBubbleTextColor() != null && getChatReceiverBubbleTextColor().length() > 0) {
          chatReceiverBubbleTextColor = getChatReceiverBubbleTextColor();
        }
        if (getChatInputContainerBackgroundColor() != null && getChatInputContainerBackgroundColor().length() > 0) {
          chatInputContainerBackgroundColor = getChatInputContainerBackgroundColor();
        }
        if (getChatTimestampTextColor() != null && getChatTimestampTextColor().length() > 0) {
          chatTimestampTextColor = getChatTimestampTextColor();
        }
        if (getChatInputTextColor() != null && getChatInputTextColor().length() > 0) {
          chatInputTextColor = getChatInputTextColor();
        }
        if (getChatInputBackgroundColor() != null && getChatInputBackgroundColor().length() > 0) {
          chatInputBackgroundColor = getChatInputBackgroundColor();
        }

        String data = "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>\n" +
            "<style>\n" +
            "html, body { margin: 0; padding: 0; height: 100%; }\n" +
            "</style>\n" +
            "</head>\n" +
            "<body>\n" +
            "<div class='cleverpush-chat-target' style='height: 100%;'></div>\n" +
            "<script>\n" +
            "window.cleverpushHandleSubscribe = function() { window.cleverpushAppInterface.subscribe() };\n" +
            "var cleverpushConfig = " + configJson + ";\n" +
            "cleverpushConfig.chatStylingOptions = {};\n" +
            "(cleverpushConfig || {}).nativeApp = true;\n" +
            "(cleverpushConfig || {}).nativeAppPlatform = 'Android';\n" +
            "(cleverpushConfig || {}).brandingColor = '" + brandingColorStr + "';\n" +
            "(cleverpushConfig || {}).chatBackgroundColor = '" + backgroundColor + "';\n" +
            "(cleverpushConfig || {}).chatStylingOptions.widgetTextColor = '" + chatSenderBubbleTextColor + "';\n" +
            "(cleverpushConfig || {}).chatStylingOptions.chatButtonColor = '" + chatSendButtonBackgroundColor + "';\n" +
            "(cleverpushConfig || {}).chatStylingOptions.widgetInputBoxColor = '" + chatInputBackgroundColor + "';\n" +
            "(cleverpushConfig || {}).chatStylingOptions.widgetInputTextColor = '" + chatInputTextColor + "';\n" +
            "(cleverpushConfig || {}).chatStylingOptions.receiverBubbleColor = '" + chatReceiverBubbleBackgroundColor + "';\n" +
            "(cleverpushConfig || {}).chatStylingOptions.inputContainer = '" + chatInputContainerBackgroundColor + "';\n" +
            "(cleverpushConfig || {}).chatStylingOptions.dateColor = '" + chatTimestampTextColor + "';\n" +
            "(cleverpushConfig || {}).chatStylingOptions.receiverTextColor = '" + chatReceiverBubbleTextColor + "';\n" +
            "(cleverpushConfig || {}).chatStylingOptions.chatSenderBubbleBackgroundColor = '" + chatSenderBubbleBackgroundColor + "';\n" +
            "var cleverpushSubscriptionId = '" + subscriptionId + "';\n" +
            "</script>\n" +
            "<script>\n" +
            "      function showErrorView() {\n" +
            "        document.body.innerHTML = `\n" +
            "        <style>\n" +
            "        .cleverpush-chat-error {\n" +
            "          color: #555;\n" +
            "          text-align: center;\n" +
            "          font-family: sans-serif;\n" +
            "          padding: center;\n" +
            "          height: 100%;\n" +
            "          display: flex;\n" +
            "          align-items: center;\n" +
            "          justify-content: center;\n" +
            "          flex-direction: column;\n" +
            "        }\n" +
            "        .cleverpush-chat-error h1 {\n" +
            "          font-size: 24px;\n" +
            "          font-weight: normal;\n" +
            "          margin-bottom: 25px;\n" +
            "        }\n" +
            "        .cleverpush-chat-error button {\n" +
            "          background-color: #555;\n" +
            "          color: #fff;\n" +
            "          border: none;\n" +
            "          font-weight: bold;\n" +
            "          display: block;\n" +
            "          font-size: 16px;\n" +
            "          border-radius: 200px;\n" +
            "          padding: 7.5px 15px;\n" +
            "          cursor: pointer;\n" +
            "          font-family: sans-serif;\n" +
            "        }\n" +
            "        </style>\n" +
            "        <div class='cleverpush-chat-error'>\n" +
            "        <h1>Laden fehlgeschlagen</h1>\n" +
            "        <button onclick='window.cleverpushAppInterface.reload()' type='button'>Erneut versuchen</button>\n"
            +
            "        </div>`;\n" +
            "      }\n" +
            "      if (!cleverpushConfig) { showErrorView() }\n" +
            "    </script>\n" +
            "<script onerror='showErrorView()' src='https://static.cleverpush.com/sdk/cleverpush-chat.js'></script>\n" +
            "</body>\n" +
            "</html>";
        getHandler().post(
            () -> webView.loadDataWithBaseURL("file:///android_asset/", data, "text/html", "UTF-8", null));
      });
    }).start();
  }

  void init() {
    this.handler = new Handler();
    Context context = this.getContext();

    WebSettings webSettings = this.getSettings();
    if (webSettings != null) {
      webSettings.setJavaScriptEnabled(true);
      webSettings.setUseWideViewPort(true);
      webSettings.setLoadWithOverviewMode(true);
      webSettings.setDomStorageEnabled(true);
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      this.setWebContentsDebuggingEnabled(true);
    }

    this.addJavascriptInterface(getChatJavascriptInterface(context), "cleverpushAppInterface");

    this.loadUrl("about:blank");

    if (getCleverPushInstance() != null) {
      this.setWebViewClient(getWebViewClient(context));
      this.loadChat();
    }
  }

  public WebViewClient getWebViewClient(Context context) {
    return new WebViewClient() {
      public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        Toast.makeText(context, description, Toast.LENGTH_SHORT).show();
      }

      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url.equals("about:blank")) {
          return super.shouldOverrideUrlLoading(view, url);
        }
        ChatUrlOpenedListener urlOpenedListener = getCleverPushInstance().getChatUrlOpenedListener();
        if (urlOpenedListener != null) {
          urlOpenedListener.opened(url);
        }
        return true;
      }
    };
  }

  public String getLastSubscriptionId() {
    return lastSubscriptionId;
  }

  public WebView getWebView() {
    return this;
  }

  public CleverPush getCleverPushInstance() {
    if (getContext() != null) {
      return CleverPush.getInstance(getWebView().getContext());
    }
    return null;
  }

  public ChatJavascriptInterface getChatJavascriptInterface(Context context) {
    return new ChatJavascriptInterface(context, this);
  }

  public int getBuildVersion() {
    return Build.VERSION.SDK_INT;
  }

  public Handler getHandler() {
    return handler;
  }
}
