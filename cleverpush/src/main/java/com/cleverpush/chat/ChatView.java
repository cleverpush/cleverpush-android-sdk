package com.cleverpush.chat;

import android.content.Context;
import android.webkit.WebView;

import com.cleverpush.CleverPush;

public class ChatView extends WebView {
    public ChatView(Context context) {
        super(context);

        String subscriptionId = CleverPush.getInstance(context).getSubscriptionId();
        String configJson = CleverPush.getInstance(context).getChannelConfig().toString();

        this.loadData("<!DOCTYPE html>\n" +
                "            <html>\n" +
                "            <head>\n" +
                "            <meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>\n" +
                "            <style>\n" +
                "            html, body { margin: 0; padding: 0; height: 100%%; }\n" +
                "            </style>\n" +
                "            </head>\n" +
                "            <body>\n" +
                "            <div class='cleverpush-chat-target' style='height: 100%%;'></div>\n" +
                "            <script>var cleverpushConfig = " + configJson +"; var cleverpushSubscriptionId = '" + subscriptionId + "';</script>\n" +
                "            <script src='https://static.cleverpush.com/sdk/cleverpush-chat.js'></script>\n" +
                "            </body>\n" +
                "            </html>", "text/html", "UTF-8");
    }
}
