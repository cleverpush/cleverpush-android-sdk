package com.cleverpush;

import android.content.Context;
import android.content.Intent;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class NotificationOpenedProcessor {

    public static void processIntent(Context context, Intent intent) {
        Map data = null;
        try {
            data = jsonToMap(intent.getStringExtra("data"));
        } catch (Throwable t) {
            t.printStackTrace();
        }

        NotificationOpenedResult result = new NotificationOpenedResult();
        result.setData(data);

        if (data != null) {
            String notificationId = (String) data.get("notificationId");
            String subscriptionId = (String) data.get("subscriptionId");

            if (notificationId != null && subscriptionId != null) {
                JSONObject jsonBody = new JSONObject();
                try {
                    jsonBody.put("notificationId", notificationId);
                    jsonBody.put("subscriptionId", subscriptionId);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                CleverPushHttpClient.post("notification/clicked", jsonBody, null);
            }
        }

        CleverPush.getInstance(context).fireNotificationOpenedListener(result);
    }

    public static Map<String, Object> jsonToMap(String jsonString ) throws JSONException {
        Map<String, Object> keys = new HashMap<>();
        org.json.JSONObject jsonObject = new JSONObject( jsonString );
        Iterator<?> keyset = jsonObject.keys();
        while (keyset.hasNext()) {
            String key =  (String) keyset.next();
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject) {
                keys.put(key, jsonToMap(value.toString()));
            } else {
                keys.put(key, value);
            }
        }
        return keys;
    }
}
