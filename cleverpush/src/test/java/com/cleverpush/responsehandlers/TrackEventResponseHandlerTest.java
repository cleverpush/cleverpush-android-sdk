package com.cleverpush.responsehandlers;

import android.content.Context;
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.validateMockitoUsage;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class TrackEventResponseHandlerTest {

    private TrackEventResponseHandler trackEventResponseHandler;
    private MockWebServer mockWebServer;

    @Mock
    CleverPushHttpClient cleverPushHttpClient;

    @Mock
    Log log;

    @Mock
    CleverPushHttpClient.ResponseHandler responseHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockWebServer = new MockWebServer();
        trackEventResponseHandler = Mockito.spy(new TrackEventResponseHandler());
        try {
            mockWebServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    @Test
//    void testGetResponseHandlerWhenSuccess() {
//        HttpUrl baseUrl = mockWebServer.url("/subscription/conversion");
//        CleverPushHttpClient.BASE_URL = baseUrl.toString().replace("/subscription/conversion","");
//        MockResponse mockResponse = new MockResponse().setBody("{}").setResponseCode(200);
//        mockWebServer.enqueue(mockResponse);
//
//        cleverPushHttpClient.get( "/subscription/conversion", trackEventResponseHandler.getResponseHandler("eventName"));
//        try {
//            sleep(200);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        verify(trackEventResponseHandler.getResponseHandler("eventName")).onSuccess("{}");
//    }
//
//    @Test
//    void testGetResponseHandlerWhenFailure() {
//        HttpUrl baseUrl = mockWebServer.url("/subscription/conversion");
//        CleverPushHttpClient.BASE_URL = baseUrl.toString().replace("/subscription/conversion","");
//        MockResponse mockResponse = new MockResponse().setBody("{}").setResponseCode(400);
//        mockWebServer.enqueue(mockResponse);
//
//
//        cleverPushHttpClient.get( "/subscription/conversion", trackEventResponseHandler.getResponseHandler("eventName"));
//
//        try {
//            sleep(60000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        verify(log).e("CleverPush", "Error tracking event - HTTP " + 400);
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