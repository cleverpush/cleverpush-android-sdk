package com.cleverpush;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cleverpush.util.Logger;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    private double delay;
    private ArrayList<Double> mDelayList = new ArrayList();
    private int transition;
    private String channelId;
    private String subscriptionId;
    private String transitionState;
    int position = 0;
    boolean mCompleted = false;
    CountDownTimer cTimer;

    public void onReceive(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.getErrorCode());
            Log.e("CPErrorMessage", errorMessage);
            return;
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        channelId = sharedPreferences.getString(CleverPushPreferences.CHANNEL_ID, null);
        subscriptionId = sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null);
        transitionState = geofencingEvent.getGeofenceTransition() == Geofence.GEOFENCE_TRANSITION_ENTER ? "enter" : "exit";
        transition = geofencingEvent.getGeofenceTransition();

        CleverPush.getChannelConfig(channelConfig -> {
            if (channelConfig != null) {
                try {
                    JSONArray geoFenceArray = channelConfig.getJSONArray("geoFences");
                    if (geoFenceArray != null) {
                        for (int i = 0; i < geoFenceArray.length(); i++) {
                            JSONObject geoFence = geoFenceArray.getJSONObject(i);
                            if (geoFence != null) {
                                delay = geoFence.getDouble("delay");
                                mDelayList.add(delay);
                            }
                        }
                    }
                } catch (Exception ex) {
                    Logger.d("Exception", ex.getMessage());
                }
            }
        });
        countDownCalling(geofencingEvent);
    }


    void countDownCalling(GeofencingEvent geofencingEvent) {
        if (position != mDelayList.size()) {
            cTimer = new CountDownTimer((long) (mDelayList.get(position) * 1000), 1000) {
                @Override
                public void onTick(long l) {
                    Logger.e("CPDelay", mDelayList.get(position).intValue() + " - " + l);
                }

                @Override
                public void onFinish() {
                    if (transition == Geofence.GEOFENCE_TRANSITION_ENTER || transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                        List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
                        if (channelId != null && subscriptionId != null) {
                            JSONObject jsonBody = new JSONObject();
                            try {
                                jsonBody.put("geoFenceId", triggeringGeofences.get(position).getRequestId());
                                jsonBody.put("channelId", channelId);
                                jsonBody.put("subscriptionId", subscriptionId);
                                jsonBody.put("state", transitionState);

                            } catch (JSONException e) {
                                Logger.e("CPJSONException", "Error generating geo-fence json", e);
                            }
                            mCompleted = true;
                            CleverPushHttpClient.post("/subscription/geo-fence", jsonBody, new CleverPushHttpClient.ResponseHandler() {
                                @Override
                                public void onSuccess(String response) {
                                }

                                @Override
                                public void onFailure(int statusCode, String response, Throwable throwable) {
                                }
                            });
                        }
                    } else {
                        Log.e("CPJSONInvalid", "Invalid type");
                    }

                    if (mCompleted) {
                        mCompleted = false;
                        cTimer.cancel();
                        position += 1;
                        countDownCalling(geofencingEvent);
                    }

                }
            }.start();
        }
    }

}