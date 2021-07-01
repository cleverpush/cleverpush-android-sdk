package com.cleverpush.stories;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.cleverpush.R;
import com.cleverpush.stories.models.Story;

import java.util.ArrayList;


public class StoryDetailListAdapter extends RecyclerView.Adapter<StoryDetailListAdapter.ItemViewHolder> {

    private Context mContext;
    private ArrayList<Story> stories;
    private OnNextEventListener onNextEventListener;
    private OnPreviousEventListener onPreviousEventListener;
    boolean isRedirected;

    public StoryDetailListAdapter(Context mContext, ArrayList<Story> stories) {
        this.mContext = mContext;
        this.stories = stories;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View itemViewStoryDetail = inflater.inflate(R.layout.item_view_story_detail, parent, false);
        return new ItemViewHolder(itemViewStoryDetail);
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        WebView webView = (WebView) holder.itemView.findViewById(R.id.webView);
        ProgressBar progressBar = (ProgressBar) holder.itemView.findViewById(R.id.progressBar);
        int measuredWidth = 0;
        int measuredHeight = 0;
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
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
                "<amp-story-player layout=\"fixed\" width=" + convertPixelsToDp(measuredWidth, mContext) + " height=" + convertPixelsToDp(measuredHeight, mContext) + ">\n" +
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
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new StoryDetailJavascriptInterface(), "storyDetailJavascriptInterface");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (view.getProgress() == 100) {
                    progressBar.setVisibility(View.GONE);
                }

            }
        });
        webView.loadData(htmlContent, "text/html; charset=utf-8", "UTF-8");
    }

    @Override
    public int getItemCount() {
        return stories.size();
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        public ItemViewHolder(View itemView) {
            super(itemView);
        }

    }

    public interface OnNextEventListener {
        void onNextEventListener(int position);
    }

    public interface OnPreviousEventListener {
        void onPreviousEventListener(int position);
    }

    public void setOnNextEventListener(OnNextEventListener onNextEventListener) {
        this.onNextEventListener = onNextEventListener;
    }

    public void setOnPreviousEventListener(OnPreviousEventListener onPreviousEventListener) {
        this.onPreviousEventListener = onPreviousEventListener;
    }

    /**
     * Will provide javascript bridge to perform close button click in HTML.
     */
    public class StoryDetailJavascriptInterface {
        @JavascriptInterface
        public void next(int position) {
            Log.e("position", position + "");
            onNextEventListener.onNextEventListener(position);
        }

        public void previous(int position) {
            onPreviousEventListener.onPreviousEventListener(position);
        }
    }

    public static float convertPixelsToDp(float px, Context context) {
        return px / ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }
}
