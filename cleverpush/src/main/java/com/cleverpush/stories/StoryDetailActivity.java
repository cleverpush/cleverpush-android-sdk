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

import com.cleverpush.R;
import com.cleverpush.stories.listener.OnSwipeDownListener;
import com.cleverpush.stories.listener.OnSwipeTouchListener;
import com.cleverpush.stories.listener.StoryChangeListener;
import com.cleverpush.stories.models.Story;

import java.util.ArrayList;

public class StoryDetailActivity extends Activity implements StoryChangeListener {

    private int selectedPosition = 0;
    private RecyclerView recyclerView;
    private OnSwipeTouchListener onSwipeTouchListener;
    private ArrayList<Story> stories = new ArrayList<>();

    public static void launch(Activity activity, ArrayList<Story> stories, int selectedPosition) {
        Intent intent = new Intent(activity, StoryDetailActivity.class);
        intent.putExtra("stories", stories);
        intent.putExtra("selectedPosition", selectedPosition);
        activity.startActivity(intent);
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
        if (extras != null) {
            if (extras.containsKey("selectedPosition")) {
                selectedPosition = extras.getInt("selectedPosition");
            }
            if (extras.containsKey("stories")) {
                stories = (ArrayList<Story>) extras.getSerializable("stories");
                loadStoryDetails();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(0, R.anim.slide_down);
    }

    private void loadStoryDetails() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        SnapHelper snapHelper = new PagerSnapHelper();
        StoryDetailListAdapter storyDetailListAdapter = new StoryDetailListAdapter(this, stories, this);
        recyclerView.setLayoutManager(linearLayoutManager);
        snapHelper.attachToRecyclerView(recyclerView);
        recyclerView.setAdapter(storyDetailListAdapter);
        recyclerView.smoothScrollToPosition(selectedPosition);
    }

    @Override
    public void onNext(int position) {
        if (position != stories.size() - 1) {
            recyclerView.smoothScrollToPosition(position + 1);
        }
    }

    @Override
    public void onPrevious(int position) {
        if (position != 0) {
            recyclerView.smoothScrollToPosition(position - 1);
        }
    }
}
