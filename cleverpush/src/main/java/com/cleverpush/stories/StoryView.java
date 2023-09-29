package com.cleverpush.stories;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cleverpush.ActivityLifecycleListener;
import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.R;
import com.cleverpush.listener.StoryViewOpenedListener;
import com.cleverpush.stories.listener.OnItemClickListener;
import com.cleverpush.stories.models.Story;
import com.cleverpush.stories.models.StoryListModel;
import com.cleverpush.util.Logger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;

public class StoryView extends LinearLayout {

  protected static final int DEFAULT_BACKGROUND_COLOR = Color.WHITE;
  private static final String TAG = "CleverPush/AppStories";

  private TypedArray attrArray;
  private StoryViewListAdapter storyViewListAdapter;

  private Context context;
  private boolean loading = false;
  private ArrayList<Story> stories = new ArrayList<>();
  private String widgetId = null;

  public StoryViewOpenedListener storyViewOpenedListener;

  public String getWidgetId() {
    return widgetId;
  }

  public void setWidgetId(String widgetId) {
    this.widgetId = widgetId;
    loadStory();
  }

  public void setOpenedListener(StoryViewOpenedListener storyViewOpenedListener) {
    this.storyViewOpenedListener = storyViewOpenedListener;
  }

  public StoryView(Context context, AttributeSet attributeSet) {
    super(context, attributeSet);
    this.context = context;
    attrArray = getContext().obtainStyledAttributes(attributeSet, R.styleable.StoryView);
    loadStory();
  }

  private void loadStory() {
    String attrWidgetId = attrArray.getString(R.styleable.StoryView_widget_id);
    if (attrWidgetId == null || attrWidgetId.equalsIgnoreCase("")) {
      widgetId = getWidgetId();
    } else {
      widgetId = attrWidgetId;
    }

    if (widgetId == null || widgetId.length() == 0) {
      return;
    }

    if (loading) {
      return;
    }
    loading = true;

    String storyPath = "/story-widget/" + widgetId + "/config";
    Logger.d(TAG, "Loading stories: " + storyPath);

    CleverPush.getInstance(this.context).getActivityLifecycleListener().setActivityInitializedListener(() -> {
      CleverPushHttpClient.get(storyPath, getResponseHandler());
    });
  }

  private CleverPushHttpClient.ResponseHandler getResponseHandler() {
    return new CleverPushHttpClient.ResponseHandler() {
      @Override
      public void onSuccess(String response) {
        try {
          loading = false;

          GsonBuilder gsonBuilder = new GsonBuilder();
          Gson gson = gsonBuilder.create();
          StoryListModel model = gson.fromJson(response, StoryListModel.class);
          stories.addAll(model.getStories());
          SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
          for (int i = 0; i < stories.size(); i++) {
            if (sharedPreferences.getString(CleverPushPreferences.APP_OPENED_STORIES, "")
                    .contains(stories.get(i).getId())) {
              stories.get(i).setOpened(true);
            } else {
              stories.get(i).setOpened(false);
            }
          }
          ActivityLifecycleListener.currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              displayStoryHead(stories);
            }
          });
        } catch (Exception e) {
          Logger.e(TAG, "getResponseHandler onSuccess Exception: " + e.getLocalizedMessage());
        }
      }

      @Override
      public void onFailure(int statusCode, String response, Throwable throwable) {
        loading = false;
        if (throwable != null) {
          Logger.e(TAG, "Something went wrong when loading stories." +
                  "\nStatus code: " + statusCode +
                  "\nResponse: " + response +
                  "\nError: " + throwable.getMessage()
          );
        } else {
          Logger.e(TAG, "Something went wrong when loading stories." +
                  "\nStatus code: " + statusCode +
                  "\nResponse: " + response
          );
        }
      }
    };
  }

  private void displayStoryHead(ArrayList<Story> stories) {
    try {
      View view = LayoutInflater.from(context).inflate(R.layout.story_view, this, true);

      RelativeLayout relativeLayout = view.findViewById(R.id.rlMain);
      relativeLayout.setBackgroundColor(
              attrArray.getColor(R.styleable.StoryView_background_color, DEFAULT_BACKGROUND_COLOR));
      ViewGroup.LayoutParams params = relativeLayout.getLayoutParams();
      params.height =
              (int) attrArray.getDimension(R.styleable.StoryView_story_view_height, ViewGroup.LayoutParams.WRAP_CONTENT);
      params.width =
              (int) attrArray.getDimension(R.styleable.StoryView_story_view_width, ViewGroup.LayoutParams.WRAP_CONTENT);
      relativeLayout.setLayoutParams(params);

      LinearLayoutManager linearLayoutManager =
              new LinearLayoutManager(ActivityLifecycleListener.currentActivity, LinearLayoutManager.HORIZONTAL, false);
      RecyclerView recyclerView = view.findViewById(R.id.rvStories);
      storyViewListAdapter = new StoryViewListAdapter(ActivityLifecycleListener.currentActivity, stories, attrArray,
              getOnItemClickListener(stories, recyclerView));
      recyclerView.setLayoutManager(linearLayoutManager);
      recyclerView.setAdapter(storyViewListAdapter);
    } catch (Exception e) {
      Logger.e(TAG, "displayStoryHead Exception: " + e.getLocalizedMessage());
    }
  }

  private OnItemClickListener getOnItemClickListener(ArrayList<Story> stories, RecyclerView recyclerView) {
    return position -> {
      try {
        ActivityLifecycleListener.currentActivity.runOnUiThread(new Runnable() {
          @Override
          public void run() {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            String storyId = stories.get(position).getId();
            String preferencesString = sharedPreferences.getString(CleverPushPreferences.APP_OPENED_STORIES, "");
            if (preferencesString.isEmpty()) {
              editor.putString(CleverPushPreferences.APP_OPENED_STORIES, storyId).apply();
            } else {
              if (!preferencesString.contains(storyId)) {
                editor.putString(CleverPushPreferences.APP_OPENED_STORIES, preferencesString + "," + storyId).apply();
              }
            }
            StoryDetailActivity.launch(ActivityLifecycleListener.currentActivity, stories, position, storyViewOpenedListener);
            stories.get(position).setOpened(true);
            storyViewListAdapter.notifyDataSetChanged();
            recyclerView.smoothScrollToPosition(position);
          }
        });
      } catch (Exception e) {
        Logger.e(TAG, "getOnItemClickListener Exception: " + e.getLocalizedMessage());
      }
    };
  }

}
