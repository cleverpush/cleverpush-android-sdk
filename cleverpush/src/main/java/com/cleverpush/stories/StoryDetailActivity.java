package com.cleverpush.stories;

import static com.cleverpush.Constants.LOG_TAG;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.cleverpush.ActivityLifecycleListener;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.R;
import com.cleverpush.listener.StoryViewOpenedListener;
import com.cleverpush.responsehandlers.TrackStoryOpenedShownResponseHandler;
import com.cleverpush.stories.listener.OnSwipeDownListener;
import com.cleverpush.stories.listener.OnSwipeTouchListener;
import com.cleverpush.stories.listener.StoryChangeListener;
import com.cleverpush.stories.models.Story;
import com.cleverpush.stories.models.Widget;
import com.cleverpush.util.Logger;
import com.cleverpush.util.SharedPreferencesManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StoryDetailActivity extends Activity implements StoryChangeListener {

  public static int selectedPosition = 0;
  private RecyclerView recyclerView;
  private OnSwipeTouchListener onSwipeTouchListener;
  private ArrayList<Story> stories = new ArrayList<>();
  public StoryViewOpenedListener storyViewOpenedListener;
  private StoryViewListAdapter storyViewListAdapter;
  private int closeButtonPosition;
  private static final String TAG = "CleverPush/AppStoryDetails";
  private int subStoryPosition = 0;
  private int sortToLastIndex = 0;
  private String widgetId = null;
  StoryDetailListAdapter storyDetailListAdapter;
  StoryView storyView;
  private Widget widget = new Widget();

  public static void launch(Activity activity, ArrayList<Story> stories, int selectedPosition, StoryViewListAdapter storyViewListAdapter,
                            int closeButtonPosition, int subStoryPosition, String widgetId, int sortToLastIndex, StoryView storyView) {
    try {
      ActivityLifecycleListener.currentActivity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          Intent intent = new Intent(activity, StoryDetailActivity.class);
          intent.putExtra("stories", stories);
          intent.putExtra("selectedPosition", selectedPosition);
          intent.putExtra("closeButtonPosition", closeButtonPosition);
          intent.putExtra("subStoryPosition", subStoryPosition);
          intent.putExtra("widgetId", widgetId);
          intent.putExtra("sortToLastIndex", sortToLastIndex);
          StoryViewListAdapter.setStoryViewListAdapter(storyViewListAdapter);
          StoryView.setStoryView(storyView);
          activity.startActivity(intent);
        }
      });
    } catch (Exception e) {
      Logger.e(TAG, "Error while launching StoryDetailActivity", e);
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_story_detail);
    init();
  }

  private void init() {
    recyclerView = findViewById(R.id.rvStories);
    ImageView closeButtonLeft, closeButtonRight;
    closeButtonLeft = findViewById(R.id.ivClose);
    closeButtonRight = findViewById(R.id.ivCloseRight);
    handleBundleData(getIntent().getExtras());

    if (closeButtonPosition == 0) {
      configureCloseButton(closeButtonLeft, closeButtonRight);
    } else {
      configureCloseButton(closeButtonRight, closeButtonLeft);
    }

    onSwipeTouchListener = new OnSwipeTouchListener(this, recyclerView, new OnSwipeDownListener() {
      @Override
      public void onSwipeDown() {
        finish();
      }
    });
  }

  private void configureCloseButton(ImageView visibleButton, ImageView hiddenButton) {
    hiddenButton.setVisibility(View.GONE);
    visibleButton.setVisibility(View.VISIBLE);

    visibleButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        updateStoryStates();
        finish();
      }
    });
  }

  @Override
  public void onBackPressed() {
    updateStoryStates();
    finish();
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent event) {
    onSwipeTouchListener.getGestureDetector().onTouchEvent(event);
    return super.dispatchTouchEvent(event);
  }

  private void handleBundleData(Bundle extras) {
    try {
      if (extras != null) {
        if (extras.containsKey("selectedPosition")) {
          selectedPosition = extras.getInt("selectedPosition");
        }
        if (extras.containsKey("closeButtonPosition")) {
          closeButtonPosition = extras.getInt("closeButtonPosition");
        }
        if (extras.containsKey("subStoryPosition")) {
          subStoryPosition = extras.getInt("subStoryPosition");
        }
        if (extras.containsKey("widgetId")) {
          widgetId = extras.getString("widgetId");
        }
        if (extras.containsKey("sortToLastIndex")) {
          sortToLastIndex = extras.getInt("sortToLastIndex");
        }
        storyViewListAdapter = StoryViewListAdapter.getStoryViewListAdapter();
        storyView = StoryView.getStoryView();
        storyViewOpenedListener = storyView.storyViewOpenedListener;
        widget = storyView.getWidget();
        trackStoryOpened();
        if (extras.containsKey("stories")) {
          stories = (ArrayList<Story>) extras.getSerializable("stories");
          loadStoryDetails();
        }
      }
    } catch (Exception e) {
      Logger.e(TAG, "Error handling bundle data in StoryDetailActivity", e);
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    overridePendingTransition(0, R.anim.slide_down);
  }

  private void loadStoryDetails() {
    try {
      boolean isHideStoryShareButton = false;
      if (closeButtonPosition == 1) {
        isHideStoryShareButton = true;
      }
      LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
      SnapHelper snapHelper = new PagerSnapHelper();
      storyDetailListAdapter = new StoryDetailListAdapter(this, stories, this,
          storyViewOpenedListener, subStoryPosition, isHideStoryShareButton, widgetId, selectedPosition);
      recyclerView.setLayoutManager(linearLayoutManager);
      snapHelper.attachToRecyclerView(recyclerView);
      recyclerView.setAdapter(storyDetailListAdapter);
      recyclerView.smoothScrollToPosition(selectedPosition);

      recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
          super.onScrollStateChanged(recyclerView, newState);
          if (newState == RecyclerView.SCROLL_STATE_IDLE) {
            LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
            int position = layoutManager.findFirstVisibleItemPosition();
            selectedPosition = position;

            if (storyDetailListAdapter != null) {
              int subStoryIndex = getSubStoryPosition();
              storyDetailListAdapter.subStoryPosition = subStoryIndex;
              storyDetailListAdapter.refresh(selectedPosition, subStoryIndex);
            }
            recyclerView.smoothScrollToPosition(selectedPosition);
            String storyId = stories.get(position).getId();
            setStoryOpened(storyId);
          }
        }
      });

    } catch (Exception e) {
      Logger.e(TAG, "Error loading story details in StoryDetailActivity", e);
    }
  }

  @Override
  public void onNext(int position) {
    try {
      if (position != stories.size() - 1) {
        selectedPosition = position + 1;

        if (storyDetailListAdapter != null) {
          int subStoryIndex = getSubStoryPosition();
          storyDetailListAdapter.subStoryPosition = subStoryIndex;
          storyDetailListAdapter.selectedPosition = selectedPosition;
          storyDetailListAdapter.apiCallMap.put(selectedPosition, false);
          storyDetailListAdapter.refresh(selectedPosition, subStoryIndex);
        }

        recyclerView.smoothScrollToPosition(selectedPosition);

        String storyId = stories.get(position + 1).getId();
        setStoryOpened(storyId);
        trackStoryOpened();
      } else {
        runOnUiThread(this::finish);
      }
    } catch (Exception e) {
      Logger.e(TAG, "Error handling onNext in StoryDetailActivity", e);
    }
  }

  @Override
  public void onPrevious(int position) {
    try {
      if (position != 0) {
        selectedPosition = position - 1;

        if (storyDetailListAdapter != null) {
          int subStoryIndex = getSubStoryPosition();
          storyDetailListAdapter.subStoryPosition = subStoryPosition;
          storyDetailListAdapter.selectedPosition = selectedPosition;
          storyDetailListAdapter.apiCallMap.put(selectedPosition, false);
          storyDetailListAdapter.refresh(selectedPosition, subStoryIndex);
        }

        recyclerView.smoothScrollToPosition(position - 1);

        String storyId = stories.get(position - 1).getId();
        setStoryOpened(storyId);
        trackStoryOpened();
      }
    } catch (Exception e) {
      Logger.e(TAG, "Error handling onPrevious in StoryDetailActivity", e);
    }
  }

  @Override
  public void onStoryNavigation(int position, int subStoryPosition) {
    try {
      String storyId = stories.get(selectedPosition).getId();
      int subStoryCount = stories.get(selectedPosition).getSubStoryCount();
      int unreadCount = subStoryCount - (subStoryPosition + 1);

      SharedPreferences sharedPreferences = SharedPreferencesManager.getSharedPreferences(getApplicationContext());

      Gson gson = new Gson();
      Type type = new TypeToken<Map<String, Integer>>() {}.getType();

      if (selectedPosition == position) {
        if (widget != null && widget.isGroupStoryCategories()) {
          updateUnreadStoryCountForGroup(selectedPosition);
        } else {
          String storyUnreadCountString = sharedPreferences.getString(CleverPushPreferences.STORIES_UNREAD_COUNT, "");
          String subStoryPositionString = sharedPreferences.getString(CleverPushPreferences.SUB_STORY_POSITION, "");

          Map<String, Integer> storyUnreadCountMap = gson.fromJson(storyUnreadCountString, type);
          Map<String, Integer> subStoryPositionMap = gson.fromJson(subStoryPositionString, type);

          if (storyUnreadCountMap == null) storyUnreadCountMap = new HashMap<>();
          if (subStoryPositionMap == null) subStoryPositionMap = new HashMap<>();

          if (storyUnreadCountString.isEmpty()) {
            storyUnreadCountMap.put(storyId, unreadCount);
            subStoryPositionMap.put(storyId, subStoryPosition);

            updateStoryPreferences(gson.toJson(storyUnreadCountMap), gson.toJson(subStoryPositionMap), unreadCount, sharedPreferences);
          } else {
            if (storyUnreadCountMap.containsKey(storyId) && subStoryPositionMap.containsKey(storyId)) {
              int preferencesSubStoryPosition = subStoryPositionMap.get(storyId);

              if (subStoryPosition > (preferencesSubStoryPosition)) {
                storyUnreadCountMap.put(storyId, unreadCount);
                subStoryPositionMap.put(storyId, subStoryPosition);

                updateStoryPreferences(gson.toJson(storyUnreadCountMap), gson.toJson(subStoryPositionMap), unreadCount, sharedPreferences);
              }
            } else {
              storyUnreadCountMap.put(storyId, unreadCount);
              subStoryPositionMap.put(storyId, subStoryPosition);

              updateStoryPreferences(gson.toJson(storyUnreadCountMap), gson.toJson(subStoryPositionMap), unreadCount, sharedPreferences);
            }
          }
        }
      }
    } catch (Exception e) {
      Logger.e(TAG, "Error handling onStoryNavigation in StoryDetailActivity", e);
    }
  }

  private void updateStoryPreferences(String unreadCountMap, String subStoryPositionMap, int unreadCount, SharedPreferences sharedPreferences) {
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putString(CleverPushPreferences.STORIES_UNREAD_COUNT, unreadCountMap).apply();
    editor.putString(CleverPushPreferences.SUB_STORY_POSITION, subStoryPositionMap).apply();
    editor.apply();

    stories.get(selectedPosition).setUnreadCount(unreadCount);
  }

  private void updateStoryStates() {
    try {
      SharedPreferences sharedPreferences = SharedPreferencesManager.getSharedPreferences(getApplicationContext());

      if (widget != null && widget.isGroupStoryCategories()) {
        setOpenedForGroupStories();
        for (int i = 0; i < stories.size(); i++) {
          updateUnreadStoryCountForGroup(i);
        }
      } else {
        String openStoriesString = sharedPreferences.getString(CleverPushPreferences.APP_OPENED_STORIES, "");
        String storyUnreadCountString = sharedPreferences.getString(CleverPushPreferences.STORIES_UNREAD_COUNT, "");

        if (!openStoriesString.isEmpty()) {
          for (int i = 0; i < stories.size(); i++) {
            if (openStoriesString.contains(stories.get(i).getId())) {
              stories.get(i).setOpened(true);
            } else {
              stories.get(i).setOpened(false);
            }
          }
        }

        if (!storyUnreadCountString.isEmpty()) {
          for (int i = 0; i < stories.size(); i++) {
            if (storyUnreadCountString.contains(stories.get(i).getId())) {
              Type type = new TypeToken<Map<String, Integer>>() {
              }.getType();
              Map<String, Integer> storyUnreadCountMap = new Gson().fromJson(storyUnreadCountString, type);
              int preferencesSubStoryPosition = storyUnreadCountMap.get(stories.get(i).getId());
              stories.get(i).setUnreadCount(preferencesSubStoryPosition);
            }
          }
        }
      }

      if (sortToLastIndex == 1) {
        ArrayList<Story> categorizeStories = categorizeStories(stories);
        stories.clear();
        stories.addAll(categorizeStories);
      }

      runOnUiThread(() -> {
        if (storyView != null) {
          storyView.updateStories(stories);
        }
        if (storyViewListAdapter != null) {
          storyViewListAdapter.updateStories(stories);
        }
      });
    } catch (Exception e) {
      Logger.e(TAG, "Error while updateStoryStates. " + e.getLocalizedMessage(), e);
    }
  }

  private int getSubStoryPosition() {
    int subStoryIndex = 0;
    try {
      SharedPreferences sharedPreferences = SharedPreferencesManager.getSharedPreferences(getApplicationContext());
      if (widget != null && widget.isGroupStoryCategories()) {
        String[] storyIdArray = stories.get(selectedPosition).getId().split(",");
        String storyUnreadCountString = sharedPreferences.getString(CleverPushPreferences.STORIES_UNREAD_COUNT_GROUP, "");
        String[] readStoryIdArray = storyUnreadCountString.split(",");

        for (String subStoryID : storyIdArray) {
          if (Arrays.asList(readStoryIdArray).contains(subStoryID)) {
            subStoryIndex++;
          }
        }

        if (storyIdArray.length == subStoryIndex) {
          subStoryIndex = 0;
        }
      } else {
        String subStoryPositionString = sharedPreferences.getString(CleverPushPreferences.SUB_STORY_POSITION, "");
        String storyId = stories.get(selectedPosition).getId();

        if (!subStoryPositionString.isEmpty()) {
          Type type = new TypeToken<Map<String, Integer>>() {
          }.getType();
          Map<String, Integer> subStoryPositionMap = new Gson().fromJson(subStoryPositionString, type);

          if (subStoryPositionMap.containsKey(storyId)) {
            subStoryIndex = subStoryPositionMap.get(storyId) + 1;
          }

          if (stories.get(selectedPosition).getSubStoryCount() == subStoryIndex) {
            subStoryIndex = 0;
          }
        }
      }
      subStoryPosition = subStoryIndex;
    } catch (Exception e) {
      Logger.e(TAG, "Error while getSubStoryPosition. " + e.getLocalizedMessage(), e);
    }
    return subStoryIndex;
  }

  private void setStoryOpened(String storyId) {
    try {
      SharedPreferences sharedPreferences = SharedPreferencesManager.getSharedPreferences(getApplicationContext());
      SharedPreferences.Editor editor = sharedPreferences.edit();
      if (widget != null && widget.isGroupStoryCategories()) {
        setOpenedForGroupStories();
      } else {
        String preferencesString = sharedPreferences.getString(CleverPushPreferences.APP_OPENED_STORIES, "");

        if (preferencesString.isEmpty()) {
          editor.putString(CleverPushPreferences.APP_OPENED_STORIES, storyId).apply();
        } else {
          if (!preferencesString.contains(storyId)) {
            editor.putString(CleverPushPreferences.APP_OPENED_STORIES, preferencesString + "," + storyId).apply();
          }
        }
        editor.apply();
        String storyIds = sharedPreferences.getString(CleverPushPreferences.APP_OPENED_STORIES, "");
        for (int i = 0; i < stories.size(); i++) {
          stories.get(i).setOpened(storyIds.contains(stories.get(i).getId()));
        }
      }

      runOnUiThread(() -> {
        if (storyViewListAdapter != null) {
          storyViewListAdapter.updateStories(stories);
        }
      });
    } catch (Exception e) {
      Logger.e(TAG, "Error while setStoryOpened. " + e.getLocalizedMessage(), e);
    }
  }

  private ArrayList<Story> categorizeStories(ArrayList<Story> stories) {
    SharedPreferences sharedPreferences = SharedPreferencesManager.getSharedPreferences(getApplicationContext());

    ArrayList<Story> openedStories = new ArrayList<>();
    ArrayList<Story> unseenStories = new ArrayList<>();

    for (Story story : stories) {
      if (widget != null && widget.isGroupStoryCategories()) {
        String storyUnreadCountString = sharedPreferences.getString(CleverPushPreferences.STORIES_UNREAD_COUNT_GROUP, "");
        Set<String> readStoryIds = storyUnreadCountString.isEmpty() ? new HashSet<>() : new HashSet<>(Arrays.asList(storyUnreadCountString.split(",")));
        String[] storyIdArray = story.getId().split(",");

        boolean isOpened = false;
        for (String subStoryID : storyIdArray) {
          if (readStoryIds.contains(subStoryID)) {
            isOpened = true;
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
        String preferencesString = sharedPreferences.getString(CleverPushPreferences.STORIES_UNREAD_COUNT, "");

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

  public void trackStoryOpened() {
    if (widgetId == null || widgetId.length() == 0) {
      return;
    }

    String storyPath = "/story-widget/" + widgetId + "/track-opened";
    Logger.d(TAG, "Loading stories: " + storyPath);

    SharedPreferences sharedPreferences = SharedPreferencesManager.getSharedPreferences(getApplicationContext());
    String preferencesString = sharedPreferences.getString(CleverPushPreferences.APP_OPENED_STORIES, "");

    ArrayList<String> storyIds = new ArrayList<>();
    if (!preferencesString.isEmpty()) {
      storyIds = new ArrayList<>(Arrays.asList(preferencesString.split(",")));
    }

    JSONObject jsonBody = new JSONObject();
    try {
      jsonBody.put("stories", new JSONArray(storyIds));
    } catch (JSONException ex) {
      Logger.e(LOG_TAG, "Error creating trackStoryOpened request parameter", ex);
    }

    CleverPushHttpClient.postWithRetry(storyPath, jsonBody,
        new TrackStoryOpenedShownResponseHandler().getResponseHandler(true));
  }

  private void setOpenedForGroupStories() {
    try {
      SharedPreferences sharedPreferences = SharedPreferencesManager.getSharedPreferences(getApplicationContext());
      String storyUnreadCountString = sharedPreferences.getString(CleverPushPreferences.STORIES_UNREAD_COUNT_GROUP, "");

      if (!storyUnreadCountString.isEmpty()) {
        Set<String> readStoryIds = new HashSet<>(Arrays.asList(storyUnreadCountString.split(",")));

        for (int i = 0; i < stories.size(); i++) {
          String[] storyIdArray = stories.get(i).getId().split(",");
          boolean isOpened = false;

          for (String subStoryID : storyIdArray) {
            if (readStoryIds.contains(subStoryID)) {
              isOpened = true;
              break;
            }
          }

          stories.get(i).setOpened(isOpened);
        }
      }
    } catch (Exception e) {
      Logger.e(TAG, "Error while setOpened for group stories. " + e.getLocalizedMessage(), e);
    }
  }

  private void updateUnreadStoryCountForGroup(int selectedPosition) {
    try {
      SharedPreferences sharedPreferences = SharedPreferencesManager.getSharedPreferences(getApplicationContext());
      SharedPreferences.Editor editor = sharedPreferences.edit();

      String storyUnreadCountString = sharedPreferences.getString(CleverPushPreferences.STORIES_UNREAD_COUNT_GROUP, "");
      String[] storyIdArray = stories.get(selectedPosition).getId().split(",");

      String subStoryId = "";
      if (subStoryPosition >= 0 && subStoryPosition < storyIdArray.length) {
        subStoryId = storyIdArray[subStoryPosition];
      }

      if (storyUnreadCountString.isEmpty()) {
        editor.putString(CleverPushPreferences.STORIES_UNREAD_COUNT_GROUP, subStoryId);
      } else {
        if (!storyUnreadCountString.contains(subStoryId)) {
          editor.putString(CleverPushPreferences.STORIES_UNREAD_COUNT_GROUP, storyUnreadCountString + "," + subStoryId);
        }
      }
      editor.apply();
      storyUnreadCountString = sharedPreferences.getString(CleverPushPreferences.STORIES_UNREAD_COUNT_GROUP, "");

      String[] readStoryIdArray = storyUnreadCountString.split(",");

      int readCount = 0;
      for (String subStoryID : storyIdArray) {
        if (Arrays.asList(readStoryIdArray).contains(subStoryID)) {
          readCount++;
        }
      }

      int unreadCount = storyIdArray.length - readCount;
      stories.get(selectedPosition).setUnreadCount(unreadCount);
    } catch (Exception e) {
      Logger.e(TAG, "Error while updateUnreadStoryCountForGroup. " + e.getLocalizedMessage(), e);
    }
  }

}
