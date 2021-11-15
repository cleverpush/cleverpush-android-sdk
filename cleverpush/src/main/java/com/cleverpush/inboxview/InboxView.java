package com.cleverpush.inboxview;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cleverpush.CleverPush;
import com.cleverpush.Notification;
import com.cleverpush.R;
import com.cleverpush.listener.InitializeListener;
import com.cleverpush.listener.NotificationClickListener;
import com.cleverpush.listener.NotificationsCallbackListener;

import java.util.ArrayList;
import java.util.Set;

public class InboxView extends LinearLayout {

    private CleverPush cleverPush;
    private TypedArray typedArray;
    private InboxViewListAdapter inboxViewListAdapter;
    private NotificationClickListener notificationClickListener;

    private Context context;
    private ArrayList<Notification> notificationArrayList = new ArrayList<>();

    public InboxView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.context = context;
        typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.InboxView);
        cleverPush = getCleverPushInstance();

        cleverPush.setInitializeListener(new InitializeListener() {
            @Override
            public void onInitialized() {
                if (typedArray.getBoolean(R.styleable.InboxView_combine_with_api, false)) {
                    getNotifications((Activity) context, true);
                } else {
                    getNotifications((Activity) context, false);
                }
            }
        });
    }

    void getNotifications(Activity activity, boolean combineWithApi) {
        cleverPush.getNotifications(combineWithApi, new NotificationsCallbackListener() {
            @Override
            public void ready(Set<Notification> notifications) {
                notificationArrayList.addAll(notifications);
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setupInboxView(notificationArrayList);
                    }
                });
            }
        });
    }

    private void setupInboxView(ArrayList<Notification> notifications) {
        View view = LayoutInflater.from(context).inflate(R.layout.inbox_view, this, true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        RecyclerView recyclerView = view.findViewById(R.id.rvNotifications);
        inboxViewListAdapter = new InboxViewListAdapter(context, notifications, typedArray, notificationClickListener, cleverPush.getNotificationOpenedListener());
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(inboxViewListAdapter);
    }

    public void setNotificationClickListener(NotificationClickListener notificationClickListener) {
        this.notificationClickListener = notificationClickListener;
    }

    public CleverPush getCleverPushInstance() {
        if (context != null) {
            return CleverPush.getInstance(context);
        }
        return null;
    }
}
