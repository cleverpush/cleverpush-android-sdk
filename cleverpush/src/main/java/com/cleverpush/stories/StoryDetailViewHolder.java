package com.cleverpush.stories;

import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;

import androidx.recyclerview.widget.RecyclerView;

import com.cleverpush.R;

public class StoryDetailViewHolder extends RecyclerView.ViewHolder {

  public WebView webView;
  public ProgressBar progressBar;

  public StoryDetailViewHolder(View itemView) {
    super(itemView);
    webView = (WebView) itemView.findViewById(R.id.webView);
    progressBar = (ProgressBar) itemView.findViewById(R.id.progressBar);
  }

}
