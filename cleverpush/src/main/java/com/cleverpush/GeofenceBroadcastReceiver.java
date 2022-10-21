package com.cleverpush;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cleverpush.util.Logger;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    private double delay;
    private Handler mHandler = new Handler();
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.getErrorCode());
            Log.e("CPTAG", errorMessage);
            return;
        }

        CleverPush.getChannelConfig(channelConfig -> {
            if (channelConfig != null) {
                try {
                    JSONArray geoFenceArray = channelConfig.getJSONArray("geoFences");
                    if (geoFenceArray != null) {
                        for (int i = 0; i < geoFenceArray.length(); i++) {
                            JSONObject geoFence = geoFenceArray.getJSONObject(i);
                            if (geoFence != null) {
                                delay = geoFence.getDouble("delay");
                            }
                        }
                    }
                } catch (Exception ex) {
                    Logger.d("CPLOG_TAG", ex.getMessage());
                }
            }
        });

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String channelId = sharedPreferences.getString(CleverPushPreferences.CHANNEL_ID, null);
        String subscriptionId = sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null);
        String transitionState = geofencingEvent.getGeofenceTransition() == Geofence.GEOFENCE_TRANSITION_ENTER ? "enter" : "exit";

        int transition = geofencingEvent.getGeofenceTransition();
        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER || transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            if (channelId != null && subscriptionId != null) {
                for (int i = 0; i < triggeringGeofences.size(); i++) {
                    JSONObject jsonBody = new JSONObject();
                    try {
                        jsonBody.put("geoFenceId", triggeringGeofences.get(i).getRequestId());
                        jsonBody.put("channelId", channelId);
                        jsonBody.put("subscriptionId", subscriptionId);
                        jsonBody.put("state", transitionState);

                        CleverPushHttpClient.post("/subscription/geo-fence", jsonBody, new CleverPushHttpClient.ResponseHandler() {
                            @Override
                            public void onSuccess(String response) {
                                Logger.e("CPintentNe", "" + response);
                            }

                            @Override
                            public void onFailure(int statusCode, String response, Throwable throwable) {

                            }
                        });
                    } catch (JSONException e) {
                        Logger.e("CPLOG", "Error generating geo-fence json", e);
                    }
                }
            }
        } else {
            Log.e("CPiTAGINVALID", "Invalid type");
        }
    }
}