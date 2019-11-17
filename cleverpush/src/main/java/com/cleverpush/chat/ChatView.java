package com.cleverpush.chat;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.cleverpush.CleverPush;

public class ChatView extends WebView {
    public ChatView(Context context) {
        super(context);

        this.init();
    }

    public ChatView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.init();
    }

    void init() {

        System.out.println("CLEVERPUSH init ");
        this.getSettings().setJavaScriptEnabled(true) ;
        this.getSettings().setUseWideViewPort(true);
        this.getSettings().setLoadWithOverviewMode(true);
        this.getSettings().setDomStorageEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            this.setWebContentsDebuggingEnabled(true);
        }
        this.loadUrl("about:blank");

        Context context = this.getContext();
        WebView webView = this;

        Handler handler = new Handler();

        new Thread(() -> {
            String subscriptionId = CleverPush.getInstance(context).getSubscriptionId();
            String configJson = CleverPush.getInstance(context).getChannelConfig().toString();

            String data = "<!DOCTYPE html>\n" +
                    "            <html>\n" +
                    "            <head>\n" +
                    "            <meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>\n" +
                    "            <style>\n" +
                    "            html, body { margin: 0; padding: 0; height: 100%; }\n" +
                    "            </style>\n" +
                    "            </head>\n" +
                    "            <body>\n" +
                    "            <div class='cleverpush-chat-target' style='height: 100%;'></div>\n" +
                    "            <script>var cleverpushConfig = " + configJson +"; var cleverpushSubscriptionId = '" + subscriptionId + "';</script>\n" +
                    "            <script src='https://static.cleverpush.com/sdk/cleverpush-chat.js'></script>\n" +
                    "            </body>\n" +
                    "            </html>";

            System.out.println("CLEVERPUSH DATA " + data);

            handler.post(() -> webView.loadDataWithBaseURL("file:///android_asset/", data, "text/html", "UTF-8", null));
        }).start();

        this.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(context, description, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
