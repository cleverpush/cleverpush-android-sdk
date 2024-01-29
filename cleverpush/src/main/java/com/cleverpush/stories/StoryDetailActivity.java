package com.cleverpush.stories;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.ImageView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.cleverpush.ActivityLifecycleListener;
import com.cleverpush.R;
import com.cleverpush.listener.StoryViewOpenedListener;
import com.cleverpush.stories.listener.OnSwipeDownListener;
import com.cleverpush.stories.listener.OnSwipeTouchListener;
import com.cleverpush.stories.listener.StoryChangeListener;
import com.cleverpush.stories.models.Story;
import com.cleverpush.util.Logger;

import java.util.ArrayList;

public class StoryDetailActivity extends Activity implements StoryChangeListener {

  public static int selectedPosition = 0;
  private RecyclerView recyclerView;
  private OnSwipeTouchListener onSwipeTouchListener;
  private ArrayList<Story> stories = new ArrayList<>();
  public StoryViewOpenedListener storyViewOpenedListener;
  private static final String TAG = "CleverPush/AppStoryDetails";

  public static void launch(Activity activity, ArrayList<Story> stories, int selectedPosition, StoryViewOpenedListener storyViewOpenedListener) {
    try {
      ActivityLifecycleListener.currentActivity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          Intent intent = new Intent(activity, StoryDetailActivity.class);
          intent.putExtra("stories", stories);
          intent.putExtra("selectedPosition", selectedPosition);
          intent.putExtra("storyViewOpenedListener", storyViewOpenedListener);
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
    ImageView closeButton = findViewById(R.id.ivClose);
    handleBundleData(getIntent().getExtras());
    closeButton.setOnClickListener(view -> finish());
    onSwipeTouchListener = new OnSwipeTouchListener(this, recyclerView, new OnSwipeDownListener() {
      @Override
      public void onSwipeDown() {
        finish();
      }
    });
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
      LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
      SnapHelper snapHelper = new PagerSnapHelper();
      StoryDetailListAdapter storyDetailListAdapter = new StoryDetailListAdapter(this, stories, this, storyViewOpenedListener);
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
      }
    } catch (Exception e) {
      Logger.e(TAG, "Error handling onPrevious in StoryDetailActivity", e);
    }
  }
}
