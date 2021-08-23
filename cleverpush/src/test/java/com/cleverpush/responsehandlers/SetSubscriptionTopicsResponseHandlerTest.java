package com.cleverpush.responsehandlers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.listener.CompletionListener;
import com.cleverpush.listener.TopicsChangedListener;

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

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SetSubscriptionTopicsResponseHandlerTest {

    private CleverPush cleverPush;
    private SetSubscriptionTopicsResponseHandler setSubscriptionTopicsResponseHandler;
    private MockWebServer mockWebServer;


    @Mock
    CleverPushHttpClient cleverPushHttpClient;

    @Mock
    CompletionListener completionListener;

    @Mock
    TopicsChangedListener topicsChangedListener;

    @Mock
    Log log;

    @Mock
    Context context;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockWebServer = new MockWebServer();
        cleverPush = Mockito.spy(new CleverPush(context));
        setSubscriptionTopicsResponseHandler = Mockito.spy(new SetSubscriptionTopicsResponseHandler(cleverPush));
        try {
            mockWebServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testGetResponseHandlerWhenSuccessWhenThereIsNoTopicsChangedListener() {

        doReturn(null).when(cleverPush).getTopicsChangedListener();

        HttpUrl baseUrl = mockWebServer.url("/subscription/sync/");
        CleverPushHttpClient.BASE_URL = baseUrl.toString().replace("/subscription/sync/","");
        MockResponse mockResponse = new MockResponse().setBody("{}").setResponseCode(200);
        mockWebServer.enqueue(mockResponse);

        String[] topicIds = {"topicId"};
        cleverPushHttpClient.get( "/subscription/sync/", setSubscriptionTopicsResponseHandler.getResponseHandler(topicIds, completionListener));
        try {
            sleep(600);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        verify(completionListener).onComplete();
        verify(topicsChangedListener, never()).changed(new HashSet<>(Arrays.asList(topicIds)));
    }

    @Test
    void testGetResponseHandlerWhenSuccessWhenThereIsNoCompletionListener() {

        doReturn(topicsChangedListener).when(cleverPush).getTopicsChangedListener();

        HttpUrl baseUrl = mockWebServer.url("/subscription/sync/");
        CleverPushHttpClient.BASE_URL = baseUrl.toString().replace("/subscription/sync/","");
        MockResponse mockResponse = new MockResponse().setBody("{}").setResponseCode(200);
        mockWebServer.enqueue(mockResponse);

        String[] topicIds = {"topicId"};
        cleverPushHttpClient.get( "/subscription/sync/", setSubscriptionTopicsResponseHandler.getResponseHandler(topicIds, null));
        try {
            sleep(600);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        verify(completionListener, never()).onComplete();
        verify(topicsChangedListener).changed(new HashSet<>(Arrays.asList(topicIds)));
    }

//    @Test
//    void testGetResponseHandlerWhenFailure() {
//        HttpUrl baseUrl = mockWebServer.url("/subscription/sync/");
//        CleverPushHttpClient.BASE_URL = baseUrl.toString().replace("/subscription/sync/","");
//        MockResponse mockResponse = new MockResponse().setBody("{}").setResponseCode(400);
//        mockWebServer.enqueue(mockResponse);
//
//        String[] topicIds = {"topicId"};
//        cleverPushHttpClient.get( "/subscription/sync/", setSubscriptionTopicsResponseHandler.getResponseHandler(topicIds, null));
//
//        try {
//            sleep(600);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        verify(log).e("CleverPush", "Error setting topics - HTTP " + 400 + "{}");
//    }

    @AfterEach
    void tearDown() {
        try {
            mockWebServer.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}