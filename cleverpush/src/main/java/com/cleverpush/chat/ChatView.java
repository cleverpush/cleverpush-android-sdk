package com.cleverpush.chat;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.cleverpush.CleverPush;
import com.cleverpush.listener.ChatUrlOpenedListener;

public class ChatView extends WebView {
    private String lastSubscriptionId;
    private Handler handler;

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
        lastSubscriptionId = subscriptionId;

        Context context = this.getContext();
        WebView webView = this;

        if (this.handler == null) {
            this.handler = new Handler();
        }

        new Thread(() -> {
            CleverPush.getInstance(context).getChannelConfig(config -> {
                String configJson = config != null ? config.toString() : "null";
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
                        "var cleverpushConfig = " + configJson + ";\n" +
                        "(cleverpushConfig || {}).nativeApp = true;\n" +
                        "(cleverpushConfig || {}).nativeAppPlatform = 'Android';\n" +
                        "(cleverpushConfig || {}).brandingColor = '" + brandingColorStr + "';\n" +
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
                        "        <button onclick='window.cleverpushAppInterface.reload()' type='button'>Erneut versuchen</button>\n" +
                        "        </div>`;\n" +
                        "      }\n" +
                        "      if (!cleverpushConfig) { showErrorView() }\n" +
                        "    </script>\n" +
                        "<script onerror='showErrorView()' src='https://static.cleverpush.com/sdk/cleverpush-chat.js'></script>\n" +
                        "</body>\n" +
                        "</html>";

                this.handler.post(() -> webView.loadDataWithBaseURL("file:///android_asset/", data, "text/html", "UTF-8", null));
            });
        }).start();
    }

    void init() {
        //this.handler = getNewHandler();
        Context context = this.getContext();

        this.getSettings().setJavaScriptEnabled(true) ;
        this.getSettings().setUseWideViewPort(true);
        this.getSettings().setLoadWithOverviewMode(true);
        this.getSettings().setDomStorageEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            this.setWebContentsDebuggingEnabled(true);
        }

        this.addJavascriptInterface(new ChatJavascriptInterface(context, this), "cleverpushAppInterface");

        this.loadUrl("about:blank");

        this.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(context, description, Toast.LENGTH_SHORT).show();
            }

            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.equals("about:blank")) {
                    return super.shouldOverrideUrlLoading(view, url);
                }
                ChatUrlOpenedListener urlOpenedListener = CleverPush.getInstance(context).getChatUrlOpenedListener();
                if (urlOpenedListener != null) {
                    urlOpenedListener.opened(url);
                }
                return true;
            }
        });

        this.loadChat();
    }

    public String getLastSubscriptionId() {
        return lastSubscriptionId;
    }

    public Handler getNewHandler() {
        return new Handler();
    }
}
