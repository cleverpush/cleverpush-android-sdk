package com.cleverpush.service;

import static com.cleverpush.Constants.LOG_TAG;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import com.cleverpush.util.Logger;

import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.util.Logger;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

public class CleverPushGeofenceTransitionsIntentService extends IntentService {
    protected static final String TAG = "CPGeofenceTransitionsIS";

    public CleverPushGeofenceTransitionsIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
//        Geofence geofence = event.getTriggeringGeofences().get(0);
        Log.e("geofence", "GeofencingEvent : " + event.getTriggeringGeofences());
        if (event.hasError()) {
            Log.e(TAG, "GeofencingEvent Error: " + event.getErrorCode());
            return;
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String channelId = sharedPreferences.getString(CleverPushPreferences.CHANNEL_ID, null);
        String subscriptionId = sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null);
        String transitionState = event.getGeofenceTransition() == Geofence.GEOFENCE_TRANSITION_ENTER ? "enter" : "exit";
		Logger.d(TAG, "Geofence Transition Details: " + getGeofenceTransitionDetails(event) + " " + transitionState + " subscription: " + subscriptionId + " channel " + channelId);

		if (channelId != null && subscriptionId != null) {
			for (Geofence geofence : event.getTriggeringGeofences()) {
				JSONObject jsonBody = new JSONObject();
				try {
					jsonBody.put("geoFenceId", geofence.getRequestId());
					jsonBody.put("channelId", channelId);
					jsonBody.put("subscriptionId", subscriptionId);
					jsonBody.put("state", transitionState);

					CleverPushHttpClient.post("/subscription/geo-fence", jsonBody, null);
				} catch (JSONException e) {
					Logger.e(LOG_TAG, "Error generating geo-fence json", e);
				}
			}
		}
	}

	private static String getGeofenceTransitionDetails(GeofencingEvent event) {
		String transitionString = GeofenceStatusCodes.getStatusCodeString(event.getGeofenceTransition());
		List<String> triggeringIDs = new ArrayList<>();
		for (Geofence geofence : event.getTriggeringGeofences()) {
			triggeringIDs.add(geofence.getRequestId());
		}
		return String.format("%s: %s", transitionString, TextUtils.join(", ", triggeringIDs));
	}
}
