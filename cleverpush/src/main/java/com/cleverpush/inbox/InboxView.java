package com.cleverpush.inbox;

import static com.cleverpush.Constants.LOG_TAG;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cleverpush.ActivityLifecycleListener;
import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.Notification;
import com.cleverpush.NotificationOpenedResult;
import com.cleverpush.R;
import com.cleverpush.inbox.listener.OnItemClickListener;
import com.cleverpush.listener.InitializeListener;
import com.cleverpush.listener.NotificationClickListener;
import com.cleverpush.listener.NotificationsCallbackListener;
import com.cleverpush.util.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Set;

public class InboxView extends LinearLayout {

  private static final String TAG = "CleverPush/InboxView";
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
    progressBar.setLayoutParams(
        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

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
    try {
      getCleverPushInstance().getNotifications(combineWithApi, new NotificationsCallbackListener() {
        @Override
        public void ready(Set<Notification> notifications) {
          notificationArrayList.addAll(notifications);
          new Handler(Looper.getMainLooper()).post(() -> setupInboxView(notificationArrayList));
        }
      });
    } catch (Exception e) {
      Logger.e(TAG, "Error in InboxView getNotifications.", e);
    }
  }

  public void setupInboxView(ArrayList<Notification> notifications) {
    View view = LayoutInflater.from(context).inflate(R.layout.inbox_view, this, true);
    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
    recyclerView = view.findViewById(R.id.rvNotifications);
    inboxViewListAdapter = new InboxViewListAdapter(context, notifications, typedArray, getOnItemClickListener(notificationArrayList, recyclerView));
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

  private OnItemClickListener getOnItemClickListener(ArrayList<Notification> notificationArrayList, RecyclerView recyclerView) {
    return position -> {
      try {
        if (notificationClickListener != null) {
          Notification clickedNotification = notificationArrayList.get(position);

          if (clickedNotification.getInboxAppBanner() != null) {
            InboxDetailActivity.launch(ActivityLifecycleListener.currentActivity, notificationArrayList, position);
          }
          notificationClickListener.onClicked(notificationArrayList.get(position));
        } else if (getCleverPushInstance().getNotificationOpenedListener() != null) {
          NotificationOpenedResult notificationOpenedResult = new NotificationOpenedResult();
          notificationOpenedResult.setNotification(notificationArrayList.get(position));
          getCleverPushInstance().getNotificationOpenedListener().notificationOpened(notificationOpenedResult);
        }

        trackInboxNotificationClick(notificationArrayList.get(position).getId());
        notificationArrayList.get(position).setRead(true);
        inboxViewListAdapter.notifyItemChanged(position, notificationArrayList.get(position));
        recyclerView.smoothScrollToPosition(position);
      } catch (Exception e) {
        Logger.e(TAG, "Error in InboxView's OnItemClickListener.", e);
      }
    };
  }

  private void trackInboxNotificationClick(String notificationId) {
    String channelId = getCleverPushInstance().getChannelId(context);
    if (channelId == null) {
      Logger.w(LOG_TAG, "Channel ID is null. Skipping inbox notification click tracking.");
      return;
    }

    JSONObject jsonBody = new JSONObject();
    try {
      jsonBody.put("channelId", channelId);
      jsonBody.put("notificationId", notificationId);
    } catch (JSONException e) {
      Logger.e(LOG_TAG, "Error creating JSON for inbox notification click tracking request.", e);
      return;
    }

    String inboxViewClickPath = "/channel/" + channelId + "/panel/clicked";
    CleverPushHttpClient.postWithRetry(inboxViewClickPath, jsonBody, new CleverPushHttpClient.ResponseHandler() {
      @Override
      public void onSuccess(String response) {
        Logger.d(LOG_TAG, "Successfully tracked inbox notification click");
      }

      @Override
      public void onFailure(int statusCode, String response, Throwable throwable) {
        Logger.e(LOG_TAG, "Failed to track inbox notification click." +
                "\nStatus code: " + statusCode +
                "\nResponse: " + response +
                (throwable != null ? ("\nError: " + throwable.getMessage()) : ""));
      }
    });
  }
}
