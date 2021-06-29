package com.cleverpush.stories;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.R;
import com.cleverpush.stories.models.Story;
import com.cleverpush.stories.models.StoryListModel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;

public class StoryView extends LinearLayout {
    private Context context;
    private boolean loading = false;
    private static final String TAG = "CleverPush/AppStories";
    private ArrayList<Story> stories = new ArrayList<>();
    StoryViewListAdapter storyViewListAdapter;

    protected static final int DEFAULT_BORDER_COLOR = Color.BLACK;

    protected int borderColor;

    public StoryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        final TypedArray attrArray = getContext().obtainStyledAttributes(attrs, R.styleable.StoryView);
        initAttributes(attrArray);
    }

    private void loadStory() {
        if (loading) {
            return;
        }
        loading = true;

        String storyPath = "/story-widget/o76RepCskiS9QiHsy/config";

        Log.d(TAG, "Loading stories: " + storyPath);

        CleverPushHttpClient.get(storyPath, new CleverPushHttpClient.ResponseHandler() {
            @Override
            public void onSuccess(String response) {
                loading = false;

                GsonBuilder gsonBuilder = new GsonBuilder();
                Gson gson = gsonBuilder.create();
                StoryListModel model = gson.fromJson(response.toString(), StoryListModel.class);
                stories.addAll(model.getStories());
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                for (int i = 0; i < stories.size(); i++) {
                    if (sharedPreferences.getString(CleverPushPreferences.APP_OPENED_STORIES, "").contains(stories.get(i).getId())) {
                        stories.get(i).setOpened(true);
                    } else {
                        stories.get(i).setOpened(false);
                    }
                }
                ((Activity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        displayStoryHead(stories);
                    }
                });
            }

            @Override
            public void onFailure(int statusCode, String response, Throwable throwable) {
                loading = false;
                Log.e(TAG, "Something went wrong when loading stories." +
                        "\nStatus code: " + statusCode +
                        "\nResponse: " + response
                );
            }
        });
    }

    private void displayStoryHead(ArrayList<Story> stories) {
        View root = LayoutInflater.from(context).inflate(R.layout.story_view, this, true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager((Activity) context, LinearLayoutManager.HORIZONTAL, false);
        RecyclerView recyclerView = root.findViewById(R.id.rvStories);
        storyViewListAdapter = new StoryViewListAdapter((Activity) context, stories, borderColor);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(storyViewListAdapter);
        storyViewListAdapter.setOnItemClicked(new StoryViewListAdapter.OnItemClicked() {
            @Override
            public void onItemClicked(int position) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                if (sharedPreferences.getString(CleverPushPreferences.APP_OPENED_STORIES, "").equalsIgnoreCase("")) {
                    sharedPreferences.edit().putString(CleverPushPreferences.APP_OPENED_STORIES, stories.get(position).getId()).apply();
                } else {
                    if (!sharedPreferences.getString(CleverPushPreferences.APP_OPENED_STORIES, "").contains(stories.get(position).getId())) {
                        sharedPreferences.edit().putString(CleverPushPreferences.APP_OPENED_STORIES, sharedPreferences.getString(CleverPushPreferences.APP_OPENED_STORIES, "") + "," + stories.get(position).getId()).apply();
                    }
                }
                StoryDetailActivity.launch((Activity) context, stories, position);
                stories.get(position).setOpened(true);
                storyViewListAdapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(position);
            }
        });
    }

    protected void initAttributes(TypedArray attrArray) {
        borderColor = attrArray.getColor(R.styleable.StoryView_border_color, DEFAULT_BORDER_COLOR);
        loadStory();
    }
}