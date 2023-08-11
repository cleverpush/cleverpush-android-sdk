package com.cleverpush.responsehandlers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.listener.RemoveTagCompletedListener;
import com.cleverpush.util.Logger;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SetSubscriptionAttributeResponseHandlerTest {

    private SetSubscriptionAttributeResponseHandler setSubscriptionAttributeResponseHandler;
    private MockWebServer mockWebServer;

    @Mock
    CleverPushHttpClient cleverPushHttpClient;

    @Mock
    Context context;

    @Mock
    SharedPreferences sharedPreferences;

    @Mock
    SharedPreferences.Editor editor;

    @Mock
    Logger logger;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockWebServer = new MockWebServer();
        setSubscriptionAttributeResponseHandler = Mockito.spy(new SetSubscriptionAttributeResponseHandler());
        try {
            mockWebServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testGetResponseHandlerWhenSuccess() {
        doReturn(context).when(setSubscriptionAttributeResponseHandler).getContext();
        doReturn(sharedPreferences).when(setSubscriptionAttributeResponseHandler).getSharedPreferences(context);
        when(sharedPreferences.edit()).thenReturn(editor);

        Map<String, Object> subscriptionAttributes = new HashMap<>();
        subscriptionAttributes.put("attributeId", "value");

        HttpUrl baseUrl = mockWebServer.url("/subscription/attribute");
        CleverPushHttpClient.BASE_URL = baseUrl.toString().replace("/subscription/attribute", "");
        MockResponse mockResponse = new MockResponse().setBody("{}").setResponseCode(200);
        mockWebServer.enqueue(mockResponse);

        cleverPushHttpClient.get("/subscription/attribute", setSubscriptionAttributeResponseHandler.getResponseHandler(subscriptionAttributes));

        try {
            sleep(600);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        verify(editor).remove(CleverPushPreferences.SUBSCRIPTION_ATTRIBUTES);
        verify(editor).apply();
        JSONObject jsonObject = new JSONObject(subscriptionAttributes);
        String jsonString = jsonObject.toString();
        verify(editor).putString(CleverPushPreferences.SUBSCRIPTION_ATTRIBUTES, jsonString);
        verify(editor).apply();
    }

    @Test
    void testGetResponseHandlerWhenFailure() {
        when(setSubscriptionAttributeResponseHandler.getLogger()).thenReturn(logger);

        HttpUrl baseUrl = mockWebServer.url("/subscription/attribute");
        CleverPushHttpClient.BASE_URL = baseUrl.toString().replace("/subscription/attribute","");
        MockResponse mockResponse = new MockResponse().setBody("{}").setResponseCode(400);
        mockWebServer.enqueue(mockResponse);

        cleverPushHttpClient.get( "/subscription/attribute", setSubscriptionAttributeResponseHandler.getResponseHandler(anyMap()));

        try {
            sleep(600);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        verify(logger).e("CleverPush", "Error setting attribute - HTTP " + 400);
    }

    @AfterEach
    void tearDown() {
        try {
            mockWebServer.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
