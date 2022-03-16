package com.cleverpush.service;

import static com.cleverpush.Constants.LOG_TAG;

import android.util.Log;
import android.webkit.WebView;

import com.cleverpush.ActivityLifecycleListener;
import com.cleverpush.ChannelTag;
import com.cleverpush.CleverPush;
import com.cleverpush.listener.TagsMatcherListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TagsMatcher {
    public static void autoAssignTagMatches(ChannelTag tag, String pathname, Map<String, ?> params, TagsMatcherListener callback) {
        if (tag.getAutoAssignPath() != null) {
            String path = tag.getAutoAssignPath();
            if (path.equals("[EMPTY]")) {
                path = "";
            }
            Pattern p = Pattern.compile(path);
            Matcher m = p.matcher(pathname);
            if (m.find()) {
                callback.tagMatches(true);
                return;
            }
        }

        if (tag.getAutoAssignFunction() != null && params != null) {
            try {
                (ActivityLifecycleListener.currentActivity).runOnUiThread(() -> {
                    WebView webview = new WebView(CleverPush.context);
                    webview.getSettings().setJavaScriptEnabled(true);
                    Gson gson = new Gson();
                    Type gsonType = new TypeToken<Map<String, ?>>(){}.getType();
                    String paramsString = gson.toJson(params, gsonType);
                    String function = "(function(params) { return (" + tag.getAutoAssignFunction() + ") ? 'true' : 'false'; })(" + paramsString + ");";
                    Log.d(LOG_TAG, "autoAssignTag function: " + function);
                    webview.evaluateJavascript(function, result -> {
                        Log.d(LOG_TAG, "autoAssignTag function result: " + result);
                        if (result.equals("true") || result.equals("\"true\"")) {
                            callback.tagMatches(true);
                        } else {
                            callback.tagMatches(false);
                        }
                    });
                });
            } catch (Exception ex) {
                Log.d(LOG_TAG, "Exception", ex);
                callback.tagMatches(false);
            }
            return;
        }

        if (tag.getAutoAssignSelector() != null) {
            // not implemented
            callback.tagMatches(false);
            return;
        }

        callback.tagMatches(false);
    }
}
