package com.cleverpush.inbox;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cleverpush.CleverPush;
import com.cleverpush.Notification;
import com.cleverpush.NotificationOpenedResult;
import com.cleverpush.R;
import com.cleverpush.listener.InitializeListener;
import com.cleverpush.listener.NotificationClickListener;
import com.cleverpush.listener.NotificationsCallbackListener;
import com.cleverpush.stories.listener.OnItemClickListener;

import java.util.ArrayList;
import java.util.Set;

public class InboxView extends LinearLayout implements OnItemClickListener {

    private TypedArray typedArray;
    private InboxViewListAdapter inboxViewListAdapter;
    private NotificationClickListener notificationClickListener;

    private Context context;
    private ArrayList<Notification> notificationArrayList = new ArrayList<>();

    private ProgressBar progressBar;
    private RecyclerView recyclerView;

    public InboxView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.context = context;
        typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.InboxView);

        progressBar = new ProgressBar(context);
        progressBar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        this.setGravity(Gravity.CENTER);
        this.addView(progressBar);

        getInitializeListener(context);
    }

    public void getInitializeListener(Context context) {
        getCleverPushInstance().setInitializeListener(new InitializeListener() {
            @Override
            public void onInitialized() {
                if (getTypedArray().getBoolean(R.styleable.InboxView_combine_with_api, false)) {
                    getNotifications(true);
                } else {
                    getNotifications(false);
                }
            }
        });
    }

    void getNotifications(boolean combineWithApi) {
        getCleverPushInstance().getNotifications(combineWithApi, new NotificationsCallbackListener() {
            @Override
            public void ready(Set<Notification> notifications) {
                notificationArrayList.addAll(notifications);
                getCleverPushInstance().getCurrentActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setupInboxView(notificationArrayList);
                    }
                });
            }
        });
    }

    public void setupInboxView(ArrayList<Notification> notifications) {
        View view = LayoutInflater.from(context).inflate(R.layout.inbox_view, this, true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        recyclerView = view.findViewById(R.id.rvNotifications);
        inboxViewListAdapter = new InboxViewListAdapter(context, notifications, typedArray, this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(inboxViewListAdapter);
        progressBar.setVisibility(GONE);
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

    public ArrayList<Notification> getNotificationArrayList() {
        return notificationArrayList;
    }

    public TypedArray getTypedArray() {
        return typedArray;
    }

    @Override
    public void onClicked(int position) {
        if (notificationClickListener != null) {
            notificationClickListener.onClicked(notificationArrayList.get(position));
        } else if (getCleverPushInstance().getNotificationOpenedListener() != null) {
            NotificationOpenedResult notificationOpenedResult = new NotificationOpenedResult();
            notificationOpenedResult.setNotification(notificationArrayList.get(position));
            getCleverPushInstance().getNotificationOpenedListener().notificationOpened(notificationOpenedResult, new Activity());
        }

        notificationArrayList.get(position).setRead(true);
        inboxViewListAdapter.notifyItemChanged(position, notificationArrayList.get(position));
        recyclerView.smoothScrollToPosition(position);
    }
}
