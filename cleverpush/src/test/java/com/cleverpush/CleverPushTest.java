package com.cleverpush;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.cleverpush.banner.AppBannerModule;
import com.cleverpush.listener.AppBannerOpenedListener;
import com.cleverpush.listener.ChannelAttributesListener;
import com.cleverpush.listener.ChannelConfigListener;
import com.cleverpush.listener.NotificationOpenedListener;
import com.cleverpush.listener.SessionListener;
import com.cleverpush.listener.SubscribedListener;
import com.cleverpush.listener.TrackingConsentListener;
import com.cleverpush.manager.SubscriptionManager;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static android.os.Looper.getMainLooper;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.validateMockitoUsage;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @Mock
    SubscribedListener subscribedListener;

    @Mock
    TrackingConsentListener trackingConsentListener;

    @Mock
    ChannelAttributesListener channelAttributesListener;

    @Mock
    AppBannerOpenedListener appBannerOpenedListener;

    @Mock
    SubscriptionManager subscriptionManager;

    @Mock
    SubscriptionManager.RegisteredHandler registeredHandler;

    @Mock
    JSONObject jsonObject;

    private Handler handler;
    private CleverPush cleverPush;
    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        cleverPush = Mockito.spy(new CleverPush(context));
        handler = Mockito.spy(new Handler(getMainLooper()));
        activityLifecycleListener = Mockito.spy(new ActivityLifecycleListener(sessionListener));
        mockWebServer = new MockWebServer();
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
    void testInitWhenChannleIdIsNull() {
        doReturn(null).when(cleverPush).getChannelId(context);
        doReturn(context).when(cleverPush).getContext();
        doNothing().when(cleverPush).incrementAppOpens();
        when(context.getResources()).thenReturn(resources);
        cleverPush.init(null, null, null, null, true);
        verify(cleverPush).getChannelId(context);
    }

    @Test
    void testInitWhenChannelIdIsNullAndAlsoNoChannelIdInPrefrence() {
        doReturn(null).when(cleverPush).getChannelId(context);
        doReturn(context).when(cleverPush).getContext();
        when(context.getResources()).thenReturn(resources);
        when(context.getPackageName()).thenReturn("com.test");
        doNothing().when(cleverPush).incrementAppOpens();
        cleverPush.init(null, null, null, null, true);
        verify(cleverPush).getChannelConfigFromBundleId("/channel-config?bundleId=com.test&platformName=Android", true);
    }

    @Test
    void testInitWhenChannelIdIsNotNull() {
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
    void testInitWhenChannelIdIsNotNullButChannelIdIsChanged() {
        doReturn("channelId").when(cleverPush).getChannelId(context);
        doReturn("subscriptionId").when(cleverPush).getSubscriptionId(context);
        doReturn(true).when(cleverPush).isChannelIdChanged("channelId","subscriptionId");
        doReturn(context).when(cleverPush).getContext();
        when(context.getResources()).thenReturn(resources);
        doNothing().when(cleverPush).addOrUpdateChannelId(context, "channelIdChanged");
        doNothing().when(cleverPush).incrementAppOpens();
        cleverPush.init("channelIdChanged", null, null, null, true);
        verify(cleverPush).clearSubscriptionData();
    }


    @Test
    void testInItWhenNotificationOpenedListenerIsNullDonotFireListenerAndClearList() {
        doReturn(null).when(cleverPush).getChannelId(context);
        doReturn(context).when(cleverPush).getContext();
        doNothing().when(cleverPush).incrementAppOpens();
        when(context.getResources()).thenReturn(resources);
        cleverPush.init(null, null, null, null, true);
        verify(cleverPush, never()).fireNotificationOpenedListener(notificationOpenedResult);
    }

    @Test
    void testInitWhenNotificationOpenedListenerIsNotNullFireListenerAndClearList() {
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
    void testSubscribeOrSyncForOldSubscriptionAndAutoRegisterTrue() {
        doReturn(context).when(cleverPush).getContext();
        doReturn("channelId").when(cleverPush).getChannelId(context);
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        when(sharedPreferences.edit()).thenReturn(editor);
        when(editor.putString(CleverPushPreferences.CHANNEL_ID, "channelId")).thenReturn(editor);
        when(sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null)).thenReturn(null);

        cleverPush.subscribeOrSync(true);

        verify(cleverPush).subscribe(true);
    }

    @Test
    void testSubscribeOrSyncForOldSubscriptionAndAutoRegisterFalse() {
        doReturn(context).when(cleverPush).getContext();
        doReturn("channelId").when(cleverPush).getChannelId(context);
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        when(sharedPreferences.edit()).thenReturn(editor);
        when(editor.putString(CleverPushPreferences.CHANNEL_ID, "channelId")).thenReturn(editor);
        when(sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null)).thenReturn(null);

        cleverPush.subscribeOrSync(false);

        verify(cleverPush).fireSubscribedListener(null);
        verify(cleverPush).setSubscriptionId(null);

    }

    @Test
    void testSubscribeOrSyncForNewSubscriptionAndSyncIsNotDue() {
        doReturn(context).when(cleverPush).getContext();
        doReturn("channelId").when(cleverPush).getChannelId(context);
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        when(sharedPreferences.edit()).thenReturn(editor);
        when(editor.putString(CleverPushPreferences.CHANNEL_ID, "channelId")).thenReturn(editor);
        when(sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null)).thenReturn(null);
        when(sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null)).thenReturn("subscription_id");
        when(sharedPreferences.getInt(CleverPushPreferences.SUBSCRIPTION_LAST_SYNC, 0)).thenReturn((int) (System.currentTimeMillis() / 1000L));

        cleverPush.subscribeOrSync(true);

        verify(cleverPush).fireSubscribedListener("subscription_id");
        verify(cleverPush).setSubscriptionId("subscription_id");
    }

    @Test
    void testSubscribeOrSyncForNewSubscriptionAndSyncIsDue() {
        doReturn(context).when(cleverPush).getContext();
        doReturn("channelId").when(cleverPush).getChannelId(context);
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        when(sharedPreferences.edit()).thenReturn(editor);
        when(editor.putString(CleverPushPreferences.CHANNEL_ID, "channelId")).thenReturn(editor);
        when(sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null)).thenReturn(null);
        when(sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null)).thenReturn("subscription_id");
        when(sharedPreferences.getInt(CleverPushPreferences.SUBSCRIPTION_LAST_SYNC, 0)).thenReturn(-4 * 60 * 60 * 24);

        cleverPush.subscribeOrSync(true);

        verify(cleverPush).subscribe(false);


    }

    @Test
    void testInitFeaturesWhenThereIsNoCurrentActivity() {
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
        assertThat(cleverPush.isPendingRequestLocationPermissionCall()).isTrue();
    }

    @Test
    void testRequestLocationPermissionWhenThereIsActivity() {
        doReturn(false).when(cleverPush).hasLocationPermission();
        doReturn(activity).when(cleverPush).getCurrentActivity();
        cleverPush.requestLocationPermission();
        assertThat(cleverPush.isPendingRequestLocationPermissionCall()).isFalse();
        //verify(activityCompat).requestPermissions(activity,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
    }

    @Test
    void testHasLocationPermission() {
        doReturn(context).when(cleverPush).getContext();
        when(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)).thenReturn(0);
        cleverPush.hasLocationPermission();
        assertThat(cleverPush.hasLocationPermission()).isTrue();
    }

    @Test
    void testTrackPageViewWhenNoActivity() {
        doReturn(null).when(cleverPush).getCurrentActivity();
        Map<String, Object> params = new HashMap<>();
        params.put("key", "value");
        cleverPush.trackPageView("url",params);
        assertThat(cleverPush.pendingPageViews.get(0).getUrl()).isEqualTo("url");
        assertThat(cleverPush.pendingPageViews.get(0).getParams()).isEqualTo(params);
    }

    @Test
    void testTrackPageViewWhenThereIsActivity() {
        doReturn(activity).when(cleverPush).getCurrentActivity();
        Map<String, Object> params = new HashMap<>();
        params.put("key", "value");
        cleverPush.trackPageView("https://url.com",params);
        assertThat(cleverPush.currentPageUrl).isEqualTo("https://url.com");
        verify(cleverPush).checkTags("https://url.com",params);
    }

    @Test
    void tesTrackSessionStartWhenThereIsConfigAndTrackAppStatisticsButNoSubscriptionID() {
        doReturn(context).when(cleverPush).getContext();
        doReturn(null).when(cleverPush).getSubscriptionId(context);
        doReturn("channelId").when(cleverPush).getChannelId(context);
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        when(sharedPreferences.getString(CleverPushPreferences.FCM_TOKEN, null)).thenReturn("token");
        when(sharedPreferences.getString(CleverPushPreferences.LAST_NOTIFICATION_ID, null)).thenReturn("notificationD");

        Answer<Void> trackingConsentListenerAnswer = new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                TrackingConsentListener callback = (TrackingConsentListener) invocation.getArguments()[0];
                callback.ready();
                return null;
            }
        };

        Answer<Void> channelConfigListenerAnswer = new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                ChannelConfigListener callback = (ChannelConfigListener) invocation.getArguments()[0];
                try {
                    JSONObject responseJson = new JSONObject("{ \"trackAppStatistics\": true}");
                    callback.ready(responseJson);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                return null;
            }
        };
        doAnswer(trackingConsentListenerAnswer).when(cleverPush).waitForTrackingConsent(any(TrackingConsentListener.class));
        doAnswer(channelConfigListenerAnswer).when(cleverPush).getChannelConfig(any(ChannelConfigListener.class));

        cleverPush.trackSessionStart();

        verify(cleverPush).updateServerSessionStart();
    }

    @Test
    void tesTrackSessionStartWhenThereIsNoConfigAndTrackAppStatisticsButSubscriptionID() {
        doReturn(context).when(cleverPush).getContext();
        doReturn("subscriptionId").when(cleverPush).getSubscriptionId(context);
        doReturn("channelId").when(cleverPush).getChannelId(context);
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        when(sharedPreferences.getString(CleverPushPreferences.FCM_TOKEN, null)).thenReturn("token");
        when(sharedPreferences.getString(CleverPushPreferences.LAST_NOTIFICATION_ID, null)).thenReturn("notificationD");

        Answer<Void> trackingConsentListenerAnswer = new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                TrackingConsentListener callback = (TrackingConsentListener) invocation.getArguments()[0];
                callback.ready();
                return null;
            }
        };

        Answer<Void> channelConfigListenerAnswer = new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                ChannelConfigListener callback = (ChannelConfigListener) invocation.getArguments()[0];
                callback.ready(null);
                return null;
            }
        };
        doAnswer(trackingConsentListenerAnswer).when(cleverPush).waitForTrackingConsent(any(TrackingConsentListener.class));
        doAnswer(channelConfigListenerAnswer).when(cleverPush).getChannelConfig(any(ChannelConfigListener.class));

        cleverPush.trackSessionStart();

        verify(cleverPush).updateServerSessionStart();
    }

    @Test
    void tesTrackSessionStartWhenThereIsConfigButNoTrackAppStatisticsAndSubscriptionID() {
        doReturn(context).when(cleverPush).getContext();
        doReturn(null).when(cleverPush).getSubscriptionId(context);
        Answer<Void> trackingConsentListenerAnswer = new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                TrackingConsentListener callback = (TrackingConsentListener) invocation.getArguments()[0];
                callback.ready();
                return null;
            }
        };

        Answer<Void> channelConfigListenerAnswer = new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                ChannelConfigListener callback = (ChannelConfigListener) invocation.getArguments()[0];
                try {
                    JSONObject responseJson = new JSONObject("{ }");
                    callback.ready(responseJson);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        doAnswer(trackingConsentListenerAnswer).when(cleverPush).waitForTrackingConsent(any(TrackingConsentListener.class));
        doAnswer(channelConfigListenerAnswer).when(cleverPush).getChannelConfig(any(ChannelConfigListener.class));

        cleverPush.trackSessionStart();

        verify(cleverPush, never()).updateServerSessionStart();
    }


//    @Test
//    void tesTrackSessionEndWhenThereIsNoSessionStartTime() {
//        when(cleverPush.getSessionStartedTimestamp()).thenReturn(0L);
//
//        cleverPush.trackSessionEnd();
//
//        verify(cleverPush).trackSessionEnd();
//        verifyNoMoreInteractions(cleverPush);
//    }

    @Test
    void tesTrackSessionEndWhenThereIsConfigAndTrackAppStatisticsButNoSubscriptionID() {
        doReturn(context).when(cleverPush).getContext();
        doReturn(null).when(cleverPush).getSubscriptionId(context);
        doReturn("channelId").when(cleverPush).getChannelId(context);
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        when(sharedPreferences.getString(CleverPushPreferences.FCM_TOKEN, null)).thenReturn("token");
        when(cleverPush.getSessionStartedTimestamp()).thenReturn(1L);

        Answer<Void> trackingConsentListenerAnswer = new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                TrackingConsentListener callback = (TrackingConsentListener) invocation.getArguments()[0];
                callback.ready();
                return null;
            }
        };

        Answer<Void> channelConfigListenerAnswer = new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                ChannelConfigListener callback = (ChannelConfigListener) invocation.getArguments()[0];
                try {
                    JSONObject responseJson = new JSONObject("{ \"trackAppStatistics\": true}");
                    callback.ready(responseJson);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                return null;
            }
        };
        doAnswer(trackingConsentListenerAnswer).when(cleverPush).waitForTrackingConsent(any(TrackingConsentListener.class));
        doAnswer(channelConfigListenerAnswer).when(cleverPush).getChannelConfig(any(ChannelConfigListener.class));

        cleverPush.trackSessionEnd();

        verify(cleverPush).updateServerSessionEnd();
    }

    @Test
    void tesTrackSessionEndWhenThereIsNoConfigAndTrackAppStatisticsButSubscriptionID() {
        doReturn(context).when(cleverPush).getContext();
        doReturn("subscriptionId").when(cleverPush).getSubscriptionId(context);
        doReturn("channelId").when(cleverPush).getChannelId(context);
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        when(sharedPreferences.getString(CleverPushPreferences.FCM_TOKEN, null)).thenReturn("token");
        when(cleverPush.getSessionStartedTimestamp()).thenReturn(1L);

        Answer<Void> trackingConsentListenerAnswer = new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                TrackingConsentListener callback = (TrackingConsentListener) invocation.getArguments()[0];
                callback.ready();
                return null;
            }
        };

        Answer<Void> channelConfigListenerAnswer = new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                ChannelConfigListener callback = (ChannelConfigListener) invocation.getArguments()[0];
                callback.ready(null);
                return null;
            }
        };
        doAnswer(trackingConsentListenerAnswer).when(cleverPush).waitForTrackingConsent(any(TrackingConsentListener.class));
        doAnswer(channelConfigListenerAnswer).when(cleverPush).getChannelConfig(any(ChannelConfigListener.class));

        cleverPush.trackSessionEnd();

        verify(cleverPush).updateServerSessionEnd();
    }

    @Test
    void tesTrackSessionEndWhenThereIsConfigButNoTrackAppStatisticsAndSubscriptionID() {
        doReturn(context).when(cleverPush).getContext();
        doReturn(null).when(cleverPush).getSubscriptionId(context);
        Answer<Void> trackingConsentListenerAnswer = new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                TrackingConsentListener callback = (TrackingConsentListener) invocation.getArguments()[0];
                callback.ready();
                return null;
            }
        };

        Answer<Void> channelConfigListenerAnswer = new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                ChannelConfigListener callback = (ChannelConfigListener) invocation.getArguments()[0];
                try {
                    JSONObject responseJson = new JSONObject("{ }");
                    callback.ready(responseJson);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        doAnswer(trackingConsentListenerAnswer).when(cleverPush).waitForTrackingConsent(any(TrackingConsentListener.class));
        doAnswer(channelConfigListenerAnswer).when(cleverPush).getChannelConfig(any(ChannelConfigListener.class));

        cleverPush.trackSessionEnd();

        verify(cleverPush, never()).updateServerSessionEnd();
    }

    @Test
    void testUpdateServerSessionStart(){
        String expectedFCMToken = "token";
        String expectedLastNotificationID = "notificationD";
        String expectedSubscriptionID = "subscriptionID";
        String expectedChannelID = "channelId";

        doReturn(context).when(cleverPush).getContext();
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        when(sharedPreferences.getString(CleverPushPreferences.FCM_TOKEN, null)).thenReturn(expectedFCMToken);
        when(sharedPreferences.getString(CleverPushPreferences.LAST_NOTIFICATION_ID, null)).thenReturn(expectedLastNotificationID);
        doReturn(expectedSubscriptionID).when(cleverPush).getSubscriptionId(context);
        doReturn(expectedChannelID).when(cleverPush).getChannelId(context);

        try {
            mockWebServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        HttpUrl baseUrl = mockWebServer.url("/subscription/session/start");
        cleverPush.setApiEndpoint(baseUrl.toString().replace("/subscription/session/start",""));
        MockResponse mockResponse = new MockResponse().setBody("{}").setResponseCode(200);
        mockWebServer.enqueue(mockResponse);

        cleverPush.updateServerSessionStart();

        try {
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getPath()).isEqualTo("/subscription/session/start");
            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("{\"lastNotificationId\":\"notificationD\",\"subscriptionId\":\"subscriptionID\",\"fcmToken\":\"token\",\"channelId\":\"channelId\"}");
            mockWebServer.shutdown();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testUpdateServerSessionEnd(){
        String expectedFCMToken = "token";
        Long expectedSessionStartedTimestamp = 1L;
        int expectedSessionVisits = 1;
        String expectedSubscriptionID = "subscriptionID";
        String expectedChannelID = "channelId";

        doReturn(context).when(cleverPush).getContext();
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        when(sharedPreferences.getString(CleverPushPreferences.FCM_TOKEN, null)).thenReturn(expectedFCMToken);
        when(cleverPush.getSessionStartedTimestamp()).thenReturn(expectedSessionStartedTimestamp);
        doReturn(expectedChannelID).when(cleverPush).getChannelId(context);
        doReturn(expectedSubscriptionID).when(cleverPush).getSubscriptionId(context);
        doReturn(expectedSessionVisits).when(cleverPush).getSessionVisits();

        try {
            mockWebServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        HttpUrl baseUrl = mockWebServer.url("/subscription/session/end");
        cleverPush.setApiEndpoint(baseUrl.toString().replace("/subscription/session/end",""));
        MockResponse mockResponse = new MockResponse().setBody("{}").setResponseCode(200);
        mockWebServer.enqueue(mockResponse);

        String expectedDuration = String.valueOf(System.currentTimeMillis() / 1000L-expectedSessionStartedTimestamp);
        cleverPush.updateServerSessionEnd();

        try {
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getPath()).isEqualTo("/subscription/session/end");
            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("{\"duration\":"+expectedDuration+",\"visits\":1,\"subscriptionId\":\"subscriptionID\",\"fcmToken\":\"token\",\"channelId\":\"channelId\"}");
            mockWebServer.shutdown();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testInitAppReview(){
        doReturn(context).when(cleverPush).getContext();
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        doReturn(activity).when(cleverPush).getCurrentActivity();
        when(sharedPreferences.getLong(CleverPushPreferences.APP_REVIEW_SHOWN, 0)).thenReturn(0L);
        when(sharedPreferences.getLong(CleverPushPreferences.SUBSCRIPTION_CREATED_AT, 0)).thenReturn(1L);
        when(sharedPreferences.getInt(CleverPushPreferences.APP_OPENS, 1)).thenReturn(1);
        when(cleverPush.getSessionStartedTimestamp()).thenReturn(1L);

        Answer<Void> channelConfigListenerAnswer = new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                ChannelConfigListener callback = (ChannelConfigListener) invocation.getArguments()[0];
                try {
                    JSONObject responseJson = new JSONObject("{\n" +
                            "  \"appReviewEnabled\": true,\n" +
                            "  \"appReviewSeconds\": 1,\n" +
                            "  \"appReviewOpens\": 1,\n" +
                            "  \"appReviewDays\": 1\n" +
                            "}");
                    callback.ready(responseJson);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };

        Answer<Void> runOnUiThreadAnswer = new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                Runnable callback = (Runnable) invocation.getArguments()[0];
                callback.run();
                return null;
            }
        };
        final Handler handler = mock(Handler.class);
        Answer<Void> handlerAnswer = new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                Message msg = invocation.getArgument(0, Message.class);
                msg.getCallback().run();
                return null;
            }
        };
        doAnswer(channelConfigListenerAnswer).when(cleverPush).getChannelConfig(any(ChannelConfigListener.class));
        doAnswer(runOnUiThreadAnswer).when(activity).runOnUiThread(any(Runnable.class));
        doAnswer(handlerAnswer).when(handler).sendMessageAtTime(any(Message.class), anyLong());

        cleverPush.initAppReview();

    }

    @Test
    void testSetAndGetAutoClearBadge(){
        cleverPush.setAutoClearBadge(true);
        assertThat(cleverPush.getAutoClearBadge()).isTrue();
    }

    @Test
    void testSetAndGetIncrementBadge(){
        cleverPush.setIncrementBadge(true);
        assertThat(cleverPush.getIncrementBadge()).isTrue();
    }

    @Test
    void testFireSubscribedListenerWhenSubscribedListenerAndSubscriptionIdIsNull(){
        when(cleverPush.getSubscribedListener()).thenReturn(null);
        cleverPush.fireSubscribedListener(null);
        verify(cleverPush).fireSubscribedListener(null);
        verify(subscribedListener, never()).subscribed(null);
    }

    @Test
    void testFireSubscribedListenerWhenSubscribedListenerAndSubscriptionIdIsNotNull(){
        when(cleverPush.getSubscribedListener()).thenReturn(subscribedListener);
        cleverPush.fireSubscribedListener("subscriptionId");
        verify(subscribedListener).subscribed("subscriptionId");
    }

    @Test
    void testTrySubscriptionSyncWhenSubscriptionInProgress(){
        doReturn(context).when(cleverPush).getContext();
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        when(sharedPreferences.getInt(CleverPushPreferences.SUBSCRIPTION_LAST_SYNC, 0)).thenReturn((int) ((System.currentTimeMillis() / 1000L)-10));
        when(sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null)).thenReturn("subscriptionId");
        doReturn(true).when(cleverPush).isSubscriptionInProgress();

        cleverPush.trySubscriptionSync();

        verify(cleverPush, never()).subscribe(false);
    }

    @Test
    void testTrySubscriptionSyncWhenThereIsNoSubscriptionId(){
        doReturn(context).when(cleverPush).getContext();
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        when(sharedPreferences.getInt(CleverPushPreferences.SUBSCRIPTION_LAST_SYNC, 0)).thenReturn((int) ((System.currentTimeMillis() / 1000L)-10));
        when(sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null)).thenReturn(null);
        doReturn(false).when(cleverPush).isSubscriptionInProgress();

        cleverPush.trySubscriptionSync();

        verify(cleverPush, never()).subscribe(false);
    }

    @Test
    void testTrySubscriptionSyncWhenThereIsTimeForNextSync(){
        doReturn(context).when(cleverPush).getContext();
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        when(sharedPreferences.getInt(CleverPushPreferences.SUBSCRIPTION_LAST_SYNC, 0)).thenReturn((int) ((System.currentTimeMillis() / 1000L)+10));
        when(sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null)).thenReturn("subscriptionId");
        doReturn(false).when(cleverPush).isSubscriptionInProgress();

        cleverPush.trySubscriptionSync();

        verify(cleverPush, never()).subscribe(false);
    }

    @Test
    void testTrySubscriptionSyncWhenThereIsNoSubscriptionInProgressThereIsSubscriptionIdAndNextSyncTimeIsPassed(){
        doReturn(context).when(cleverPush).getContext();
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        when(sharedPreferences.getInt(CleverPushPreferences.SUBSCRIPTION_LAST_SYNC, 0)).thenReturn((int) ((System.currentTimeMillis() / 1000L)-10));
        when(sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null)).thenReturn("subscriptionId");
        doReturn(false).when(cleverPush).isSubscriptionInProgress();

        cleverPush.trySubscriptionSync();

        verify(cleverPush).subscribe(false);
    }

    @Test
    void testWaitForTrackingConsentWhenThereIsNoTrackingConsentListener(){
        Collection<TrackingConsentListener> trackingConsentListenerArrayList = new ArrayList<>();
        doReturn(trackingConsentListenerArrayList).when(cleverPush).getTrackingConsentListeners();

        cleverPush.waitForTrackingConsent(null);

        assertThat(trackingConsentListenerArrayList.size()).isEqualTo(0);
        verify(trackingConsentListener, never()).ready();
    }

    @Test
    void testWaitForTrackingConsentWhenThereIsNoTrackingConsentRequired(){
        Collection<TrackingConsentListener> trackingConsentListenerArrayList = new ArrayList<>();
        doReturn(trackingConsentListenerArrayList).when(cleverPush).getTrackingConsentListeners();
        doReturn(false).when(cleverPush).isTrackingConsentRequired();
        doReturn(false).when(cleverPush).hasTrackingConsentCalled();

        cleverPush.waitForTrackingConsent(trackingConsentListener);

        verify(trackingConsentListener).ready();
    }

    @Test
    void testWaitForTrackingConsentWhenThereIsTrackingConsentCalled(){
        Collection<TrackingConsentListener> trackingConsentListenerArrayList = new ArrayList<>();
        doReturn(trackingConsentListenerArrayList).when(cleverPush).getTrackingConsentListeners();
        doReturn(true).when(cleverPush).isTrackingConsentRequired();
        doReturn(true).when(cleverPush).hasTrackingConsentCalled();

        cleverPush.waitForTrackingConsent(trackingConsentListener);

        verify(trackingConsentListener).ready();
    }

    @Test
    void testWaitForTrackingConsentWhenThereIsTrackingConsentRequiredAndTrackingConsentNotCalled(){
        Collection<TrackingConsentListener> trackingConsentListenerArrayList = new ArrayList<>();
        doReturn(trackingConsentListenerArrayList).when(cleverPush).getTrackingConsentListeners();
        doReturn(true).when(cleverPush).isTrackingConsentRequired();
        doReturn(false).when(cleverPush).hasTrackingConsentCalled();

        cleverPush.waitForTrackingConsent(trackingConsentListener);

        assertThat(trackingConsentListenerArrayList.size()).isGreaterThan(0);
        assertThat(trackingConsentListenerArrayList.size()).isEqualTo(1);
    }

    @Test
    void testGetSubscriptionTags(){
        Set<String> tags = new HashSet<String>();;
        tags.add("tagId");

        doReturn(context).when(cleverPush).getContext();
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        when(sharedPreferences.getStringSet(CleverPushPreferences.SUBSCRIPTION_TAGS, new HashSet<>())).thenReturn(tags);

        assertThat(cleverPush.getSubscriptionTags().size()).isEqualTo(tags.size());
        assertThat(cleverPush.getSubscriptionTags().contains("tagId")).isTrue();

    }

    @Test
    void testHasSubscriptionTagWhenItIsTrue(){
        Set<String> tags = new HashSet<String>();;
        tags.add("tagId");

        doReturn(context).when(cleverPush).getContext();
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        when(sharedPreferences.getStringSet(CleverPushPreferences.SUBSCRIPTION_TAGS, new HashSet<>())).thenReturn(tags);

        assertThat(cleverPush.hasSubscriptionTag("tagId")).isTrue();

    }

    @Test
    void testHasSubscriptionTagWhenItIsFalse(){
        Set<String> tags = new HashSet<String>();;
        tags.add("tagId");

        doReturn(context).when(cleverPush).getContext();
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        when(sharedPreferences.getStringSet(CleverPushPreferences.SUBSCRIPTION_TAGS, new HashSet<>())).thenReturn(tags);

        assertThat(cleverPush.hasSubscriptionTag("tagIdTwo")).isFalse();

    }

    @Test
    void testGetAvailableAttributes() {
        JSONObject responseJson = null;
        try {
           responseJson  = new JSONObject("{ }");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject finalResponseJson = responseJson;
        Answer<Void> channelConfigListenerAnswer = new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                ChannelConfigListener callback = (ChannelConfigListener) invocation.getArguments()[0];
                callback.ready(finalResponseJson);
                return null;
            }
        };
        doAnswer(channelConfigListenerAnswer).when(cleverPush).getChannelConfig(any(ChannelConfigListener.class));

        cleverPush.getAvailableAttributes(channelAttributesListener);

        verify(cleverPush).getAvailableAttributesFromConfig(finalResponseJson);
    }

    @Test
    void testGetAvailableAttributesFromConfigWhenChannelConfigIsNull() {
        assertThat(cleverPush.getAvailableAttributesFromConfig(null).size()).isEqualTo(0);
    }

    @Test
    void testGetAvailableAttributesFromConfigWhenChannelConfigDoNotHaveCustomAttributes() {
        try {
            JSONObject channelConfig  = new JSONObject("{ }");
            assertThat(cleverPush.getAvailableAttributesFromConfig(channelConfig).size()).isEqualTo(0);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    /*@Test
    void testGetAvailableAttributesFromConfigWhenThereIsException() {
        Throwable throwable = null;

        try {
            JSONObject channelConfig  = new JSONObject("{\n" +
                    "\t\"customAttributes\": null,\n" +
                    "}");
            cleverPush.getAvailableAttributesFromConfig(channelConfig);
        } catch (JSONException exception) {
            throwable = exception;
        }
        assertThat(throwable instanceof JSONException).isTrue();
    }*/

    @Test
    void testGetAvailableAttributesFromConfigWhenChannelConfigHaveZeroCustomAttributes() {
        try {
            JSONObject channelConfig  = new JSONObject("{\"customAttributes\":[]}");
            assertThat(cleverPush.getAvailableAttributesFromConfig(channelConfig).size()).isEqualTo(0);
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
    }

    @Test
    void testGetAvailableAttributesFromConfigWhenChannelConfigHaveNonZeroCustomAttributes() {
        Set<CustomAttribute> attributes = new HashSet<>();
        try {
            JSONObject channelConfig  = new JSONObject("{\n" +
                    "\t\"customAttributes\": [{\n" +
                    "\t\t\"id\": \"1\",\n" +
                    "\t\t\"name\": \"test\"\n" +
                    "\t}]\n" +
                    "}");
            assertThat(cleverPush.getAvailableAttributesFromConfig(channelConfig).size()).isEqualTo(1);
            attributes = cleverPush.getAvailableAttributesFromConfig(channelConfig);
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
        List<CustomAttribute> attributesList = new ArrayList<CustomAttribute>(attributes);

        assertThat(attributesList.get(0).getId()).isEqualTo("1");
        assertThat(attributesList.get(0).getName()).isEqualTo("test");
    }

    @Test
    void testGetSubscriptionAttributeWhenThereIsAttributeValueForAttributeId() {
        doReturn(context).when(cleverPush).getContext();
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        when(sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ATTRIBUTES, (new JSONObject()).toString())).thenReturn("{\"firstname\":\"123\"}");

        assertThat(cleverPush.getSubscriptionAttribute("firstname")).isEqualTo("123");
    }

    @Test
    void testGetSubscriptionAttributeWhenThereIsNoAttributeValueForAttributeId() {
        doReturn(context).when(cleverPush).getContext();
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        when(sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ATTRIBUTES, (new JSONObject()).toString())).thenReturn("{\"firstname\":\"123\"}");

        assertThat(cleverPush.getSubscriptionAttribute("first")).isNull();
    }

    @Test
    void testGetSubscriptionAttributeWhenThereIsNoSharedPreferences() {
        doReturn(context).when(cleverPush).getContext();
        doReturn(null).when(cleverPush).getSharedPreferences(context);
        //when(sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ATTRIBUTES, (new JSONObject()).toString())).thenReturn("{\"firstname\":\"123\"}");

        assertThat(cleverPush.getSubscriptionAttributes().size()).isEqualTo(0);
    }

    @Test
    void testGetSubscriptionAttributeWhenThereIsZeroSubscriptionAttributes() {
        doReturn(context).when(cleverPush).getContext();
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        when(sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ATTRIBUTES, (new JSONObject()).toString())).thenReturn("{}");

        assertThat(cleverPush.getSubscriptionAttributes().size()).isEqualTo(0);
    }

    @Test
    void testGetSubscriptionAttributeWhenThereIsSubscriptionAttributes() {
        Map<String, Object> outputMap;
        doReturn(context).when(cleverPush).getContext();
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        when(sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ATTRIBUTES, (new JSONObject()).toString())).thenReturn("{\"firstname\":\"123\"}");

        outputMap = cleverPush.getSubscriptionAttributes();

        assertThat(outputMap.get("firstname")).isEqualTo("123");
        assertThat(outputMap.size()).isEqualTo(1);
    }

    @Test
    void testGetSubscriptionTopics(){
        Set<String> topics = new HashSet<String>();;
        topics.add("tagId");

        doReturn(context).when(cleverPush).getContext();
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        when(sharedPreferences.getStringSet(CleverPushPreferences.SUBSCRIPTION_TOPICS, new HashSet<>())).thenReturn(topics);

        assertThat(cleverPush.getSubscriptionTopics().size()).isEqualTo(topics.size());
        assertThat(cleverPush.getSubscriptionTopics().contains("tagId")).isTrue();
    }

    @Test
    void testGetNotificationsWhenThereIsNoNotificationsStored(){
        doReturn(context).when(cleverPush).getContext();
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        when(sharedPreferences.getString(CleverPushPreferences.NOTIFICATIONS_JSON, null)).thenReturn(null);
        when(sharedPreferences.getStringSet(CleverPushPreferences.NOTIFICATIONS, new HashSet<>())).thenReturn(null);

        assertThat(cleverPush.getNotifications().size()).isEqualTo(0);
    }

    @Test
    void testGetNotificationsWhenThereIsNotificationJson(){
        Set<Notification> notifications;
        doReturn(context).when(cleverPush).getContext();
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        when(sharedPreferences.getString(CleverPushPreferences.NOTIFICATIONS_JSON, null)).thenReturn("[{\"actions\":[],\"createdAt\":\"2021-08-18T12:59:51.840Z\",\"_id\":\"RfS4GzvnjzwXa9cqr\",\"rawPayload\":\"{\\\"createdAt\\\":\\\"2021-08-18T12:59:51.840Z\\\",\\\"mediaUrl\\\":null,\\\"_id\\\":\\\"RfS4GzvnjzwXa9cqr\\\",\\\"iconUrl\\\":null,\\\"tag\\\":\\\"RfS4GzvnjzwXa9cqr\\\",\\\"text\\\":\\\"test\\\",\\\"title\\\":\\\"test3\\\",\\\"actions\\\":[],\\\"expiresAt\\\":null,\\\"url\\\":\\\"https:\\\\/\\\\/app.cleverpush.com\\\\/en\\\\/app\\\\/create-new?utm_source\\u003dbrowser\\u0026utm_medium\\u003dpush-notification\\u0026utm_campaign\\u003dcleverpush-1629291566\\\"}\",\"tag\":\"RfS4GzvnjzwXa9cqr\",\"text\":\"test\",\"title\":\"test3\",\"url\":\"https://app.cleverpush.com/en/app/create-new?utm_source\\u003dbrowser\\u0026utm_medium\\u003dpush-notification\\u0026utm_campaign\\u003dcleverpush-1629291566\"}]");
        when(sharedPreferences.getStringSet(CleverPushPreferences.NOTIFICATIONS, new HashSet<>())).thenReturn(null);

        notifications =  cleverPush.getNotifications();
        List<Notification> notificationList = new ArrayList<Notification>(notifications);

        assertThat(notificationList.size()).isEqualTo(1);
        assertThat(notificationList.get(0).getId()).isEqualTo("RfS4GzvnjzwXa9cqr");
    }

    @Test
    void testSetAppBannerOpenedListener(){
        cleverPush.setAppBannerOpenedListener(appBannerOpenedListener);
        assertThat(cleverPush.getAppBannerOpenedListener()).isEqualTo(appBannerOpenedListener);
    }

    @Test
    void testTriggerAppBannerEventWhenAppBannerModuleIsNotInitialized(){
        doReturn(null).when(cleverPush).getAppBannerModule();

        cleverPush.triggerAppBannerEvent("key","value");

        assertThat(cleverPush.getPendingAppBannerEvents().size()).isEqualTo(1);
        assertThat(cleverPush.getPendingAppBannerEvents().containsKey("key")).isTrue();
        assertThat(cleverPush.getPendingAppBannerEvents().containsValue("value")).isTrue();
    }

    @Test
    void testTriggerAppBannerEventWhenAppBannerModuleIsInitialized(){
        doReturn(appBannerModule).when(cleverPush).getAppBannerModule();

        cleverPush.triggerAppBannerEvent("key","value");

        verify(appBannerModule).triggerEvent("key","value");
    }

    @Test
    void testClearSubscriptionData(){
        doReturn(context).when(cleverPush).getContext();
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        when(sharedPreferences.edit()).thenReturn(editor);

        cleverPush.clearSubscriptionData();

        verify(editor).remove(CleverPushPreferences.SUBSCRIPTION_ID);
        verify(editor).remove(CleverPushPreferences.SUBSCRIPTION_LAST_SYNC);
        verify(editor).remove(CleverPushPreferences.SUBSCRIPTION_CREATED_AT);
        verify(editor).remove(CleverPushPreferences.SUBSCRIPTION_TOPICS);
        verify(editor).remove(CleverPushPreferences.SUBSCRIPTION_TOPICS_VERSION);
        verify(editor).remove(CleverPushPreferences.SUBSCRIPTION_TAGS);
        verify(editor).remove(CleverPushPreferences.SUBSCRIPTION_ATTRIBUTES);
        verify(editor).apply();
    }

//    @Test
//    void testSubscribeWhenSubscriptionInProgress(){
//        doReturn(true).when(cleverPush).isSubscriptionInProgress();
//
//        cleverPush.subscribe(false);
//
//        //verify(cleverPush).subscribe(false);
//        verifyNoMoreInteractions(cleverPush);
//    }
//
//    @Test
//    void testSubscribeWhenNewSubscriptionIdNull(){
//        doReturn(false).when(cleverPush).isSubscriptionInProgress();
//        doReturn(subscriptionManager).when(cleverPush).getSubscriptionManager();
//
//        Answer<Void> channelConfigListenerAnswer = new Answer<Void>() {
//            public Void answer(InvocationOnMock invocation) {
//                ChannelConfigListener callback = (ChannelConfigListener) invocation.getArguments()[0];
//                try {
//                    JSONObject responseJson = new JSONObject("{}");
//                    callback.ready(responseJson);
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//
//                return null;
//            }
//        };
//
//        Answer<Void> subscriptionManagerAnswer = new Answer<Void>() {
//            public Void answer(InvocationOnMock invocation) {
//                SubscriptionManager callback = (SubscriptionManager) invocation.getArguments()[0];
//                try {
//                    JSONObject responseJson = new JSONObject("{}");
//                    callback.subscribe(responseJson, registeredHandler);
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//
//                return null;
//            }
//        };
//
//        Answer<Void> registeredHandlerAnswer = new Answer<Void>() {
//            public Void answer(InvocationOnMock invocation) {
//                SubscriptionManager.RegisteredHandler callback = (SubscriptionManager.RegisteredHandler) invocation.getArguments()[0];
//                callback.complete(null);
//                return null;
//            }
//        };
//
//        doAnswer(channelConfigListenerAnswer).when(cleverPush).getChannelConfig(any(ChannelConfigListener.class));
//        doAnswer(registeredHandlerAnswer).when(registeredHandler).complete(null);
//        try {
//            JSONObject responseJson = new JSONObject("{}");
//            doAnswer(subscriptionManagerAnswer).when(subscriptionManager).subscribe(responseJson,registeredHandler);
//
//        } catch (JSONException exception) {
//            exception.printStackTrace();
//        }
//
//        cleverPush.subscribe(false);
//
//        //verify(cleverPush).subscribe(false);
//    }

    @Test
    void testUnsubscribe(){
        String expectedSubscriptionID = "subscriptionID";
        String expectedChannelID = "channelId";

        doReturn(context).when(cleverPush).getContext();
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        doReturn(expectedChannelID).when(cleverPush).getChannelId(context);
        doReturn(expectedSubscriptionID).when(cleverPush).getSubscriptionId(context);

        try {
            mockWebServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        HttpUrl baseUrl = mockWebServer.url("/subscription/unsubscribe");
        cleverPush.setApiEndpoint(baseUrl.toString().replace("/subscription/unsubscribe",""));
        MockResponse mockResponse = new MockResponse().setBody("{}").setResponseCode(200);
        mockWebServer.enqueue(mockResponse);

        cleverPush.unsubscribe();

        try {
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getPath()).isEqualTo("/subscription/unsubscribe");
            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            mockWebServer.shutdown();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testUnsubscribeWhenThereISJSONException(){
        String expectedSubscriptionID = "subscriptionID";
        String expectedChannelID = "channelId";
        doReturn(context).when(cleverPush).getContext();
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        doReturn(expectedChannelID).when(cleverPush).getChannelId(context);
        doReturn(expectedSubscriptionID).when(cleverPush).getSubscriptionId(context);
        doReturn(jsonObject).when(cleverPush).getJsonObject();

        try {
            when(jsonObject.put("channelId", expectedChannelID)).thenThrow(new JSONException("Error"));
        } catch (JSONException exception) {
            exception.printStackTrace();
        }

        cleverPush.unsubscribe();
        assertThrows(
                JSONException.class,
                () -> jsonObject.put("channelId", expectedChannelID),
                "Error"
        );
    }


    @Test
    void testSetSubscriptionAttribute(){
        Map<String, Object> subscriptionAttributes = new HashMap<>();

        doReturn(context).when(cleverPush).getContext();
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        doReturn("channelId").when(cleverPush).getChannelId(context);
        doReturn(subscriptionAttributes).when(cleverPush).getSubscriptionAttributes();

        Answer<Void> trackingConsentListenerAnswer = new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                TrackingConsentListener callback = (TrackingConsentListener) invocation.getArguments()[0];
                callback.ready();
                return null;
            }
        };

        Answer<Void> subscribedListenerAnswer = new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                SubscribedListener callback = (SubscribedListener) invocation.getArguments()[0];
                callback.subscribed("subscriptionId");
                return null;
            }
        };
        doAnswer(trackingConsentListenerAnswer).when(cleverPush).waitForTrackingConsent(any(TrackingConsentListener.class));
        doAnswer(subscribedListenerAnswer).when(cleverPush).getSubscriptionId(any(SubscribedListener.class));


        try {
            mockWebServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        HttpUrl baseUrl = mockWebServer.url("/subscription/attribute");
        cleverPush.setApiEndpoint(baseUrl.toString().replace("/subscription/attribute",""));
        MockResponse mockResponse = new MockResponse().setBody("{}").setResponseCode(200);
        mockWebServer.enqueue(mockResponse);

        cleverPush.setSubscriptionAttribute("attributeId","value");

        try {
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getPath()).isEqualTo("/subscription/attribute");
            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            mockWebServer.shutdown();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testSetSubscriptionAttributeWhenThereIsJSONException(){
        doReturn(context).when(cleverPush).getContext();
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        doReturn("channelId").when(cleverPush).getChannelId(context);
        doReturn(jsonObject).when(cleverPush).getJsonObject();

        Answer<Void> trackingConsentListenerAnswer = new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                TrackingConsentListener callback = (TrackingConsentListener) invocation.getArguments()[0];
                callback.ready();
                return null;
            }
        };

        Answer<Void> subscribedListenerAnswer = new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                SubscribedListener callback = (SubscribedListener) invocation.getArguments()[0];
                callback.subscribed("subscriptionId");
                return null;
            }
        };
        doAnswer(trackingConsentListenerAnswer).when(cleverPush).waitForTrackingConsent(any(TrackingConsentListener.class));
        doAnswer(subscribedListenerAnswer).when(cleverPush).getSubscriptionId(any(SubscribedListener.class));


        try {
            when(jsonObject.put("channelId", "channelId")).thenThrow(new JSONException("Error"));
        } catch (JSONException exception) {
            exception.printStackTrace();
        }

        cleverPush.unsubscribe();
        assertThrows(
                JSONException.class,
                () -> jsonObject.put("channelId", "channelId"),
                "Error"
        );
    }

    @AfterEach
    public void validate() {
        validateMockitoUsage();
    }
}