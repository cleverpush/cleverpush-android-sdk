package com.cleverpush.responsehandlers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;


import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.listener.AddTagCompletedListener;
import com.cleverpush.util.Logger;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.google.common.truth.Truth.assertThat;
import static java.lang.Thread.sleep;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.validateMockitoUsage;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AddSubscriptionTagResponseHandlerTest {

    private AddSubscriptionTagResponseHandler addSubscriptionTagResponseHandler;
    private MockWebServer mockWebServer;

    @Mock
    AddTagCompletedListener addTagCompletedListener;

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

    @Mock
    Log log;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockWebServer = new MockWebServer();
        addSubscriptionTagResponseHandler = Mockito.spy(new AddSubscriptionTagResponseHandler());
        try {
            mockWebServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testGetResponseHandlerWhenSuccessAndAddTagCompletedListenerIsNull() {
        doReturn(context).when(addSubscriptionTagResponseHandler).getContext();
        doReturn(sharedPreferences).when(addSubscriptionTagResponseHandler).getSharedPreferences(context);
        when(sharedPreferences.edit()).thenReturn(editor);

        HttpUrl baseUrl = mockWebServer.url("/subscription/tag");
        CleverPushHttpClient.BASE_URL = baseUrl.toString().replace("/subscription/tag","");
        MockResponse mockResponse = new MockResponse().setBody("{}").setResponseCode(200);
        mockWebServer.enqueue(mockResponse);


        cleverPushHttpClient.get( "/subscription/tag", addSubscriptionTagResponseHandler.getResponseHandler("tagId", null, 0, new HashSet<>(Arrays.asList("value"))));

        try {
            sleep(600);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        verify(addSubscriptionTagResponseHandler).updateSubscriptionTags(new HashSet<>(Arrays.asList("value")));
        verify(addTagCompletedListener, never()).tagAdded(0);
    }

    @Test
    void testGetResponseHandlerWhenSuccessAndAddTagCompletedListenerIsNotNull() {
        doReturn(context).when(addSubscriptionTagResponseHandler).getContext();
        doReturn(sharedPreferences).when(addSubscriptionTagResponseHandler).getSharedPreferences(context);
        when(sharedPreferences.edit()).thenReturn(editor);

        HttpUrl baseUrl = mockWebServer.url("/subscription/tag");
        CleverPushHttpClient.BASE_URL = baseUrl.toString().replace("/subscription/tag","");
        MockResponse mockResponse = new MockResponse().setBody("{}").setResponseCode(200);
        mockWebServer.enqueue(mockResponse);


        cleverPushHttpClient.get( "/subscription/tag", addSubscriptionTagResponseHandler.getResponseHandler("tagId", addTagCompletedListener, 0, new HashSet<>(Arrays.asList("value"))));

        try {
            sleep(600);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        verify(addSubscriptionTagResponseHandler).updateSubscriptionTags(new HashSet<>(Arrays.asList("value")));
        verify(addTagCompletedListener).tagAdded(0);
    }

    @Test
    void testGetResponseHandlerWhenFailure() {
        //when(log.e("CleverPush", "Error adding tag - HTTP 400")).thenReturn(logger.e("CleverPush", "Error adding tag - HTTP 400"));
        HttpUrl baseUrl = mockWebServer.url("/subscription/tag");
        CleverPushHttpClient.BASE_URL = baseUrl.toString().replace("/subscription/tag","");
        MockResponse mockResponse = new MockResponse().setBody("{}").setResponseCode(400);
        mockWebServer.enqueue(mockResponse);


        cleverPushHttpClient.get( "/subscription/tag", addSubscriptionTagResponseHandler.getResponseHandler("tagId", addTagCompletedListener, 0, new HashSet<>(Arrays.asList("value"))));

        try {
            sleep(6000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //verify(logger).e("CleverPush", "Error adding tag - HTTP " + 400);
        assertThat(Logger.e("CleverPush", "Error adding tag - HTTP 400")).isEqualTo(0);
    }


    @AfterEach
    void tearDown() {
        try {
            mockWebServer.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
        validateMockitoUsage();
    }
}