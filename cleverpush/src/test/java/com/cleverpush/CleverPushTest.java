package com.cleverpush;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import com.cleverpush.listener.NotificationOpenedListener;
import com.cleverpush.manager.SubscriptionManagerFCM;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collection;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    CleverPush cleverPush;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        cleverPush = Mockito.spy(new CleverPush(context));
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
        verify(editor).putInt(CleverPushPreferences.APP_OPENS,sharedPreferences.getInt(CleverPushPreferences.APP_OPENS, 0) + 1);
        verify(editor).apply();
    }

    @Test
    void testWhenNotificationOpenedListenerIsNullDontFireListenerAndClearList() {
        doReturn(null).when(cleverPush).getChannelId(context);
        doReturn(context).when(cleverPush).getContext();
        doNothing().when(cleverPush).incrementAppOpens();
        when(context.getResources()).thenReturn(resources);
        cleverPush.init(null, null, null, null, true);
        verify(cleverPush,never()).fireNotificationOpenedListener(notificationOpenedResult);
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
   /* @Test
    void testUnSubscribe() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        doReturn(context).when(cleverPush).getContext();
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        when(sharedPreferences.contains(CleverPushPreferences.SUBSCRIPTION_ID)).thenReturn(true);
        boolean result = cleverPush.isSubscribed();
        Assertions.assertEquals(true, result);
    }*/
}