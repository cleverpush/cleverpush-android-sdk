package com.cleverpush.responsehandlers;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.util.Logger;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static java.lang.Thread.sleep;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChannelConfigFromBundleIdResponseHandlerTest {

    private ChannelConfigFromBundleIdResponseHandler channelConfigFromBundleIdResponseHandler;
    private MockWebServer mockWebServer;
    private CleverPush cleverPush;

    @Mock
    CleverPushHttpClient cleverPushHttpClient;

    @Mock
    Context context;

    @Mock
    SharedPreferences sharedPreferences;

    @Mock
    SharedPreferences.Editor editor;

    @Mock
    Activity activity;

    @Mock
    Logger logger;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mockWebServer = new MockWebServer();
        cleverPush = Mockito.spy(new CleverPush(context));
        channelConfigFromBundleIdResponseHandler = Mockito.spy(new ChannelConfigFromBundleIdResponseHandler(cleverPush));
        try {
            mockWebServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testGetResponseHandlerWhenSuccess() {
        doReturn(context).when(channelConfigFromBundleIdResponseHandler).getContext();
        doReturn(sharedPreferences).when(channelConfigFromBundleIdResponseHandler).getSharedPreferences(context);
        doReturn(context).when(cleverPush).getContext();
        doReturn("channelId").when(cleverPush).getChannelId(context);
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        when(sharedPreferences.edit()).thenReturn(editor);
        when(editor.putString(CleverPushPreferences.CHANNEL_ID, "channelId")).thenReturn(editor);
        when(sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null)).thenReturn(null);
        doReturn(activity).when(cleverPush).getCurrentActivity();
        doNothing().when(cleverPush).subscribe(true);

        Map<String, Object> subscriptionAttributes = new HashMap<>();
        subscriptionAttributes.put("attributeId", "value");

        HttpUrl baseUrl = mockWebServer.url("/channel-config?bundleId=com.test&platformName=Android");
        CleverPushHttpClient.BASE_URL = baseUrl.toString().replace("/channel-config?bundleId=com.test&platformName=Android","");
        MockResponse mockResponse = new MockResponse().setBody("{\n" +
                "\t\"channelId\": \"channelId\",\n" +
                "}").setResponseCode(200);
        mockWebServer.enqueue(mockResponse);

        cleverPushHttpClient.get( "/channel-config?bundleId=com.test&platformName=Android", channelConfigFromBundleIdResponseHandler.getResponseHandler(true));

        try {
            sleep(600);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        verify(cleverPush).setInitialized(true);
        verify(cleverPush).setChannelConfig(any());
        verify(cleverPush).setChannelId("channelId");
        verify(cleverPush).subscribeOrSync(true);
        verify(cleverPush).initFeatures();
    }

    @Test
    void testGetResponseHandlerWhenFailureAndChannelConfigIsNull() {
        when(channelConfigFromBundleIdResponseHandler.getLogger()).thenReturn(logger);
        doReturn(context).when(channelConfigFromBundleIdResponseHandler).getContext();
        doReturn(sharedPreferences).when(channelConfigFromBundleIdResponseHandler).getSharedPreferences(context);
        doReturn(null).when(cleverPush).getChannelConfig();
        when(sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null)).thenReturn("subscriptionID");

        HttpUrl baseUrl = mockWebServer.url("/channel-config?bundleId=com.test&platformName=Android");
        CleverPushHttpClient.BASE_URL = baseUrl.toString().replace("/channel-config?bundleId=com.test&platformName=Android","");
        MockResponse mockResponse = new MockResponse().setBody("{}").setResponseCode(400);
        mockWebServer.enqueue(mockResponse);

        cleverPushHttpClient.get( "/channel-config?bundleId=com.test&platformName=Android", channelConfigFromBundleIdResponseHandler.getResponseHandler(true));

        try {
            sleep(600);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        verify(logger).e("CleverPush", "Failed to fetch Channel Config via Package Name. Did you specify the package name in the CleverPush channel settings?", null);
        verify(cleverPush).setInitialized(true);
        verify(cleverPush).fireSubscribedListener("subscriptionID");
        verify(cleverPush).setSubscriptionId("subscriptionID");
        verify(cleverPush).setChannelConfig(null);
    }

    @Test
    void testGetResponseHandlerWhenFailureAndChannelConfigIsNotNull() {
        when(channelConfigFromBundleIdResponseHandler.getLogger()).thenReturn(logger);
        doReturn(context).when(channelConfigFromBundleIdResponseHandler).getContext();
        doReturn(sharedPreferences).when(channelConfigFromBundleIdResponseHandler).getSharedPreferences(context);
        doReturn(new JSONObject()).when(cleverPush).getChannelConfig();
        when(sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null)).thenReturn("subscriptionID");

        HttpUrl baseUrl = mockWebServer.url("/channel-config?bundleId=com.test&platformName=Android");
        CleverPushHttpClient.BASE_URL = baseUrl.toString().replace("/channel-config?bundleId=com.test&platformName=Android","");
        MockResponse mockResponse = new MockResponse().setBody("{}").setResponseCode(400);
        mockWebServer.enqueue(mockResponse);

        cleverPushHttpClient.get( "/channel-config?bundleId=com.test&platformName=Android", channelConfigFromBundleIdResponseHandler.getResponseHandler(true));


        try {
            sleep(600);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        verify(logger).e("CleverPush", "Failed to fetch Channel Config via Package Name. Did you specify the package name in the CleverPush channel settings?", null);
        verify(cleverPush).setInitialized(true);
        verify(cleverPush, never()).fireSubscribedListener("subscriptionID");
        verify(cleverPush, never()).setSubscriptionId("subscriptionID");
        verify(cleverPush, never()).setChannelConfig(null);
    }

    @AfterEach
    public void tearDown() {
        try {
             mockWebServer.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
