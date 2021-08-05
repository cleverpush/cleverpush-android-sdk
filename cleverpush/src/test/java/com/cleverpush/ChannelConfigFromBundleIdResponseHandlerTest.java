package com.cleverpush;


import android.content.Context;
import android.content.SharedPreferences;

import com.cleverpush.responsehandlers.ChannelConfigFromBundleIdResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChannelConfigFromBundleIdResponseHandlerTest {

    private MockWebServer mockWebServer;


    @Mock
    private CleverPush cleverPush;

    private ChannelConfigFromBundleIdResponseHandler channelConfigFromBundleIdResponseHandler;

    @Mock
    Context context;

    @Mock
    SharedPreferences sharedPreferences;

    @Mock
    SharedPreferences.Editor editor;


    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mockWebServer = new MockWebServer();
        /*cleverPush = Mockito.spy(new CleverPush(context));
        channelConfigFromBundleIdResponseHandler = Mockito.spy(new ChannelConfigFromBundleIdResponseHandler(cleverPush));*/
    }

    @Test
     void testGetChannelConfigFromChannelIdSuccessResponse() {
        try {
            mockWebServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        HttpUrl baseUrl = mockWebServer.url("/v1/chat/");
        mockWebServer.enqueue(new MockResponse().setBody(new MockResponseFileReader("test1.json").getContent()).setResponseCode(200));
        doReturn(context).when(cleverPush).getContext();
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        doReturn("taWutEzzBZM8za3PS").when(cleverPush).getChannelId(context);
        when(sharedPreferences.edit()).thenReturn(editor);
        when(editor.putString(CleverPushPreferences.CHANNEL_ID, "channelId")).thenReturn(editor);

        CleverPushHttpClient.get( baseUrl.url().toString(),   new ChannelConfigFromBundleIdResponseHandler(cleverPush).getResponseHandler(true));
        verify(cleverPush).setInitialized(true);
        verify(cleverPush).setChannelConfig(any());
        verify(cleverPush).setChannelId("taWutEzzBZM8za3PS");
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