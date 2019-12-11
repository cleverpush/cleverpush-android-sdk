package com.cleverpush.chat;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.cleverpush.CleverPush;
import com.cleverpush.listener.ChatSubscribeListener;
import com.cleverpush.listener.ChatUrlOpenedListener;

public class ChatView extends WebView {
    public ChatView(Context context) {
        super(context);

        this.init();
    }

    public ChatView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.init();
    }

    public void loadChat() {
        Context context = this.getContext();
        CleverPush.getInstance(context).getSubscriptionId(subscriptionId -> {
            this.loadChat(subscriptionId);
        });
    }

    public void lockChat() {
        loadChat("preview");
    }

    public void loadChat(String subscriptionId) {
        Context context = this.getContext();
        WebView webView = this;

        Handler handler = new Handler();

        new Thread(() -> {
            CleverPush.getInstance(context).getChannelConfig(config -> {
                String configJson = config.toString();
                String brandingColorStr = "";
                int brandingColor = CleverPush.getInstance(context).getBrandingColor();
                if (brandingColor != 0) {
                    brandingColorStr = "#" + Integer.toHexString(brandingColor).substring(2);
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
                        "var cleverpushConfig = " + configJson +";\n" +
                        "cleverpushConfig.nativeApp = true;\n" +
                        "cleverpushConfig.nativeAppPlatform = 'Android';\n" +
                        "cleverpushConfig.brandingColor = '" + brandingColorStr + "';\n" +
                        "var cleverpushSubscriptionId = '" + subscriptionId + "';\n" +
                        "</script>\n" +
                        "<script src='https://static.cleverpush.com/sdk/cleverpush-chat.js'></script>\n" +
                        "</body>\n" +
                        "</html>";

                handler.post(() -> webView.loadDataWithBaseURL("file:///android_asset/", data, "text/html", "UTF-8", null));
            });
        }).start();
    }

    void init() {
        this.getSettings().setJavaScriptEnabled(true) ;
        this.getSettings().setUseWideViewPort(true);
        this.getSettings().setLoadWithOverviewMode(true);
        this.getSettings().setDomStorageEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            this.setWebContentsDebuggingEnabled(true);
        }
        this.loadUrl("about:blank");

        Context context = this.getContext();

        ChatView chatView = this;

        class JsInterface {
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
                });
            }
        }

        this.loadChat();

        this.addJavascriptInterface(new JsInterface(), "cleverpushAppInterface");

        this.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(context, description, Toast.LENGTH_SHORT).show();
            }

            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.equals("about:blank")) {
                    return false;
                }
                ChatUrlOpenedListener urlOpenedListener = CleverPush.getInstance(context).getChatUrlOpenedListener();
                if (urlOpenedListener != null) {
                    urlOpenedListener.opened(url);
                }
                return true;
            }
        });
    }

}