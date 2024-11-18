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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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
        String brandingColorStr = "", backgroundColor = "", chatSenderBubbleTextColor = "", chatSenderBubbleBackgroundColor = "", chatSendButtonBackgroundColor = "", chatReceiverBubbleBackgroundColor = "",
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

        String htmlTemplate = loadHtmlTemplate("chat_view_template.html");
        if (htmlTemplate == null) return;

        String data = htmlTemplate
            .replace("{{configJson}}", configJson)
            .replace("{{brandingColorStr}}", brandingColorStr)
            .replace("{{backgroundColor}}", backgroundColor)
            .replace("{{chatSenderBubbleTextColor}}", chatSenderBubbleTextColor)
            .replace("{{chatSenderBubbleBackgroundColor}}", chatSenderBubbleBackgroundColor)
            .replace("{{chatSendButtonBackgroundColor}}", chatSendButtonBackgroundColor)
            .replace("{{chatReceiverBubbleBackgroundColor}}", chatReceiverBubbleBackgroundColor)
            .replace("{{chatReceiverBubbleTextColor}}", chatReceiverBubbleTextColor)
            .replace("{{chatInputContainerBackgroundColor}}", chatInputContainerBackgroundColor)
            .replace("{{chatTimestampTextColor}}", chatTimestampTextColor)
            .replace("{{chatInputTextColor}}", chatInputTextColor)
            .replace("{{chatInputBackgroundColor}}", chatInputBackgroundColor)
            .replace("{{subscriptionId}}", subscriptionId);

        getHandler().post(
            () -> webView.loadDataWithBaseURL("file:///android_asset/", data, "text/html", "UTF-8", null));
      });
    }).start();
  }

  private String loadHtmlTemplate(String filename) {
    try (InputStream is = getContext().getAssets().open(filename)) {
      int size = is.available();
      byte[] buffer = new byte[size];
      is.read(buffer);
      return new String(buffer, StandardCharsets.UTF_8);
    } catch (Exception e) {
      Logger.e(LOG_TAG, "Error reading ChatView HTML template: " + e.getMessage());
      return null;
    }
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
