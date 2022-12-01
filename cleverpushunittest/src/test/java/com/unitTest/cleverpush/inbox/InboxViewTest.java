package com.unitTest.cleverpush.inbox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;

import com.cleverpush.CleverPush;
import com.cleverpush.Notification;
import com.cleverpush.NotificationList;
import com.cleverpush.inbox.InboxView;
import com.cleverpush.listener.InitializeListener;
import com.cleverpush.listener.NotificationsCallbackListener;
import com.google.gson.Gson;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

    private CleverPush cleverPush;
    private InboxView inboxView;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        cleverPush = Mockito.spy(new CleverPush(context));
//        inboxView = spy(new InboxView(context, attributeSet));
    }

    @Test
    public void getInitializeListenerWhenCombineWithApi() {
        Set<Notification> notifications = generateNotificationData();
        ArrayList<Notification> notificationArrayList = new ArrayList<Notification>(notifications);

//        doReturn(cleverPush).when(inboxView).getCleverPushInstance();
        doReturn(activity).when(cleverPush).getCurrentActivity();
//        doReturn(typedArray).when(inboxView).getTypedArray();
//        when(typedArray.getBoolean(R.styleable.InboxView_combine_with_api, false)).thenReturn(true);
//        doNothing().when(inboxView).getNotifications(true);
//        doNothing().when(inboxView).setupInboxView(notificationArrayList);

        Answer<Void> notificationsCallbackListenerAnswer = new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                NotificationsCallbackListener callback = (NotificationsCallbackListener) invocation.getArguments()[1];

                callback.ready(notifications);
                return null;
            }
        };

        Answer<Void> runOnUiThreadAnswer = new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                Runnable callback = (Runnable) invocation.getArguments()[0];
                callback.run();
                return null;
            }
        };

        Answer<Void> initializeListenerAnswer = new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                InitializeListener callback = (InitializeListener) invocation.getArguments()[0];
                callback.onInitialized();
                return null;
            }
        };

        doAnswer(notificationsCallbackListenerAnswer).when(cleverPush).getNotifications(anyBoolean(), any(NotificationsCallbackListener.class));
        doAnswer(runOnUiThreadAnswer).when(activity).runOnUiThread(any(Runnable.class));
        doAnswer(initializeListenerAnswer).when(cleverPush).setInitializeListener(any(InitializeListener.class));

//        inboxView.getInitializeListener(context);

//        verify(inboxView).getNotifications(true);
    }

    @Test
    public void getInitializeListenerWhenWithoutCombineWithApi() {
        Set<Notification> notifications = generateNotificationData();
        ArrayList<Notification> notificationArrayList = new ArrayList<Notification>(notifications);

//        doReturn(cleverPush).when(inboxView).getCleverPushInstance();
        doReturn(activity).when(cleverPush).getCurrentActivity();
//        doReturn(typedArray).when(inboxView).getTypedArray();
//        when(typedArray.getBoolean(R.styleable.InboxView_combine_with_api, false)).thenReturn(false);
//        doNothing().when(inboxView).getNotifications(false);
//        doNothing().when(inboxView).setupInboxView(notificationArrayList);

        Answer<Void> notificationsCallbackListenerAnswer = new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                NotificationsCallbackListener callback = (NotificationsCallbackListener) invocation.getArguments()[1];

                callback.ready(notifications);
                return null;
            }
        };

        Answer<Void> runOnUiThreadAnswer = new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                Runnable callback = (Runnable) invocation.getArguments()[0];
                callback.run();
                return null;
            }
        };

        Answer<Void> initializeListenerAnswer = new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                InitializeListener callback = (InitializeListener) invocation.getArguments()[0];
                callback.onInitialized();
                return null;
            }
        };

        doAnswer(notificationsCallbackListenerAnswer).when(cleverPush).getNotifications(anyBoolean(), any(NotificationsCallbackListener.class));
        doAnswer(runOnUiThreadAnswer).when(activity).runOnUiThread(any(Runnable.class));
        doAnswer(initializeListenerAnswer).when(cleverPush).setInitializeListener(any(InitializeListener.class));

//        inboxView.getInitializeListener(context);

//        verify(inboxView).getNotifications(false);
    }

    @Test
    public void getNotifications() {
        Set<Notification> notifications = generateNotificationData();
        ArrayList<Notification> notificationArrayList = new ArrayList<Notification>(notifications);

//        doReturn(cleverPush).when(inboxView).getCleverPushInstance();
        doReturn(activity).when(cleverPush).getCurrentActivity();
//        doNothing().when(inboxView).setupInboxView(notificationArrayList);

        Answer<Void> notificationsCallbackListenerAnswer = new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                NotificationsCallbackListener callback = (NotificationsCallbackListener) invocation.getArguments()[1];

                callback.ready(notifications);
                return null;
            }
        };

        Answer<Void> runOnUiThreadAnswer = new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                Runnable callback = (Runnable) invocation.getArguments()[0];
                callback.run();
                return null;
            }
        };

        doAnswer(notificationsCallbackListenerAnswer).when(cleverPush).getNotifications(anyBoolean(), any(NotificationsCallbackListener.class));
        doAnswer(runOnUiThreadAnswer).when(activity).runOnUiThread(any(Runnable.class));

//        inboxView.getNotifications(true);

//        verify(inboxView).getNotifications(true);
//        assertThat(inboxView.getNotificationArrayList().size()).isEqualTo(notifications.size());
//        verify(inboxView).setupInboxView(notificationArrayList);
    }

    public Set<Notification> generateNotificationData() {
        Gson gson = new Gson();
        String notificationsJson = "[{\"actions\":[],\"createdAt\":\"2021-08-18T12:59:51.840Z\",\"_id\":\"RfS4GzvnjzwXa9cqr\",\"rawPayload\":\"{\\\"createdAt\\\":\\\"2021-08-18T12:59:51.840Z\\\",\\\"mediaUrl\\\":null,\\\"_id\\\":\\\"RfS4GzvnjzwXa9cqr\\\",\\\"iconUrl\\\":null,\\\"tag\\\":\\\"RfS4GzvnjzwXa9cqr\\\",\\\"text\\\":\\\"test\\\",\\\"title\\\":\\\"test3\\\",\\\"actions\\\":[],\\\"expiresAt\\\":null,\\\"url\\\":\\\"https:\\\\/\\\\/app.cleverpush.com\\\\/en\\\\/app\\\\/create-new?utm_source\\u003dbrowser\\u0026utm_medium\\u003dpush-notification\\u0026utm_campaign\\u003dcleverpush-1629291566\\\"}\",\"tag\":\"RfS4GzvnjzwXa9cqr\",\"text\":\"test\",\"title\":\"test3\",\"url\":\"https://app.cleverpush.com/en/app/create-new?utm_source\\u003dbrowser\\u0026utm_medium\\u003dpush-notification\\u0026utm_campaign\\u003dcleverpush-1629291566\"}]";
        if (notificationsJson != null) {
            try {
                List<Notification> notifications = gson.fromJson(notificationsJson, NotificationList.class);
                return new HashSet<>(notifications);
            } catch (Exception ex) {
                Log.e("CleverPush", "error while getting stored notifications", ex);
            }
        }
        return new HashSet<>();
    }
}
