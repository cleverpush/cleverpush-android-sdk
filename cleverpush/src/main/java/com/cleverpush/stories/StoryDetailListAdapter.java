package com.cleverpush.stories;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.cleverpush.R;
import com.cleverpush.stories.listener.StoryChangeListener;
import com.cleverpush.stories.listener.StoryDetailJavascriptInterface;
import com.cleverpush.stories.models.Story;

import java.util.ArrayList;

public class StoryDetailListAdapter extends RecyclerView.Adapter<StoryDetailViewHolder> {

  private Activity activity;
  private ArrayList<Story> stories;
  private StoryChangeListener storyChangeListener;

  public StoryDetailListAdapter(Activity activity, ArrayList<Story> stories, StoryChangeListener storyChangeListener) {
    this.activity = activity;
    this.stories = stories;
    this.storyChangeListener = storyChangeListener;
  }

  @Override
  public StoryDetailViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(activity);
    View itemViewStoryDetail = inflater.inflate(R.layout.item_view_story_detail, parent, false);
    return new StoryDetailViewHolder(itemViewStoryDetail);
  }

  @Override
  public void onBindViewHolder(@NonNull StoryDetailViewHolder storyDetailViewHolder, int position) {
    storyDetailViewHolder.setIsRecyclable(false);
    storyDetailViewHolder.progressBar.setVisibility(View.VISIBLE);
    storyDetailViewHolder.webView.setWebViewClient(new WebViewClient() {
      @Override
      public void onPageStarted(WebView view, String url, Bitmap favicon) {
        // Show the progress bar when the page starts loading
        storyDetailViewHolder.progressBar.setVisibility(View.VISIBLE);
      }

      @Override
      public void onPageFinished(WebView view, String url) {
        // Hide the progress bar when the page finishes loading
        if (position == StoryDetailActivity.selectedPosition) {
          storyDetailViewHolder.progressBar.setVisibility(View.GONE);
        }
      }
    });

    int measuredWidth = 0;
    int measuredHeight = 0;
    WindowManager windowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
    Display display = windowManager.getDefaultDisplay();
    measuredWidth = display.getWidth();
    measuredHeight = display.getHeight();

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
        " <a href=\"https://api.cleverpush.com/channel/" + stories.get(position).getChannel() + "/story/" + stories.get(
        position).getId() + "/html\">\n" +
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
        "  </script>\n" +
        "</body>\n" +
        "</html>";

    storyDetailViewHolder.webView.getSettings().setJavaScriptEnabled(true);
    storyDetailViewHolder.webView.addJavascriptInterface(
        new StoryDetailJavascriptInterface(storyDetailViewHolder, storyChangeListener, activity),
        "storyDetailJavascriptInterface");

    /*// solution 1
    String encodedHtml = Base64.encodeToString(htmlContent.getBytes(),
            Base64.NO_PADDING);
    storyDetailViewHolder.webView.loadData(encodedHtml, "text/html", "base64");*/

    /*// solution 2
    String header = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>";
    storyDetailViewHolder.webView.loadData(header + htmlContent, "text/html", "UTF-8");*/

    /*// solution 3
    WebSettings settings = storyDetailViewHolder.webView.getSettings();
    settings.setDefaultTextEncodingName("utf-8");
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
      String base64 = Base64.encodeToString(htmlContent.getBytes(), Base64.DEFAULT);
      storyDetailViewHolder.webView.loadData(base64, "text/html; charset=utf-8", "base64");
    } else {
      String header = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>";
      storyDetailViewHolder.webView.loadData(header + htmlContent, "text/html; charset=UTF-8", null);
    }*/

    /*// solution 4
    WebSettings settings = storyDetailViewHolder.webView.getSettings();
    settings.setDefaultTextEncodingName("utf-8");
//    storyDetailViewHolder.webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null);
    storyDetailViewHolder.webView.loadDataWithBaseURL(null,htmlContent,"text/html; charset=utf-8", "UTF-8", null);*/

    // solution 5
    storyDetailViewHolder.webView.setWebViewClient(new WebViewClient() {
      @Override
      public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        // Handle URL requests here
        String url = request.getUrl().toString();
        // Otherwise, load the URL in the WebView
        view.loadUrl(url);
        return true;
      }

      @Override
      public void onPageFinished(WebView view, String url) {
        // HTML data has finished loading
        super.onPageFinished(view, url);
        if (position == StoryDetailActivity.selectedPosition) {
          storyDetailViewHolder.progressBar.setVisibility(View.GONE);
        }
      }

      @Override
      public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        // Handle errors here
        super.onReceivedError(view, request, error);
      }
    });

    String mimeType = "text/html";
    String encoding = "UTF-8";

    storyDetailViewHolder.webView.loadData(htmlContent, mimeType, encoding);

    /*// original code
    storyDetailViewHolder.webView.loadData(htmlContent, "text/html; charset=utf-8", "UTF-8");*/
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
}
