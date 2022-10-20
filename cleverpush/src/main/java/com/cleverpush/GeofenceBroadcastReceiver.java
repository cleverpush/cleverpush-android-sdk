package com.cleverpush;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.cleverpush.util.Logger;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    // ...
    public void onReceive(Context context, Intent intent) {
        Logger.e("CPintentNew", "" + intent.getAction() + " " + intent.getIdentifier() + intent.getPackage() + intent.getExtras());
//        Logger.e("CPgetExtrans", intent.getExtras().toString());


        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
//        Logger.e("CPgetExtrans", geofencingEvent.getTriggeringLocation().toString());

        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceStatusCodes
                    .getStatusCodeString(geofencingEvent.getErrorCode());
            Log.e("CPTAG", errorMessage);
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        Logger.e("CPtriggeringGeofences", String.valueOf(geofenceTransition));

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER || geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            // Get the geofences that were triggered. A single event can trigger
            // multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            Logger.e("CPtriggeringGeofencs", triggeringGeofences.get(0).getRequestId());
            // Get the transition details as a String.
        } else {
            // Log the error.
            Log.e("CPiTAGINVALID", "Invalid type");
        }
    }
}