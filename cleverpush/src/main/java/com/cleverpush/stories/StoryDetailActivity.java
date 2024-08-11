package com.cleverpush.stories;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.cleverpush.ActivityLifecycleListener;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.R;
import com.cleverpush.listener.StoryViewOpenedListener;
import com.cleverpush.stories.listener.OnSwipeDownListener;
import com.cleverpush.stories.listener.OnSwipeTouchListener;
import com.cleverpush.stories.listener.StoryChangeListener;
import com.cleverpush.stories.models.Story;
import com.cleverpush.util.Logger;
import com.cleverpush.util.SharedPreferencesManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

  public static void launch(Activity activity, ArrayList<Story> stories, int selectedPosition, StoryViewOpenedListener storyViewOpenedListener,
                            StoryViewListAdapter storyViewListAdapter, int closeButtonPosition, int subStoryPosition) {
    try {
      ActivityLifecycleListener.currentActivity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          Intent intent = new Intent(activity, StoryDetailActivity.class);
          intent.putExtra("stories", stories);
          intent.putExtra("selectedPosition", selectedPosition);
          intent.putExtra("storyViewOpenedListener", storyViewOpenedListener);
          intent.putExtra("closeButtonPosition", closeButtonPosition);
          intent.putExtra("subStoryPosition", subStoryPosition);
          StoryViewListAdapter.setStoryViewListAdapter(storyViewListAdapter);
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
        SharedPreferences sharedPreferences = SharedPreferencesManager.getSharedPreferences(getApplicationContext());
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
              Type type = new TypeToken<Map<String, Integer>>() {}.getType();
              Map<String, Integer> storyUnreadCountMap = new Gson().fromJson(storyUnreadCountString, type);
              int preferencesSubStoryPosition = storyUnreadCountMap.get(stories.get(i).getId());
              stories.get(i).setUnreadCount(preferencesSubStoryPosition);
            }
          }
        }

        runOnUiThread(() -> {
          if (storyViewListAdapter != null) {
            storyViewListAdapter.updateStories(stories);
          }
        });

        finish();
      }
    });
  }

  @Override
  public void onBackPressed() {
    SharedPreferences sharedPreferences = SharedPreferencesManager.getSharedPreferences(getApplicationContext());
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
          Type type = new TypeToken<Map<String, Integer>>() {}.getType();
          Map<String, Integer> storyUnreadCountMap = new Gson().fromJson(storyUnreadCountString, type);
          int preferencesSubStoryPosition = storyUnreadCountMap.get(stories.get(i).getId());
          stories.get(i).setUnreadCount(preferencesSubStoryPosition);
        }
      }
    }

    runOnUiThread(() -> {
      if (storyViewListAdapter != null) {
        storyViewListAdapter.updateStories(stories);
      }
    });

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
        if (extras.containsKey("storyViewOpenedListener")) {
          storyViewOpenedListener = (StoryViewOpenedListener) getIntent().getSerializableExtra("storyViewOpenedListener");
        }
        if (extras.containsKey("stories")) {
          stories = (ArrayList<Story>) extras.getSerializable("stories");
          loadStoryDetails();
        }
        if (extras.containsKey("closeButtonPosition")) {
          closeButtonPosition = extras.getInt("closeButtonPosition");
        }
        if (extras.containsKey("subStoryPosition")) {
          subStoryPosition = extras.getInt("subStoryPosition");
        }
        storyViewListAdapter = StoryViewListAdapter.getStoryViewListAdapter();
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
      StoryDetailListAdapter storyDetailListAdapter = new StoryDetailListAdapter(this, stories, this, storyViewOpenedListener, subStoryPosition, isHideStoryShareButton);
      recyclerView.setLayoutManager(linearLayoutManager);
      snapHelper.attachToRecyclerView(recyclerView);
      recyclerView.setAdapter(storyDetailListAdapter);
      recyclerView.smoothScrollToPosition(selectedPosition);
    } catch (Exception e) {
      Logger.e(TAG, "Error loading story details in StoryDetailActivity", e);
    }
  }

  @Override
  public void onNext(int position) {
    try {
      if (position != stories.size() - 1) {
        selectedPosition = position + 1;
        recyclerView.smoothScrollToPosition(position + 1);
      } else {
        runOnUiThread(this::finish);
      }

      if (position + 1 < stories.size()) {
        SharedPreferences sharedPreferences = SharedPreferencesManager.getSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String preferencesString = sharedPreferences.getString(CleverPushPreferences.APP_OPENED_STORIES, "");

        String nextStoryId = stories.get(position + 1).getId();
        if (preferencesString.isEmpty()) {
          editor.putString(CleverPushPreferences.APP_OPENED_STORIES, nextStoryId).apply();
        } else {
          if (!preferencesString.contains(nextStoryId)) {
            editor.putString(CleverPushPreferences.APP_OPENED_STORIES, preferencesString + "," + nextStoryId).apply();
          }
        }
        editor.commit();
        String storyIds = sharedPreferences.getString(CleverPushPreferences.APP_OPENED_STORIES, "");
        for (int i = 0; i < stories.size(); i++) {
          if (storyIds.contains(stories.get(i).getId())) {
            stories.get(i).setOpened(true);
          } else {
            stories.get(i).setOpened(false);
          }
        }
        runOnUiThread(() -> {
          if (storyViewListAdapter != null) {
            storyViewListAdapter.updateStories(stories);
          }
        });
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
        recyclerView.smoothScrollToPosition(position - 1);

        SharedPreferences sharedPreferences = SharedPreferencesManager.getSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String preferencesString = sharedPreferences.getString(CleverPushPreferences.APP_OPENED_STORIES, "");
        String previousStoryId = stories.get(position - 1).getId();
        if (preferencesString.isEmpty()) {
          editor.putString(CleverPushPreferences.APP_OPENED_STORIES, previousStoryId).apply();
        } else {
          if (!preferencesString.contains(previousStoryId)) {
            editor.putString(CleverPushPreferences.APP_OPENED_STORIES, preferencesString + "," + previousStoryId).apply();
          }
        }
        editor.commit();
        String storyIds = sharedPreferences.getString(CleverPushPreferences.APP_OPENED_STORIES, "");
        for (int i = 0; i < stories.size(); i++) {
          stories.get(i).setOpened(storyIds.contains(stories.get(i).getId()));
        }

        runOnUiThread(() -> {
          if (storyViewListAdapter != null) {
            storyViewListAdapter.updateStories(stories);
          }
        });
      }
    } catch (Exception e) {
      Logger.e(TAG, "Error handling onPrevious in StoryDetailActivity", e);
    }
  }

  @Override
  public void onStoryNavigation(int position, int subStoryPosition) {
    try {
      String storyId = stories.get(position).getId();
      int subStoryCount = stories.get(position).getSubStoryCount();
      int unreadCount = subStoryCount - (subStoryPosition + 1);

      SharedPreferences sharedPreferences = SharedPreferencesManager.getSharedPreferences(getApplicationContext());
      SharedPreferences.Editor editor = sharedPreferences.edit();

      String storyUnreadCountString = sharedPreferences.getString(CleverPushPreferences.STORIES_UNREAD_COUNT, "");
      String subStoryPositionString = sharedPreferences.getString(CleverPushPreferences.SUB_STORY_POSITION, "");

      Gson gson = new Gson();
      Type type = new TypeToken<Map<String, Integer>>() {}.getType();

      Map<String, Integer> storyUnreadCountMap = gson.fromJson(storyUnreadCountString, type);
      Map<String, Integer> subStoryPositionMap = gson.fromJson(subStoryPositionString, type);

      if (storyUnreadCountMap == null) storyUnreadCountMap = new HashMap<>();
      if (subStoryPositionMap == null) subStoryPositionMap = new HashMap<>();

      if (storyUnreadCountString.isEmpty()) {
        storyUnreadCountMap.put(storyId, unreadCount);
        editor.putString(CleverPushPreferences.STORIES_UNREAD_COUNT, gson.toJson(storyUnreadCountMap)).apply();

        subStoryPositionMap.put(storyId, subStoryPosition);
        editor.putString(CleverPushPreferences.SUB_STORY_POSITION, gson.toJson(subStoryPositionMap)).apply();

        stories.get(position).setUnreadCount(unreadCount);
      } else {
        if (storyUnreadCountMap.containsKey(storyId) && subStoryPositionMap.containsKey(storyId)) {
          int preferencesSubStoryPosition = subStoryPositionMap.get(storyId);

          if (subStoryPosition > preferencesSubStoryPosition) {
            storyUnreadCountMap.put(storyId, unreadCount);
            editor.putString(CleverPushPreferences.STORIES_UNREAD_COUNT, gson.toJson(storyUnreadCountMap)).apply();

            subStoryPositionMap.put(storyId, subStoryPosition);
            editor.putString(CleverPushPreferences.SUB_STORY_POSITION, gson.toJson(subStoryPositionMap)).apply();

            stories.get(position).setUnreadCount(unreadCount);
          }
        } else {
          storyUnreadCountMap.put(storyId, unreadCount);
          editor.putString(CleverPushPreferences.STORIES_UNREAD_COUNT, gson.toJson(storyUnreadCountMap)).apply();

          subStoryPositionMap.put(storyId, subStoryPosition);
          editor.putString(CleverPushPreferences.SUB_STORY_POSITION, gson.toJson(subStoryPositionMap)).apply();

          stories.get(position).setUnreadCount(unreadCount);
        }
      }

    } catch (Exception e) {
      Logger.e(TAG, "Error handling onStoryNavigation in StoryDetailActivity", e);
    }
  }

}
