package com.unitTest.cleverpush.responsehandlers;


import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static java.lang.Thread.sleep;

import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.responsehandlers.TrackEventResponseHandler;
import com.cleverpush.util.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;


class TrackEventResponseHandlerTest {

    private TrackEventResponseHandler trackEventResponseHandler;
    private MockWebServer mockWebServer;

    @Mock
    CleverPushHttpClient cleverPushHttpClient;

    @Mock
    Logger logger;

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

    @Test
    void testGetResponseHandlerWhenSuccess() {
        Logger.e("LOG_TAG", "trackEventResponseHandler: " + when(trackEventResponseHandler));

        HttpUrl baseUrl = mockWebServer.url("/subscription/conversion");
        CleverPushHttpClient.BASE_URL = baseUrl.toString().replace("/subscription/conversion","");
        MockResponse mockResponse = new MockResponse().setBody("{}").setResponseCode(200);
        mockWebServer.enqueue(mockResponse);

        cleverPushHttpClient.get( "/subscription/conversion", trackEventResponseHandler.getResponseHandler("eventName"));

        try {
            sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        verify(logger).d("CleverPush", "Event successfully tracked: eventName");
    }

    @Test
    void testGetResponseHandlerWhenFailure() {
        Logger.e("LOG_TAG", "trackEventResponseHandler: " + when(trackEventResponseHandler));

        HttpUrl baseUrl = mockWebServer.url("/subscription/conversion");
        CleverPushHttpClient.BASE_URL = baseUrl.toString().replace("/subscription/conversion","");
        MockResponse mockResponse = new MockResponse().setBody("{}").setResponseCode(400);
        mockWebServer.enqueue(mockResponse);

        cleverPushHttpClient.get( "/subscription/conversion", trackEventResponseHandler.getResponseHandler("eventName"));

        try {
            sleep(600);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        verify(logger).e("CleverPush", "Error tracking event - HTTP " + 400);
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
