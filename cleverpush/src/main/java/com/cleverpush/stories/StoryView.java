package com.cleverpush.stories;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
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
import com.cleverpush.util.SharedPreferencesManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Map;

public class StoryView extends LinearLayout {

  protected static final int DEFAULT_BACKGROUND_COLOR = Color.WHITE;
  private static final String TAG = "CleverPush/AppStories";

  private TypedArray attrArray;
  private StoryViewListAdapter storyViewListAdapter;

  private Context context;
  private boolean loading = false;
  private ArrayList<Story> stories = new ArrayList<>();
  private String widgetId = null;
  public static StoryView storyView;
  public StoryViewOpenedListener storyViewOpenedListener;
  private int sortToLastIndex = 0;

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
      CleverPushHttpClient.getWithRetry(storyPath, getResponseHandler());
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
          SharedPreferences sharedPreferences = SharedPreferencesManager.getSharedPreferences(context);
          String preferencesString = sharedPreferences.getString(CleverPushPreferences.STORIES_UNREAD_COUNT, "");
          for (int i = 0; i < stories.size(); i++) {
            if (stories.get(i).getContent().getPages() != null) {
              stories.get(i).setSubStoryCount(stories.get(i).getContent().getPages().size());
            }
            if (!preferencesString.isEmpty()) {
              Type type = new TypeToken<Map<String, Integer>>() {}.getType();
              Map<String, Integer> existingMap = gson.fromJson(preferencesString, type);
              String storyId = stories.get(i).getId();

              if (existingMap.containsKey(storyId)) {
                int unreadCount = existingMap.get(storyId);
                stories.get(i).setUnreadCount(unreadCount);
              } else {
                stories.get(i).setUnreadCount(stories.get(i).getContent().getPages().size());
              }
            } else {
              stories.get(i).setUnreadCount(stories.get(i).getContent().getPages().size());
            }

            if (sharedPreferences.getString(CleverPushPreferences.APP_OPENED_STORIES, "")
                    .contains(stories.get(i).getId())) {
              stories.get(i).setOpened(true);
            } else {
              stories.get(i).setOpened(false);
            }
          }

          sortToLastIndex = attrArray.getInt(R.styleable.StoryView_sort_to_last_index, 0);
          if (sortToLastIndex == 1) {
            ArrayList<Story> categorizeStories = categorizeStories(stories);
            stories.clear();
            stories.addAll(categorizeStories);
          }

          ActivityLifecycleListener.currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              displayStoryHead(stories);
            }
          });
        } catch (Exception e) {
          Logger.e(TAG, "Error in onSuccess of loading stories request.", e);
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
      RecyclerView recyclerView = view.findViewById(R.id.rvStories);

      relativeLayout.setBackgroundColor(
              attrArray.getColor(R.styleable.StoryView_background_color, DEFAULT_BACKGROUND_COLOR));
      ViewGroup.LayoutParams params = relativeLayout.getLayoutParams();
      params.height = getDimensionOrEnum(attrArray, R.styleable.StoryView_story_view_height);
      params.width = getDimensionOrEnum(attrArray, R.styleable.StoryView_story_view_width);
      relativeLayout.setLayoutParams(params);

      ViewGroup.LayoutParams recyclerViewParams = recyclerView.getLayoutParams();
      recyclerViewParams.height = getDimensionOrEnum(attrArray, R.styleable.StoryView_story_view_height);
      recyclerViewParams.width = getDimensionOrEnum(attrArray, R.styleable.StoryView_story_view_width);
      recyclerView.setLayoutParams(recyclerViewParams);

      recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
          recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

          int recyclerViewWidth = recyclerView.getWidth();

          LinearLayoutManager linearLayoutManager =
              new LinearLayoutManager(ActivityLifecycleListener.currentActivity, LinearLayoutManager.HORIZONTAL, false);
          storyViewListAdapter = new StoryViewListAdapter(ActivityLifecycleListener.currentActivity, stories, attrArray,
              getOnItemClickListener(stories, recyclerView),recyclerViewWidth);
          recyclerView.setLayoutManager(linearLayoutManager);
          recyclerView.setAdapter(storyViewListAdapter);
        }
      });
    } catch (Exception e) {
      Logger.e(TAG, "Error while displaying story head.", e);
    }
  }

  private OnItemClickListener getOnItemClickListener(ArrayList<Story> stories, RecyclerView recyclerView) {
    return position -> {
      try {
        ActivityLifecycleListener.currentActivity.runOnUiThread(new Runnable() {
          @Override
          public void run() {
            SharedPreferences sharedPreferences = SharedPreferencesManager.getSharedPreferences(context);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            String preferencesString = sharedPreferences.getString(CleverPushPreferences.APP_OPENED_STORIES, "");
            String subStoryPositionString = sharedPreferences.getString(CleverPushPreferences.SUB_STORY_POSITION, "");

            if (sortToLastIndex == 1) {
              ArrayList<Story> categorizeStories = categorizeStories(stories);
              stories.clear();
              stories.addAll(categorizeStories);
            }

            String storyId = stories.get(position).getId();
            if (preferencesString.isEmpty()) {
              editor.putString(CleverPushPreferences.APP_OPENED_STORIES, storyId).apply();
            } else {
              if (!preferencesString.contains(storyId)) {
                editor.putString(CleverPushPreferences.APP_OPENED_STORIES, preferencesString + "," + storyId).apply();
              }
            }

            int subStoryIndex = 0;
            if (!subStoryPositionString.isEmpty()) {
              Type type = new TypeToken<Map<String, Integer>>() {}.getType();
              Map<String, Integer> subStoryPositionMap = new Gson().fromJson(subStoryPositionString, type);

              if (subStoryPositionMap.containsKey(storyId)) {
                subStoryIndex = subStoryPositionMap.get(storyId) + 1;
              }

              if (stories.get(position).getSubStoryCount() == subStoryIndex) {
                subStoryIndex = 0;
              }
            }

            int closeButtonPosition = attrArray.getInt(R.styleable.StoryView_close_button_position, 0);

            stories.get(position).setOpened(true);
            StoryDetailActivity.launch(ActivityLifecycleListener.currentActivity, stories, position, storyViewOpenedListener, storyViewListAdapter,
                closeButtonPosition, subStoryIndex, widgetId, sortToLastIndex, StoryView.this);
            recyclerView.smoothScrollToPosition(position);
          }
        });
      } catch (Exception e) {
        Logger.e(TAG, "Error getOnItemClickListener of StoryView.", e);
      }
    };
  }

  private int getDimensionOrEnum(TypedArray attrArray, int index) {
    try {
      // Try to get the dimension first
      return attrArray.getDimensionPixelSize(index, ViewGroup.LayoutParams.WRAP_CONTENT);
    } catch (Exception e) {
      // If it fails, it's likely an enum, so get it as an int
      return attrArray.getInt(index, ViewGroup.LayoutParams.WRAP_CONTENT);
    }
  }

  public void updateStories(ArrayList<Story> stories) {
    this.stories.clear();
    this.stories.addAll(stories);
    if (storyViewListAdapter != null) {
      if (sortToLastIndex == 1) {
        ArrayList<Story> categorizedStories = categorizeStories(this.stories);
        storyViewListAdapter.updateStories(categorizedStories);
      } else {
        storyViewListAdapter.updateStories(this.stories);
      }
      storyViewListAdapter.notifyDataSetChanged();
    }
  }

  public static StoryView getStoryView() {
    return storyView;
  }

  public static void setStoryView(StoryView storyview) {
    storyView = storyview;
  }

  private ArrayList<Story> categorizeStories(ArrayList<Story> stories) {
    SharedPreferences sharedPreferences = SharedPreferencesManager.getSharedPreferences(context);
    String preferencesString = sharedPreferences.getString(CleverPushPreferences.STORIES_UNREAD_COUNT, "");

    ArrayList<Story> openedStories = new ArrayList<>();
    ArrayList<Story> unseenStories = new ArrayList<>();

    for (Story story : stories) {
      if (preferencesString.contains(story.getId())) {
        story.setOpened(true);
        openedStories.add(story);
      } else {
        story.setOpened(false);
        unseenStories.add(story);
      }
    }

    ArrayList<Story> categorizeStories = new ArrayList<>();
    categorizeStories.addAll(unseenStories);
    categorizeStories.addAll(openedStories);

    return categorizeStories;
  }

}
