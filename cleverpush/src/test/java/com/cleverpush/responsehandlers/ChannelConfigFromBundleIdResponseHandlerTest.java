package com.cleverpush.responsehandlers;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.responsehandlers.ChannelConfigFromBundleIdResponseHandler;

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

import static com.google.common.truth.Truth.assertThat;
import static java.lang.Thread.sleep;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
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

    @AfterEach
    public void tearDown() {
        try {
             mockWebServer.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}