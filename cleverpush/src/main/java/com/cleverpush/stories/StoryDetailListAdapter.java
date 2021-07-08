package com.cleverpush.stories;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.ProgressBar;

import androidx.recyclerview.widget.RecyclerView;

import com.cleverpush.R;
import com.cleverpush.stories.listener.StoryChangeListener;
import com.cleverpush.stories.listener.StoryDetailJavascriptInterface;
import com.cleverpush.stories.models.Story;

import java.util.ArrayList;


public class StoryDetailListAdapter extends RecyclerView.Adapter<ItemViewHolder> {

    private Context context;
    private ArrayList<Story> stories;
    private StoryChangeListener storyChangeListener;

    public StoryDetailListAdapter(Context mContext, ArrayList<Story> stories, StoryChangeListener storyChangeListener) {
        this.context = mContext;
        this.stories = stories;
        this.storyChangeListener = storyChangeListener;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View itemViewStoryDetail = inflater.inflate(R.layout.item_view_story_detail, parent, false);
        return new ItemViewHolder(itemViewStoryDetail);
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        WebView webView = (WebView) holder.itemView.findViewById(R.id.webView);
        ProgressBar progressBar = (ProgressBar) holder.itemView.findViewById(R.id.progressBar);
        int measuredWidth = 0;
        int measuredHeight = 0;
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        measuredWidth = display.getWidth();
        measuredHeight = display.getHeight();

        String htmlContent = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "  <script async src=\"https://cdn.ampproject.org/v0.js\"></script>\n" +
                "  <script\n" +
                "    async\n" +
                "    custom-element=\"amp-story-player\"\n" +
                "    src=\"https://cdn.ampproject.org/v0/amp-story-player-0.1.js\"\n" +
                "  ></script>\n" +
                "</head>\n" +
                "<body>\n" +
                "<amp-story-player layout=\"fixed\" width=" + convertPixelsToDp(measuredWidth, context) + " height=" + convertPixelsToDp(measuredHeight, context) + ">\n" +
                " <a href=\"https://api.cleverpush.com/channel/" + stories.get(position).getChannel() + "/story/" + stories.get(position).getId() + "/html\">\n" +
                "    </a>\n" +
                "  </amp-story-player>\n" +
                "  <script>\n" +
                "    var player = document.querySelector('amp-story-player');\n" +
                "    player.addEventListener('noPreviousStory', function (event) {\n" +
                "      storyDetailJavascriptInterface.previous(" + position + ");" +
                "    });\n" +
                "    player.addEventListener('noNextStory', function (event) {\n" +
                "       storyDetailJavascriptInterface.next(" + position + ");" +
                "    });\n" +
                "  </script>\n" +
                "</body>\n" +
                "</html>";
        String htmlContent2 = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "  <script async src=\"https://cdn.ampproject.org/v0.js\"></script>\n" +
                "  <script\n" +
                "    async\n" +
                "    custom-element=\"amp-story-player\"\n" +
                "    src=\"https://cdn.ampproject.org/v0/amp-story-player-0.1.js\"\n" +
                "  ></script>\n" +
                "</head>\n" +
                "<body>\n" +
                "<amp-story-player layout=\"fixed\" width=" + convertPixelsToDp(measuredWidth, context) + " height=" + convertPixelsToDp(measuredHeight, context) + ">\n" +
                " <a href=\"https://api.cleverpush.com/channel/" + stories.get(position).getChannel() + "/story/" + stories.get(position).getId() + "/html\">\n" +
                "      Story Name\n" +
                "    </a>\n" +
                "  </amp-story-player>\n" +
                "  <script>\n" +
                "    var player = document.querySelector('amp-story-player');\n" +
                "    player.addEventListener('noPreviousStory', function (event) {\n" +
                "      alert('First Page! (Make Native Bridge Calls here)');\n" +
                "    });\n" +
                "    player.addEventListener('noNextStory', function (event) {\n" +
                "      alert('Last Page! (Make Native Bridge Calls here)');\n" +
                "    });\n" +
                "    player.addEventListener('ready', function (event) {\n" +
                "      alert('Ready! (Make Native Bridge Calls here)');\n" +
                "    });\n" +
                "  </script>\n" +
                "</body>\n" +
                "</html>";
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new StoryDetailJavascriptInterface(), "storyDetailJavascriptInterface");
        webView.loadData(htmlContent, "text/html; charset=utf-8", "UTF-8");
    }

    @Override
    public int getItemCount() {
        return stories.size();
    }

    public float convertPixelsToDp(float px, Context context) {
        if (px == 0) {
            return 0;
        }
        return px / ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }
}
