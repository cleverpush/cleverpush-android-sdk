package com.cleverpush.stories;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cleverpush.ActivityLifecycleListener;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.R;
import com.cleverpush.listener.StoryViewOpenedListener;
import com.cleverpush.responsehandlers.TrackStoryOpenedShownResponseHandler;
import com.cleverpush.stories.listener.StoryChangeListener;
import com.cleverpush.stories.listener.StoryDetailJavascriptInterface;
import com.cleverpush.stories.models.Story;
import com.cleverpush.util.Logger;
import com.cleverpush.util.SharedPreferencesManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

public class StoryDetailListAdapter extends RecyclerView.Adapter<StoryDetailViewHolder> {

  private Activity activity;
  private ArrayList<Story> stories;
  private StoryChangeListener storyChangeListener;
  public StoryViewOpenedListener storyViewOpenedListener;
  int subStoryPosition = 0;
  boolean isHideStoryShareButton = false;
  String widgetId = null;
  private static final String TAG = "CleverPush/StoryDetailAdapter";
  private StoryDetailViewHolder storyDetailViewHolder;
  int measuredWidth = 0;
  int measuredHeight = 0;
  int selectedPosition = 0;

  public StoryDetailListAdapter(Activity activity, ArrayList<Story> stories, StoryChangeListener storyChangeListener,
                                StoryViewOpenedListener storyViewOpenedListener, int subStoryPosition, boolean isHideStoryShareButton,
                                String widgetId, int selectedPosition) {
    this.activity = activity;
    this.stories = stories;
    this.storyChangeListener = storyChangeListener;
    this.storyViewOpenedListener = storyViewOpenedListener;
    this.subStoryPosition = subStoryPosition;
    this.isHideStoryShareButton = isHideStoryShareButton;
    this.widgetId = widgetId;
    this.selectedPosition = selectedPosition;
  }

  @Override
  public StoryDetailViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(activity);
    View itemViewStoryDetail = inflater.inflate(R.layout.item_view_story_detail, parent, false);
    return new StoryDetailViewHolder(itemViewStoryDetail);
  }

  @Override
  public void onBindViewHolder(@NonNull StoryDetailViewHolder storyDetailViewHolder, int position) {
    try {
      this.storyDetailViewHolder = storyDetailViewHolder;
      storyDetailViewHolder.setIsRecyclable(false);

      WindowManager windowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
      Display display = windowManager.getDefaultDisplay();
      measuredWidth = display.getWidth();
      measuredHeight = display.getHeight();

      storyDetailViewHolder.webView.getSettings().setJavaScriptEnabled(true);
      storyDetailViewHolder.webView.getSettings().setLoadsImagesAutomatically(true);
      storyDetailViewHolder.webView.addJavascriptInterface(
              new StoryDetailJavascriptInterface(storyDetailViewHolder, storyChangeListener, activity),
              "storyDetailJavascriptInterface");

      storyDetailViewHolder.webView.setWebViewClient(new StoryViewWebViewClient(storyViewOpenedListener) {
        @Override
        public void onPageFinished(WebView view, String url) {
          super.onPageFinished(view, url);

          // Call the trackStoriesShown method when content is fully rendered
          if (selectedPosition == position) {
            trackStoriesShown(position);
          }
        }
      });
      storyDetailViewHolder.webView.loadData(loadHtml(position, subStoryPosition), "text/html; charset=utf-8", "UTF-8");
    } catch (Exception e) {
      Logger.e(TAG, "Error in StoryDetail onBindViewHolder.", e);
    }
  }

  public String loadHtml(int position, int subStoryPosition) {
    String storyId = stories.get(position).getId();

    String htmlContent = "<!DOCTYPE html>\n" +
        "<html>\n" +
        "<head>\n" +
        "  <script     async src=\"https://cdn.ampproject.org/v0.js\"></script>\n" +
        "  <script\n" +
        "    async\n" +
        "    custom-element=\"amp-story-player\"\n" +
        "    src=\"https://cdn.ampproject.org/v0/amp-story-player-0.1.js\"\n" +
        "  ></script>\n" +
        "</head>\n" +
        "<body>\n" +
        "<amp-story-player layout=\"fixed\" width=" + convertPixelsToDp(measuredWidth, activity) + " height="
        + convertPixelsToDp(measuredHeight, activity) + ">\n" +
        "<a href=\"https://api.cleverpush.com/channel/" + stories.get(position).getChannel() + "/story/" + storyId+
        "/html?hideStoryShareButton=" + isHideStoryShareButton + "&widgetId=" + widgetId + "&%23page=page-" + subStoryPosition + "\">\n" +
        "    </a>\n" +
        "  </amp-story-player>\n" +
        "  <script>\n" +
        "    var player = document.querySelector('amp-story-player');\n" +
        "    player.addEventListener('noPreviousStory', function (event) {\n" +
        "      storyDetailJavascriptInterface.previous(" + position + ");" +
        "    });\n" +
        "    player.addEventListener('noNextStory', function (event) {\n" +
        "      storyDetailJavascriptInterface.next(" + position + ");" +
        "    });\n" +
        "    player.addEventListener('ready', function (event) {\n" +
        "       storyDetailJavascriptInterface.ready();" +
        "    });\n" +
        "    player.addEventListener('storyNavigation', function (event) {\n" +
        "      storyDetailJavascriptInterface.storyNavigation(" + position + ", Number(event.detail.pageId?.split('-')?.[1]));\n" +
        "    });\n" +
        "  </script>\n" +
        "</body>\n" +
        "</html>";

    return htmlContent;
  }

  public void refresh (int position, int subStoryPosition) {
    ActivityLifecycleListener.currentActivity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        storyDetailViewHolder.webView.loadData(loadHtml(position, subStoryPosition), "text/html; charset=utf-8", "UTF-8");
      }
    });
  }

  @Override
  public int getItemCount() {
    return stories.size();
  }

  public float convertPixelsToDp(float px, Context context) {
    if (px == 0) {
      return 0f;
    }
    return px / ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public int getItemViewType(int position) {
    return position;
  }

  private void trackStoriesShown(int position) {
    if (widgetId == null || widgetId.length() == 0) {
      return;
    }

    if (selectedPosition == position) {
      String storyPath = "/story-widget/" + widgetId + "/track-shown";

      SharedPreferences sharedPreferences = SharedPreferencesManager.getSharedPreferences(activity.getApplicationContext());
      String storyOpenPreferencesString = sharedPreferences.getString(CleverPushPreferences.APP_OPENED_STORIES, "");

      String currentStoryId = stories.get(position).getId();

      if (storyOpenPreferencesString.contains(currentStoryId)) {

        ArrayList<String> storyIds = new ArrayList<>();
        if (!storyOpenPreferencesString.isEmpty()) {
          storyIds = new ArrayList<>(Arrays.asList(storyOpenPreferencesString.split(",")));
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
    }
  }

}
