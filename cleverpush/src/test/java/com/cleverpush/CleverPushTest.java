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
import com.cleverpush.listener.ChannelConfigListener;
import com.cleverpush.listener.NotificationOpenedListener;
import com.cleverpush.listener.SessionListener;
import com.cleverpush.listener.TrackingConsentListener;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.OngoingStubbing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static android.os.Looper.getMainLooper;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
        assertThat(cleverPush.isPendingRequestLocationPermissionCall()).isTrue();
    }

    @Test
    void testRequestLocationPermissionWhenThereIsActivity() {
        doReturn(false).when(cleverPush).hasLocationPermission();
        doReturn(activity).when(cleverPush).getCurrentActivity();
        cleverPush.requestLocationPermission();
        assertThat(cleverPush.isPendingRequestLocationPermissionCall()).isFalse();
        verify(activityCompat).requestPermissions(activity,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
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

}