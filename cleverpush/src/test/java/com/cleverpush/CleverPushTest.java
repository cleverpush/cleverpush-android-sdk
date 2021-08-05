package com.cleverpush;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.cleverpush.banner.AppBannerModule;
import com.cleverpush.listener.NotificationOpenedListener;
import com.cleverpush.listener.SessionListener;
import com.google.android.gms.common.api.GoogleApiClient;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class CleverPushTest {

    @Mock
    Context context;

    @Mock
    SharedPreferences sharedPreferences;

    @Mock
    Resources resources;

    @Mock
    SharedPreferences.Editor editor;

    @Mock
    NotificationOpenedResult notificationOpenedResult;

    @Mock
    Notification notification;

    @Mock
    NotificationOpenedListener notificationOpenedListener;

    @Mock
    ActivityLifecycleListener activityLifecycleListener;

    @Mock
    SessionListener sessionListener;

    @Mock
    GoogleApiClient googleApiClient;

    @Mock
    AppBannerModule appBannerModule;

    @Mock
    ActivityCompat activityCompat;

    @Mock
    Activity activity;

    @Mock
    ContextCompat contextCompat;

    CleverPush cleverPush;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        cleverPush = Mockito.spy(new CleverPush(context));
        activityLifecycleListener = Mockito.spy(new ActivityLifecycleListener(sessionListener));
    }

    @Test
    void testGetInstance() {
        Throwable e = null;
        try {
            CleverPush.getInstance(null);
        } catch (Throwable ex) {
            e = ex;
        }
        assertThat(e instanceof NullPointerException).isTrue();
    }

    @Test
    void testGetChannelIdFromSharedPreferencesWhenItIsNull() {
        doReturn(null).when(cleverPush).getChannelId(context);
        doReturn(context).when(cleverPush).getContext();
        doNothing().when(cleverPush).incrementAppOpens();
        when(context.getResources()).thenReturn(resources);
        cleverPush.init(null, null, null, null, true);
        verify(cleverPush).getChannelId(context);
    }

    @Test
    void testGetChannelConfigFromBundleIdWhenChannelIdIsNull() {
        doReturn(null).when(cleverPush).getChannelId(context);
        doReturn(context).when(cleverPush).getContext();
        when(context.getResources()).thenReturn(resources);
        when(context.getPackageName()).thenReturn("com.test");
        doNothing().when(cleverPush).incrementAppOpens();
        cleverPush.init(null, null, null, null, true);
        verify(cleverPush).getChannelConfigFromBundleId("/channel-config?bundleId=com.test&platformName=Android", true);
    }

    @Test
    void testGetChannelConfigFromChannelId() {
        doReturn("channelId").when(cleverPush).getChannelId(context);
        doReturn("subscriptionId").when(cleverPush).getSubscriptionId(context);
        doReturn(context).when(cleverPush).getContext();
        when(context.getResources()).thenReturn(resources);
        doNothing().when(cleverPush).addOrUpdateChannelId(context, "channelId");
        doNothing().when(cleverPush).incrementAppOpens();
        cleverPush.init("channelId", null, null, null, true);
        verify(cleverPush).getChannelConfigFromChannelId(true, "channelId", "subscriptionId");
    }

    @Test
    void testIsSubscribed() {
        doReturn(context).when(cleverPush).getContext();
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        when(sharedPreferences.contains(CleverPushPreferences.SUBSCRIPTION_ID)).thenReturn(true);
        boolean result = cleverPush.isSubscribed();
        Assertions.assertEquals(true, result);
    }

    @Test
    void testIncrementAppOpens() {
        doReturn(context).when(cleverPush).getContext();
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        when(sharedPreferences.edit()).thenReturn(editor);
        when(sharedPreferences.contains(CleverPushPreferences.SUBSCRIPTION_ID)).thenReturn(true);
        cleverPush.incrementAppOpens();
        verify(editor).putInt(CleverPushPreferences.APP_OPENS, sharedPreferences.getInt(CleverPushPreferences.APP_OPENS, 0) + 1);
        verify(editor).apply();
    }

    @Test
    void testWhenNotificationOpenedListenerIsNullDontFireListenerAndClearList() {
        doReturn(null).when(cleverPush).getChannelId(context);
        doReturn(context).when(cleverPush).getContext();
        doNothing().when(cleverPush).incrementAppOpens();
        when(context.getResources()).thenReturn(resources);
        cleverPush.init(null, null, null, null, true);
        verify(cleverPush, never()).fireNotificationOpenedListener(notificationOpenedResult);
    }

    @Test
    void testWhenNotificationOpenedListenerIsNotNullFireListenerAndClearList() {
        doReturn(null).when(cleverPush).getChannelId(context);
        doReturn(context).when(cleverPush).getContext();
        doNothing().when(cleverPush).incrementAppOpens();
        when(context.getResources()).thenReturn(resources);
        when(notificationOpenedResult.getNotification()).thenReturn(notification);
        when(notification.getAppBanner()).thenReturn(null);
        Collection<NotificationOpenedResult> unprocessedOpenedNotifications = new ArrayList<>();
        unprocessedOpenedNotifications.add(notificationOpenedResult);
        doReturn(unprocessedOpenedNotifications).when(cleverPush).getUnprocessedOpenedNotifications();
        cleverPush.init(null, null, notificationOpenedListener, null, true);
        verify(cleverPush).fireNotificationOpenedListener(notificationOpenedResult);
    }

    @Test
    void testSubscribeOrSyncForNewSubscription() {
        doReturn(context).when(cleverPush).getContext();
        doReturn("channelId").when(cleverPush).getChannelId(context);
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        when(sharedPreferences.edit()).thenReturn(editor);
        when(editor.putString(CleverPushPreferences.CHANNEL_ID, "channelId")).thenReturn(editor);
        when(sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null)).thenReturn(null);

        //for old subscription and auto register true
        cleverPush.subscribeOrSync(true);
        verify(cleverPush).subscribe(true);

        //for old subscription auto register false
        cleverPush.subscribeOrSync(false);
        verify(cleverPush).fireSubscribedListener(null);
        verify(cleverPush).setSubscriptionId(null);

        //for new subscription and sync is not due
        when(sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null)).thenReturn("subscription_id");
        when(sharedPreferences.getInt(CleverPushPreferences.SUBSCRIPTION_LAST_SYNC, 0)).thenReturn((int) (System.currentTimeMillis() / 1000L));
        cleverPush.subscribeOrSync(true);
        verify(cleverPush).fireSubscribedListener("subscription_id");
        verify(cleverPush).setSubscriptionId("subscription_id");

        //for new subscription and sync is due
        when(sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null)).thenReturn("subscription_id");
        when(sharedPreferences.getInt(CleverPushPreferences.SUBSCRIPTION_LAST_SYNC, 0)).thenReturn(-4 * 60 * 60 * 24);
        cleverPush.subscribeOrSync(true);
        verify(cleverPush).subscribe(false);


    }

    @Test
    void testInitFeaturesWhenThereIsNoCurrentActivity() {
        // when there is no current activity
        doReturn(null).when(cleverPush).getCurrentActivity();
        cleverPush.initFeatures();
        assertThat(cleverPush.isPendingInitFeaturesCall()).isTrue();
    }

    @Test
    void testInitFeaturesWhenThereIsCurrentActivity() {

        doReturn(activity).when(cleverPush).getCurrentActivity();
        doReturn(true).when(cleverPush).hasLocationPermission();
        doReturn(googleApiClient).when(cleverPush).getGoogleApiClient();
        doReturn(appBannerModule).when(cleverPush).getAppBannerModule();
        Map<String, String> pendingAppBannerEvents = new HashMap<>();
        pendingAppBannerEvents.put("key","value");
        cleverPush.initFeatures();
        assertThat(cleverPush.isPendingInitFeaturesCall()).isFalse();
        verify(cleverPush).showPendingTopicsDialog();
        verify(cleverPush).initAppReview();
        verify(cleverPush).initGeoFences();
        verify(appBannerModule).initSession(any());

    }


    @Test
    void testRequestLocationPermissionWhenThereIsAlreadyPermission() {
        doReturn(true).when(cleverPush).hasLocationPermission();
        cleverPush.requestLocationPermission();
        verify(cleverPush).requestLocationPermission();
        verify(cleverPush).hasLocationPermission();
        verifyNoMoreInteractions(cleverPush);

    }

    @Test
    void testRequestLocationPermissionWhenThereIsNoActivity() {
        doReturn(false).when(cleverPush).hasLocationPermission();
        doReturn(null).when(cleverPush).getCurrentActivity();
        cleverPush.requestLocationPermission();
        assertThat(cleverPush.ispendingRequestLocationPermissionCall()).isTrue();
    }

    @Test
    void testRequestLocationPermissionWhenThereIsActivity() {
        doReturn(false).when(cleverPush).hasLocationPermission();
        doReturn(activity).when(cleverPush).getCurrentActivity();
        cleverPush.requestLocationPermission();
        assertThat(cleverPush.ispendingRequestLocationPermissionCall()).isFalse();
        verify(activityCompat).requestPermissions(activity,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
    }

    @Test
    void testHasLocationPermission() {
        doReturn(context).when(cleverPush).getContext();
        when(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)).thenReturn(0);
        cleverPush.hasLocationPermission();
        assertThat(cleverPush.hasLocationPermission()).isTrue();
    }


}