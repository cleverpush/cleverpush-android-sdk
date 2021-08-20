package com.cleverpush.responsehandlers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.listener.RemoveTagCompletedListener;

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

class UnSubscribeResponseHandlerTest {

    private CleverPush cleverPush;
    private UnSubscribeResponseHandler unSubscribeResponseHandler;
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
    Log log;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockWebServer = new MockWebServer();
        cleverPush = Mockito.spy(new CleverPush(context));
        unSubscribeResponseHandler = Mockito.spy(new UnSubscribeResponseHandler(cleverPush));
        try {
            mockWebServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testGetResponseHandlerWhenSuccessWhenThereIsNoExcepton() {
        doReturn(context).when(cleverPush).getContext();
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        when(sharedPreferences.edit()).thenReturn(editor);

        HttpUrl baseUrl = mockWebServer.url("/subscription/unsubscribe");
        CleverPushHttpClient.BASE_URL = baseUrl.toString().replace("/subscription/unsubscribe","");
        MockResponse mockResponse = new MockResponse().setBody("{}").setResponseCode(200);
        mockWebServer.enqueue(mockResponse);

        cleverPushHttpClient.get( "/subscription/unsubscribe", unSubscribeResponseHandler.getResponseHandler());
        try {
            sleep(600);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        verify(cleverPush).clearSubscriptionData();
        verify(log).d("CleverPush", "unsubscribe success");
    }

    @Test
    void testGetResponseHandlerWhenSuccessWhenThereIsExcepton() {
        HttpUrl baseUrl = mockWebServer.url("/subscription/unsubscribe");
        CleverPushHttpClient.BASE_URL = baseUrl.toString().replace("/subscription/unsubscribe","");
        MockResponse mockResponse = new MockResponse().setBody("{}").setResponseCode(200);
        mockWebServer.enqueue(mockResponse);

        cleverPushHttpClient.get( "/subscription/unsubscribe", unSubscribeResponseHandler.getResponseHandler());
        try {
            sleep(600);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        verify(log).e("CleverPush", "Error, java.lang.NullPointerException");
    }

    @Test
    void testGetResponseHandlerWhenFailure() {
        HttpUrl baseUrl = mockWebServer.url("/subscription/untag");
        CleverPushHttpClient.BASE_URL = baseUrl.toString().replace("/subscription/untag","");
        MockResponse mockResponse = new MockResponse().setBody("{}").setResponseCode(400);
        mockWebServer.enqueue(mockResponse);


        cleverPushHttpClient.get( "/subscription/unsubscribe", unSubscribeResponseHandler.getResponseHandler());

        try {
            sleep(600);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        verify(log).e("CleverPush", "Failed while unsubscribe request - " + 400);
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