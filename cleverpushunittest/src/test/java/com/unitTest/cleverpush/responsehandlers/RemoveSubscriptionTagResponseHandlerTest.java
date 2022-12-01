package com.unitTest.cleverpush.responsehandlers;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static java.lang.Thread.sleep;

import android.content.Context;
import android.content.SharedPreferences;

import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.listener.RemoveTagCompletedListener;
import com.cleverpush.responsehandlers.RemoveSubscriptionTagResponseHandler;
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

class RemoveSubscriptionTagResponseHandlerTest {

    private RemoveSubscriptionTagResponseHandler removeSubscriptionTagResponseHandler;
    private MockWebServer mockWebServer;

    @Mock
    RemoveTagCompletedListener removeTagCompletedListener;

    @Mock
    CleverPushHttpClient cleverPushHttpClient;

    @Mock
    Context context;

    @Mock
    SharedPreferences sharedPreferences;

    @Mock
    SharedPreferences.Editor editor;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mockWebServer = new MockWebServer();
        removeSubscriptionTagResponseHandler = Mockito.spy(new RemoveSubscriptionTagResponseHandler());
        try {
            mockWebServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testGetResponseHandlerWhenSuccessAndRemoveTagCompletedListenerIsNull() {
        doReturn(context).when(removeSubscriptionTagResponseHandler).getContext();
        doReturn(sharedPreferences).when(removeSubscriptionTagResponseHandler).getSharedPreferences(context);
        when(sharedPreferences.edit()).thenReturn(editor);

        HttpUrl baseUrl = mockWebServer.url("/subscription/untag");
        CleverPushHttpClient.BASE_URL = baseUrl.toString().replace("/subscription/untag", "");
        MockResponse mockResponse = new MockResponse().setBody("{}").setResponseCode(200);
        mockWebServer.enqueue(mockResponse);

        cleverPushHttpClient.get("/subscription/untag", removeSubscriptionTagResponseHandler.getResponseHandler("tagId", null, 0, new HashSet<>(Arrays.asList("value"))));

        try {
            sleep(600);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        verify(removeSubscriptionTagResponseHandler).updateSubscriptionTags(new HashSet<>(Arrays.asList("value")));
        verify(removeTagCompletedListener, never()).tagRemoved(0);
    }

    @Test
    void testGetResponseHandlerWhenSuccessAndRemoveTagCompletedListenerIsNotNull() {
        doReturn(context).when(removeSubscriptionTagResponseHandler).getContext();
        doReturn(sharedPreferences).when(removeSubscriptionTagResponseHandler).getSharedPreferences(context);
        when(sharedPreferences.edit()).thenReturn(editor);

        HttpUrl baseUrl = mockWebServer.url("/subscription/untag");
        CleverPushHttpClient.BASE_URL = baseUrl.toString().replace("/subscription/untag", "");
        MockResponse mockResponse = new MockResponse().setBody("{}").setResponseCode(200);
        mockWebServer.enqueue(mockResponse);

        cleverPushHttpClient.get("/subscription/untag", removeSubscriptionTagResponseHandler.getResponseHandler("tagId", removeTagCompletedListener, 0, new HashSet<>(Arrays.asList("value"))));

        try {
            sleep(600);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        verify(removeSubscriptionTagResponseHandler).updateSubscriptionTags(new HashSet<>(Arrays.asList("value")));
        verify(removeTagCompletedListener).tagRemoved(0);
    }

    @Test
    void testGetResponseHandlerWhenFailure() {

        HttpUrl baseUrl = mockWebServer.url("/subscription/untag");
        CleverPushHttpClient.BASE_URL = baseUrl.toString().replace("/subscription/untag", "");
        MockResponse mockResponse = new MockResponse().setBody("{}").setResponseCode(400);
        mockWebServer.enqueue(mockResponse);

        cleverPushHttpClient.get("/subscription/untag", removeSubscriptionTagResponseHandler.getResponseHandler("tagId", removeTagCompletedListener, 0, new HashSet<>(Arrays.asList("value"))));

        try {
            sleep(600);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Logger.e("CleverPush", "Error removing tag - HTTP " + 400);
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
