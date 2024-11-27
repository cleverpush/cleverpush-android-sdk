package com.cleverpush.stories;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
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
import com.cleverpush.responsehandlers.TrackStoryOpenedShownResponseHandler;
import com.cleverpush.stories.listener.OnItemClickListener;
import com.cleverpush.stories.models.Story;
import com.cleverpush.stories.models.StoryListModel;
import com.cleverpush.stories.models.Widget;
import com.cleverpush.util.Logger;
import com.cleverpush.util.SharedPreferencesManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StoryView extends LinearLayout {

  protected static final int DEFAULT_BACKGROUND_COLOR = Color.WHITE;
  private static final String TAG = "CleverPush/AppStories";

  private TypedArray attrArray;
  private StoryViewListAdapter storyViewListAdapter;

  private Context context;
  private boolean loading = false;
  private ArrayList<Story> stories = new ArrayList<>();
  private Widget widget = new Widget();
  private String widgetId = null;
  public static StoryView storyView;
  public StoryViewOpenedListener storyViewOpenedListener;
  private int sortToLastIndex = 0;
  private boolean isDarkModeEnabled = false;
  private boolean hasTrackedStoryShown = false;

  public String getWidgetId() {
    return widgetId;
  }

  public Widget getWidget() {
    return widget;
  }

  public void setWidgetId(String widgetId) {
    this.widgetId = widgetId;
    loadStory();
  }

  public void setDarkModeEnabled(boolean darkModeEnabled) {
    this.isDarkModeEnabled = darkModeEnabled;
    if (storyViewListAdapter != null) {
      storyViewListAdapter.notifyDataSetChanged();
    }
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

    String storyPath = "/story-widget/" + widgetId + "/config?platform=app";
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
          widget = model.getWidget();
          SharedPreferences sharedPreferences = SharedPreferencesManager.getSharedPreferences(context);
          String storyOpenPreferences = sharedPreferences.getString(CleverPushPreferences.APP_OPENED_STORIES, "");
          for (int i = 0; i < stories.size(); i++) {
            if (widget != null && widget.isGroupStoryCategories()) {
              String[] storyIdArray = stories.get(i).getId().split(",");
              int subStoryCount = storyIdArray.length;
              stories.get(i).setSubStoryCount(subStoryCount);
              String storyUnreadCountString = sharedPreferences.getString(CleverPushPreferences.STORIES_UNREAD_COUNT_GROUP, "");
              String[] readStoryIdArray = storyUnreadCountString.split(",");

              int readCount = 0;
              for (String subStoryID : storyIdArray) {
                if (Arrays.asList(readStoryIdArray).contains(subStoryID)) {
                  readCount++;
                }
              }

              int unreadCount = storyIdArray.length - readCount;

              stories.get(i).setUnreadCount(unreadCount);

              setOpenedForGroupStories();
            } else {
              String preferencesString = sharedPreferences.getString(CleverPushPreferences.STORIES_UNREAD_COUNT, "");

              if (stories.get(i).getContent().getPages() != null) {
                stories.get(i).setSubStoryCount(stories.get(i).getContent().getPages().size());
              }
              if (!preferencesString.isEmpty()) {
                Type type = new TypeToken<Map<String, Integer>>() {
                }.getType();
                Map<String, Integer> existingMap = gson.fromJson(preferencesString, type);
                String storyId = stories.get(i).getId();

                if (existingMap.containsKey(storyId)) {
                  int unreadCount = existingMap.get(storyId);
                  stories.get(i).setUnreadCount(unreadCount);
                } else {
                  if (stories.get(i).getContent().getPages() != null) {
                    stories.get(i).setUnreadCount(stories.get(i).getContent().getPages().size());
                  }
                }
              } else {
                if (stories.get(i).getContent().getPages() != null) {
                  stories.get(i).setUnreadCount(stories.get(i).getContent().getPages().size());
                }
              }

              if (storyOpenPreferences.contains(stories.get(i).getId())) {
                stories.get(i).setOpened(true);
              } else {
                stories.get(i).setOpened(false);
              }
            }
          }

          if (widget != null && widget.isGroupStoryCategories()) {
            syncUnreadStoryIds();
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

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    getViewTreeObserver().addOnScrollChangedListener(() -> {
      if (!hasTrackedStoryShown && isViewVisibleOnScreen(StoryView.this)) {
        hasTrackedStoryShown = true;
        getViewTreeObserver().removeOnScrollChangedListener(this::onAttachedToWindow);
        trackStoryShown();
      }
    });
  }

  /**
   * Checks if the view is visible on the screen (at least 1 pixel).
   *
   * @param view The view to check.
   * @return true if the view is visible on the screen.
   */
  private boolean isViewVisibleOnScreen(View view) {
    if (view == null || !view.isShown()) {
      return false;
    }

    Rect rect = new Rect();
    boolean isVisible = view.getGlobalVisibleRect(rect);

    // Check if at least one pixel is visible
    return isVisible && rect.height() > 0 && rect.width() > 0;
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
              getOnItemClickListener(stories, recyclerView), recyclerViewWidth, widget.isGroupStoryCategories(), isDarkModeEnabled);
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
        if (ActivityLifecycleListener.currentActivity != null) {
          ActivityLifecycleListener.currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              SharedPreferences sharedPreferences = SharedPreferencesManager.getSharedPreferences(context);
              SharedPreferences.Editor editor = sharedPreferences.edit();
              String preferencesString = sharedPreferences.getString(CleverPushPreferences.APP_OPENED_STORIES, "");

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

              int subStoryIndex = calculateSubStoryIndex(sharedPreferences, stories.get(position));

              int closeButtonPosition = attrArray.getInt(R.styleable.StoryView_close_button_position, 0);

              if (widget != null && !widget.isGroupStoryCategories()) {
                stories.get(position).setOpened(true);
              }
              StoryDetailActivity.launch(ActivityLifecycleListener.currentActivity, stories, position, storyViewListAdapter,
                  closeButtonPosition, subStoryIndex, widgetId, sortToLastIndex, StoryView.this);
              recyclerView.smoothScrollToPosition(position);
            }
          });
        } else {
          Logger.i(TAG, "getOnItemClickListener: ActivityLifecycleListener.currentActivity is null");
        }
      } catch (Exception e) {
        Logger.e(TAG, "Error getOnItemClickListener of StoryView.", e);
      }
    };
  }

  private int calculateSubStoryIndex(SharedPreferences sharedPreferences, Story story) {
    int subStoryIndex = 0;
    String subStoryPositionString = sharedPreferences.getString(CleverPushPreferences.SUB_STORY_POSITION, "");

    if (widget != null && widget.isGroupStoryCategories()) {
      String[] storyIdArray = story.getId().split(",");
      String storyUnreadCountString = sharedPreferences.getString(CleverPushPreferences.STORIES_UNREAD_COUNT_GROUP, "");
      String[] readStoryIdArray = storyUnreadCountString.split(",");

      for (String subStoryID : storyIdArray) {
        if (Arrays.asList(readStoryIdArray).contains(subStoryID)) {
          subStoryIndex++;
        } else {
          break;
        }
      }

      if (storyIdArray.length == subStoryIndex) {
        subStoryIndex = 0;
      }
    } else {
      if (!subStoryPositionString.isEmpty()) {
        String storyId = story.getId();
        Type type = new TypeToken<Map<String, Integer>>() {
        }.getType();
        Map<String, Integer> subStoryPositionMap = new Gson().fromJson(subStoryPositionString, type);

        if (subStoryPositionMap.containsKey(storyId)) {
          subStoryIndex = subStoryPositionMap.get(storyId) + 1;
        }

        if (story.getSubStoryCount() == subStoryIndex) {
          subStoryIndex = 0;
        }
      }
    }
    return subStoryIndex;
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
      if (widget != null && widget.isGroupStoryCategories()) {
        String storyUnreadCountString = sharedPreferences.getString(CleverPushPreferences.STORIES_UNREAD_COUNT_GROUP, "");
        Set<String> readStoryIds = storyUnreadCountString.isEmpty() ? new HashSet<>() : new HashSet<>(Arrays.asList(storyUnreadCountString.split(",")));
        String[] storyIdArray = story.getId().split(",");

        boolean isOpened = true;
        for (String subStoryID : storyIdArray) {
          if (!readStoryIds.contains(subStoryID)) {
            isOpened = false;
            break;
          }
        }

        story.setOpened(isOpened);
        if (isOpened) {
          openedStories.add(story);
        } else {
          unseenStories.add(story);
        }
      } else {
        if (preferencesString.contains(story.getId())) {
          story.setOpened(true);
          openedStories.add(story);
        } else {
          story.setOpened(false);
          unseenStories.add(story);
        }
      }
    }

    ArrayList<Story> categorizeStories = new ArrayList<>();
    categorizeStories.addAll(unseenStories);
    categorizeStories.addAll(openedStories);

    return categorizeStories;
  }

  private void setOpenedForGroupStories() {
    try {
      SharedPreferences sharedPreferences = SharedPreferencesManager.getSharedPreferences(context);
      String storyUnreadCountString = sharedPreferences.getString(CleverPushPreferences.STORIES_UNREAD_COUNT_GROUP, "");

      if (!storyUnreadCountString.isEmpty()) {
        Set<String> readStoryIds = new HashSet<>(Arrays.asList(storyUnreadCountString.split(",")));

        for (int i = 0; i < stories.size(); i++) {
          String[] storyIdArray = stories.get(i).getId().split(",");
          boolean isOpened = true;

          for (String subStoryID : storyIdArray) {
            if (!readStoryIds.contains(subStoryID)) {
              isOpened = false;
              break;
            }
          }

          stories.get(i).setOpened(isOpened);
        }
      }
    } catch (Exception e) {
      Logger.e(TAG, "Error while setting opened for group stories. " + e.getLocalizedMessage(), e);
    }
  }

  private void trackStoryShown() {
    if (widgetId == null || widgetId.length() == 0) {
      return;
    }

    if (stories.size() == 0) {
      return;
    }

    String storyPath = "/story-widget/" + widgetId + "/track-shown";

    ArrayList<String> storyIds = new ArrayList<>();

    for (int i = 0; i < stories.size(); i++) {
      String storyIdStr = stories.get(i).getId();
      if (!storyIdStr.isEmpty()) {
        storyIds.addAll(Arrays.asList(storyIdStr.split(",")));
      }
    }

    JSONObject jsonBody = new JSONObject();
    try {
      jsonBody.put("stories", new JSONArray(storyIds));
    } catch (JSONException ex) {
      Logger.e(TAG, "Error creating track stories shown request parameter", ex);
    }

    CleverPushHttpClient.postWithRetry(storyPath, jsonBody,
        new TrackStoryOpenedShownResponseHandler().getResponseHandler(false));
  }

  private void syncUnreadStoryIds() {
    try {
      SharedPreferences sharedPreferences = SharedPreferencesManager.getSharedPreferences(context);
      SharedPreferences.Editor editor = sharedPreferences.edit();
      String storyUnreadCountString = sharedPreferences.getString(CleverPushPreferences.STORIES_UNREAD_COUNT_GROUP, "");

      if (!storyUnreadCountString.isEmpty()) {
        Set<String> readStoryIds = new HashSet<>(Arrays.asList(storyUnreadCountString.split(",")));

        StringBuilder updatedUnreadStoryIds = new StringBuilder();
        for (int i = 0; i < stories.size(); i++) {
          String[] storyIdArray = stories.get(i).getId().split(",");

          for (String subStoryID : storyIdArray) {
            if (readStoryIds.contains(subStoryID)) {
              if (updatedUnreadStoryIds.length() == 0) {
                updatedUnreadStoryIds = new StringBuilder(subStoryID);
              } else {
                updatedUnreadStoryIds.append(",").append(subStoryID);
              }
            }
          }
        }

        editor.remove(CleverPushPreferences.STORIES_UNREAD_COUNT_GROUP);
        editor.apply();
        editor.putString(CleverPushPreferences.STORIES_UNREAD_COUNT_GROUP, String.valueOf(updatedUnreadStoryIds));
        editor.apply();
      }
    } catch (Exception e) {
      Logger.e(TAG, "Error while updating unread story IDs. " + e.getLocalizedMessage(), e);
    }
  }
}
