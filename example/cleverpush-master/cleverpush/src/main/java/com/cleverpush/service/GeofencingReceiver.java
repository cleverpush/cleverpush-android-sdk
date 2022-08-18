package com.cleverpush.service;

import android.util.Log;

public class GeofencingReceiver extends CleverPushGeofenceTransitionsIntentService {


    @Override
    protected void onExitedGeofences(String[] geofenceIds) {
        Log.d(GeofencingReceiver.class.getName(), "onExit");
    }

    @Override
    protected void onError(int errorCode) {
        Log.e(GeofencingReceiver.class.getName(), "Error: " + errorCode);
    }
}