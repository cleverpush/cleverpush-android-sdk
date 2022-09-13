package com.cleverpush;

import static com.cleverpush.Constants.LOG_TAG;
import static com.google.firebase.messaging.Constants.TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceStatusCodes
                    .getStatusCodeString(geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
        String channelId = sharedPreferences.getString(CleverPushPreferences.CHANNEL_ID, null);
        String subscriptionId = sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null);
        String transitionState = geofencingEvent.getGeofenceTransition() == Geofence.GEOFENCE_TRANSITION_ENTER ? "enter" : "exit";

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER || geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            // multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            if (channelId != null && subscriptionId != null) {
                for (int i = 0; i < triggeringGeofences.size(); i++) {
                    JSONObject jsonBody = new JSONObject();
                    try {
                        jsonBody.put("geoFenceId", triggeringGeofences.get(i).getRequestId());
                        jsonBody.put("channelId", channelId);
                        jsonBody.put("subscriptionId", subscriptionId);
                        jsonBody.put("state", transitionState);
                        CleverPushHttpClient.post("/subscription/geo-fence", jsonBody, null);
                    } catch (JSONException e) {
                        Log.e(LOG_TAG, "Error generating geo-fence json", e);
                    }
                }
            }

        } else {
            Log.e(LOG_TAG, "Error generating geo-fence json");
        }
    }
}