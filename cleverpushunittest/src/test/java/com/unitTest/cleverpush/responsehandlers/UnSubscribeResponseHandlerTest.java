package com.unitTest.cleverpush.responsehandlers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static java.lang.Thread.sleep;

import android.content.Context;
import android.content.SharedPreferences;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.listener.UnsubscribedListener;
import com.cleverpush.responsehandlers.UnsubscribeResponseHandler;
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

class UnSubscribeResponseHandlerTest {

    private CleverPush cleverPush;
    private UnsubscribeResponseHandler unSubscribeResponseHandler;
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
        cleverPush = Mockito.spy(new CleverPush(context));
        unSubscribeResponseHandler = Mockito.spy(new UnsubscribeResponseHandler(cleverPush, new UnsubscribedListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(Throwable throwable) {

            }
        }));
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
        CleverPushHttpClient.BASE_URL = baseUrl.toString().replace("/subscription/unsubscribe", "");
        MockResponse mockResponse = new MockResponse().setBody("{}").setResponseCode(200);
        mockWebServer.enqueue(mockResponse);

        cleverPushHttpClient.get("/subscription/unsubscribe", unSubscribeResponseHandler.getResponseHandler());

        try {
            sleep(600);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        verify(cleverPush).clearSubscriptionData();
        Logger.d("CleverPush", "unsubscribe success");
    }

    @Test
    void testGetResponseHandlerWhenSuccessWhenThereIsExcepton() {
        HttpUrl baseUrl = mockWebServer.url("/subscription/unsubscribe");
        CleverPushHttpClient.BASE_URL = baseUrl.toString().replace("/subscription/unsubscribe", "");
        MockResponse mockResponse = new MockResponse().setBody("{}").setResponseCode(200);
        mockWebServer.enqueue(mockResponse);

        cleverPushHttpClient.get("/subscription/unsubscribe", unSubscribeResponseHandler.getResponseHandler());

        try {
            sleep(600);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Logger.e(anyString(), anyString(), any());
    }

    @Test
    void testGetResponseHandlerWhenFailure() {

        HttpUrl baseUrl = mockWebServer.url("/subscription/untag");
        CleverPushHttpClient.BASE_URL = baseUrl.toString().replace("/subscription/untag", "");
        MockResponse mockResponse = new MockResponse().setBody("{}").setResponseCode(400);
        mockWebServer.enqueue(mockResponse);

        cleverPushHttpClient.get("/subscription/unsubscribe", unSubscribeResponseHandler.getResponseHandler());

        try {
            sleep(600);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Logger.e("CleverPush", "Failed while unsubscribe request - " + 400 + " - {}", null);
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
