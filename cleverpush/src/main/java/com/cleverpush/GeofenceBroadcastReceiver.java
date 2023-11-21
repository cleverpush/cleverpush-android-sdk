package com.cleverpush;

import static com.cleverpush.Constants.COUNTDOWN_TIMER;
import static com.cleverpush.Constants.COUNTDOWN_TIMER_INTERVAL;
import static com.cleverpush.Constants.GEOFENCE_ENTER_STATE;
import static com.cleverpush.Constants.GEOFENCE_EXIT_STATE;
import static com.cleverpush.Constants.LOG_TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;

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
  private final ArrayList<Double> geoFenceDelayArray = new ArrayList();
  private int transition;
  private String channelId;
  private String subscriptionId;
  private String transitionState;
  int geoFenceIndex;
  boolean geoFenceTimeoutCompleted = false;
  CountDownTimer countDownTimer;

  public void onReceive(Context context, Intent intent) {
    GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
    if (geofencingEvent.hasError()) {
      String errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.getErrorCode());
      return;
    }

    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    channelId = sharedPreferences.getString(CleverPushPreferences.CHANNEL_ID, null);
    subscriptionId = sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null);
    transitionState =
        geofencingEvent.getGeofenceTransition() == Geofence.GEOFENCE_TRANSITION_ENTER ? GEOFENCE_ENTER_STATE :
            GEOFENCE_EXIT_STATE;
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
                this.geoFenceDelayArray.add(delay);
              }
            }
          }
        } catch (Exception ex) {
          Logger.e("Exception", ex.getMessage());
        }
      }
    });
    geoFenceHandleTimer(geofencingEvent);
  }

  void geoFenceHandleTimer(GeofencingEvent geofencingEvent) {
    if (geoFenceIndex != geoFenceDelayArray.size()) {
      countDownTimer = new CountDownTimer((long) (geoFenceDelayArray.get(geoFenceIndex) * COUNTDOWN_TIMER),
          COUNTDOWN_TIMER_INTERVAL) {
        @Override
        public void onTick(long l) {
        }

        @Override
        public void onFinish() {
          if (transition == Geofence.GEOFENCE_TRANSITION_ENTER || transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            List<Geofence> triggeringGeofence = geofencingEvent.getTriggeringGeofences();
            if (channelId != null && subscriptionId != null && geoFenceIndex < triggeringGeofence.size()) {
              JSONObject jsonBody = new JSONObject();
              try {
                jsonBody.put("geoFenceId", triggeringGeofence.get(geoFenceIndex).getRequestId());
                jsonBody.put("channelId", channelId);
                jsonBody.put("subscriptionId", subscriptionId);
                jsonBody.put("state", transitionState);
              } catch (JSONException e) {
                Logger.e(LOG_TAG, "Error generating geo-fence json", e);
              }
              geoFenceTimeoutCompleted = true;
              CleverPushHttpClient.postWithRetry("/subscription/geo-fence", jsonBody,
                  new CleverPushHttpClient.ResponseHandler() {
                    @Override
                    public void onSuccess(String response) {
                      Logger.d("onSuccess", response);
                    }

                    @Override
                    public void onFailure(int statusCode, String response, Throwable throwable) {
                      Logger.e("onFailure", response);
                      if (throwable != null) {
                        Logger.e("CleverPush", "Failed while geo-fence request." +
                                "\nStatus code: " + statusCode +
                                "\nResponse: " + response +
                                "\nError: " + throwable.getMessage()
                        );
                      } else {
                        Logger.e("CleverPush", "Failed while geo-fence request." +
                                "\nStatus code: " + statusCode +
                                "\nResponse: " + response
                        );
                      }
                    }
                  });
            }
          } else {
            Logger.e("CPJSONInvalid", "Invalid type");
          }

          if (geoFenceTimeoutCompleted) {
            geoFenceTimeoutCompleted = false;
            countDownTimer.cancel();
            geoFenceIndex += 1;
            geoFenceHandleTimer(geofencingEvent);
          }
        }
      }.start();
    }
  }
}
