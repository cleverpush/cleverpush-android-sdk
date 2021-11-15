package com.cleverpush.inboxview;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.Notification;
import com.cleverpush.R;
import com.cleverpush.chat.ChatView;
import com.cleverpush.listener.ChannelConfigListener;
import com.cleverpush.listener.InitializeListener;
import com.cleverpush.listener.NotificationClickListener;
import com.cleverpush.listener.NotificationsCallbackListener;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.HashSet;
import java.util.Set;

class InboxViewTest {

    @Mock
    private Context context;



    @Mock
    AttributeSet attributeSet;

    @Mock
    private TypedArray typedArray;

    @Mock
    private Activity activity;

    @Mock
    SharedPreferences sharedPreferences;

    private CleverPush cleverPush;
    private InboxView inboxView;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        cleverPush = Mockito.spy(new CleverPush(context));
        inboxView = spy(new InboxView(context, attributeSet));
    }

    @Test
    public void constructorWhenCombineWithApi() {
        inboxView = spy(new InboxView(context, attributeSet));

        Answer<Void> initializeListenerANswer = new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                InitializeListener callback = (InitializeListener) invocation.getArguments()[0];
                callback.onInitialized();
                return null;
            }
        };
        doAnswer(initializeListenerANswer).when(cleverPush).setInitializeListener(any(InitializeListener.class));

        when(context.obtainStyledAttributes(attributeSet, R.styleable.InboxView)).thenReturn(typedArray);
        when(typedArray.getBoolean(R.styleable.InboxView_combine_with_api, false)).thenReturn(true);
        doReturn(cleverPush).when(inboxView).getCleverPushInstance();

        //verify(inboxView).getNotifications(activity,true);
    }

    @Test
    public void getNotifications() {
        Set<Notification> notifications = new HashSet<>();

        Answer<Void> notificationsCallbackListenerAnswer = new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                NotificationsCallbackListener callback = (NotificationsCallbackListener) invocation.getArguments()[0];

                callback.ready(notifications);
                return null;
            }
        };

        when(context.obtainStyledAttributes(attributeSet, R.styleable.InboxView)).thenReturn(typedArray);
        when(typedArray.getBoolean(R.styleable.InboxView_combine_with_api, false)).thenReturn(true);
        doReturn(cleverPush).when(inboxView).getCleverPushInstance();
        doReturn(context).when(cleverPush).getContext();
        doReturn(sharedPreferences).when(cleverPush).getSharedPreferences(context);
        when(sharedPreferences.getString(CleverPushPreferences.NOTIFICATIONS_JSON, null)).thenReturn("[{\"actions\":[],\"createdAt\":\"2021-08-18T12:59:51.840Z\",\"_id\":\"RfS4GzvnjzwXa9cqr\",\"rawPayload\":\"{\\\"createdAt\\\":\\\"2021-08-18T12:59:51.840Z\\\",\\\"mediaUrl\\\":null,\\\"_id\\\":\\\"RfS4GzvnjzwXa9cqr\\\",\\\"iconUrl\\\":null,\\\"tag\\\":\\\"RfS4GzvnjzwXa9cqr\\\",\\\"text\\\":\\\"test\\\",\\\"title\\\":\\\"test3\\\",\\\"actions\\\":[],\\\"expiresAt\\\":null,\\\"url\\\":\\\"https:\\\\/\\\\/app.cleverpush.com\\\\/en\\\\/app\\\\/create-new?utm_source\\u003dbrowser\\u0026utm_medium\\u003dpush-notification\\u0026utm_campaign\\u003dcleverpush-1629291566\\\"}\",\"tag\":\"RfS4GzvnjzwXa9cqr\",\"text\":\"test\",\"title\":\"test3\",\"url\":\"https://app.cleverpush.com/en/app/create-new?utm_source\\u003dbrowser\\u0026utm_medium\\u003dpush-notification\\u0026utm_campaign\\u003dcleverpush-1629291566\"}]");
        when(sharedPreferences.getStringSet(CleverPushPreferences.NOTIFICATIONS, new HashSet<>())).thenReturn(null);
        doAnswer(notificationsCallbackListenerAnswer).when(cleverPush).getNotifications(anyBoolean(), any(NotificationsCallbackListener.class));

        inboxView.getNotifications(activity, true);

        //verify(inboxView).getNotifications(activity,true);
    }

}