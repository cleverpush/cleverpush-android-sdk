package com.cleverpush.stories;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.R;
import com.cleverpush.stories.listener.OnItemClickListener;
import com.cleverpush.stories.models.Story;
import com.cleverpush.stories.models.StoryListModel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;

public class StoryView extends LinearLayout {

    protected static final int DEFAULT_BACKGROUND_COLOR = Color.WHITE;
    private static final String TAG = "CleverPush/AppStories";

    TypedArray attrArray;
    StoryViewListAdapter storyViewListAdapter;

    private Context context;
    private boolean loading = false;
    private ArrayList<Story> stories = new ArrayList<>();

    public StoryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        attrArray = getContext().obtainStyledAttributes(attrs, R.styleable.StoryView);
        loadStory();
    }

    private void loadStory() {
        if (loading) {
            return;
        }
        loading = true;

        String storyPath = "/story-widget/"+attrArray.getString(R.styleable.StoryView_widget_id)+"/config";

        Log.d(TAG, "Loading stories: " + storyPath);

        CleverPushHttpClient.get(storyPath, loadStoriesResponseHandler());
    }

    private CleverPushHttpClient.ResponseHandler loadStoriesResponseHandler() {
        return new CleverPushHttpClient.ResponseHandler() {
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
        };
    }

    private void displayStoryHead(ArrayList<Story> stories) {
        View root = LayoutInflater.from(context).inflate(R.layout.story_view, this, true);

        RelativeLayout rlMain = root.findViewById(R.id.rlMain);
        rlMain.setBackgroundColor(attrArray.getColor(R.styleable.StoryView_background_color, DEFAULT_BACKGROUND_COLOR));
        ViewGroup.LayoutParams params = rlMain.getLayoutParams();
        params.height = (int) attrArray.getDimension(R.styleable.StoryView_story_view_height, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.width = (int) attrArray.getDimension(R.styleable.StoryView_story_view_width, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlMain.setLayoutParams(params);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager((Activity) context, LinearLayoutManager.HORIZONTAL, false);
        RecyclerView recyclerView = root.findViewById(R.id.rvStories);
        storyViewListAdapter = new StoryViewListAdapter((Activity) context, stories, attrArray, getOnItemClickListener(stories, recyclerView));
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(storyViewListAdapter);
    }

    private OnItemClickListener getOnItemClickListener(ArrayList<Story> stories, RecyclerView recyclerView) {
        return new OnItemClickListener() {
            @Override
            public void onClicked(int position) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                if (sharedPreferences.getString(CleverPushPreferences.APP_OPENED_STORIES, "").isEmpty()) {
                    editor.putString(CleverPushPreferences.APP_OPENED_STORIES, stories.get(position).getId()).apply();
                } else {
                    if (!sharedPreferences.getString(CleverPushPreferences.APP_OPENED_STORIES, "").contains(stories.get(position).getId())) {
                        editor.putString(CleverPushPreferences.APP_OPENED_STORIES, sharedPreferences.getString(CleverPushPreferences.APP_OPENED_STORIES, "") + "," + stories.get(position).getId()).apply();
                    }
                }
                StoryDetailActivity.launch((Activity) context, stories, position);
                stories.get(position).setOpened(true);
                storyViewListAdapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(position);
            }

        };
    }

}
