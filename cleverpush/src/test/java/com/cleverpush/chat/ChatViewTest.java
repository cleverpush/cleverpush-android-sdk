package com.cleverpush.chat;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.cleverpush.CleverPush;
import com.cleverpush.listener.ChannelConfigListener;
import com.cleverpush.responsehandlers.TrackEventResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static com.google.common.truth.Truth.assertThat;
import static java.lang.Thread.sleep;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

class ChatViewTest {

    @Mock
    private Context context;

    @Mock
    ChatJavascriptInterface chatJavascriptInterface;

    @Mock
    WebSettings webSettings;

    @Mock
    WebViewClient webViewClient;

    @Mock
    CleverPush cleverPush;

    @Mock
    Handler handler;

    private ChatView chatView;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        chatView = spy(new ChatView(context));
    }


    @Test
    void testInit() {
        //doReturn(webView).when(chatView).getWebView();
        doReturn(context).when(chatView).getContext();
        doReturn(webSettings).when(chatView).getSettings();
        doReturn(chatJavascriptInterface).when(chatView).getChatJavascriptInterface(context);
        doReturn(webViewClient).when(chatView).getWebViewClient(context);

        chatView.init();

        verify(chatView).loadUrl("about:blank");
        verify(chatView).addJavascriptInterface(chatJavascriptInterface, "cleverpushAppInterface");
        verify(chatView).setWebViewClient(webViewClient);
        verify(chatView).loadChat();
    }

    @Test
    void testLoadChat() {
        doReturn(cleverPush).when(chatView).getCleverPushInstance();
        doReturn(context).when(chatView).getContext();
        doReturn(handler).when(chatView).getHandler();
        Answer<Void> channelConfigListenerAnswer = new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                ChannelConfigListener callback = (ChannelConfigListener) invocation.getArguments()[0];
                try {
                    JSONObject responseJson = new JSONObject("{ \"trackAppStatistics\": true}");
                    callback.ready(responseJson);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                return null;
            }
        };

        doAnswer(channelConfigListenerAnswer).when(cleverPush).getChannelConfig(any(ChannelConfigListener.class));
        when(handler.post(any(Runnable.class))).thenAnswer((Answer) invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        });

        // Run the test
        chatView.loadChat("subscriptionId");
        try {
            sleep(600);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        verify(chatView).loadDataWithBaseURL(anyString(), anyString(), anyString(), anyString(), any());
    }
}
