package com.cleverpush.service;

import static com.cleverpush.Constants.LOG_TAG;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CleverPushGeofenceTransitionsIntentService extends IntentService {
    protected static final String TAG = "CPGeofenceTransitionsIS";

    public CleverPushGeofenceTransitionsIntentService() {
        super(TAG);  // use TAG to name the IntentService worker thread
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
        if (event.hasError()) {
            Log.e(TAG, "GeofencingEvent Error: " + event.getErrorCode());
            return;
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String channelId = sharedPreferences.getString(CleverPushPreferences.CHANNEL_ID, null);
        String subscriptionId = sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null);
        String transitionState = event.getGeofenceTransition() == Geofence.GEOFENCE_TRANSITION_ENTER ? "enter" : "exit";
        int transition = event.getGeofenceTransition();
        try {
            if (channelId != null && subscriptionId != null) {
                List<String> geofenceIds = new ArrayList<>();
                for (Geofence geofence : event.getTriggeringGeofences()) {
                    geofenceIds.add(geofence.getRequestId());
                    Log.e("GeoFence",""+geofence.getRequestId());
                    JSONObject jsonBody = new JSONObject();
                    try {
                        jsonBody.put("geoFenceId", geofence.getRequestId());
                        jsonBody.put("channelId", channelId);
                        jsonBody.put("subscriptionId", subscriptionId);
                        jsonBody.put("state", transitionState);

                        CleverPushHttpClient.post("/subscription/geo-fence", jsonBody, null);
                    } catch (JSONException e) {
                        Log.e(LOG_TAG, "Error generating geo-fence json", e);
                    }
                }
                if (transition == Geofence.GEOFENCE_TRANSITION_ENTER || transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                    onEnteredGeofences(geofenceIds);
                }
            }
        } catch (Exception e) {
            Log.e("ex", "Error generating geo-fence json", e);
        }

/*
        new CountDownTimer(500000, 1000) {
            public void onTick(long millisUntilFinished) {
                Log.e("onTick", "");
            }

            public void onFinish() {

            }

        }.start();
*/

    }

    private void onEnteredGeofences(List<String> geofenceIds) {
        Log.e("geofenceIds", "" + geofenceIds.toString());
    }


    private static String getGeofenceTransitionDetails(GeofencingEvent event) {
        String transitionString = GeofenceStatusCodes.getStatusCodeString(event.getGeofenceTransition());
        List<String> triggeringIDs = new ArrayList<>();
        try {
            for (Geofence geofence : event.getTriggeringGeofences()) {
                triggeringIDs.add(geofence.getRequestId());
            }

        } catch (Exception e) {
            Log.e("Ex", e.getMessage());
        }
        return String.format("%s: %s", transitionString, TextUtils.join(", ", triggeringIDs));
    }



    protected void onExitedGeofences(String[] geofenceIds) {

    }

    protected void onError(int errorCode) {

    }
}
