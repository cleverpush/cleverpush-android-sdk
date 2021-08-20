package com.cleverpush.responsehandlers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.listener.AddTagCompletedListener;

import org.json.JSONObject;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SetSubscriptionAttributeResponseHandler {

    public CleverPushHttpClient.ResponseHandler getResponseHandler(Map<String, Object> subscriptionAttributes) {
           return new CleverPushHttpClient.ResponseHandler() {
               @Override
               public void onSuccess(String response) {
                   try {
                       SharedPreferences sharedPreferences = getSharedPreferences(getContext());
                       if (sharedPreferences != null) {
                           JSONObject jsonObject = new JSONObject(subscriptionAttributes);
                           String jsonString = jsonObject.toString();
                           SharedPreferences.Editor editor = sharedPreferences.edit();
                           editor.remove(CleverPushPreferences.SUBSCRIPTION_ATTRIBUTES);
                           editor.apply();
                           editor.putString(CleverPushPreferences.SUBSCRIPTION_ATTRIBUTES, jsonString);
                           editor.commit();
                       }
                   } catch (Exception ex) {
                       Log.e("CleverPush", ex.getMessage(), ex);
                   }
               }

               @Override
               public void onFailure(int statusCode, String response, Throwable throwable) {
                   Log.e("CleverPush", "Error setting attribute - HTTP " + statusCode);
               }
           };
    }

    public SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public Context getContext() {
        return CleverPush.context;
    }

}
