package com.cleverpush;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import com.cleverpush.listener.AppBannerUrlOpenedListener;

import java.util.Objects;

public class AppBanner {
	private Context context;
	private String id;
	private String content;
	private Dialog dialog;
	private AppBannerUrlOpenedListener urlOpenedListener;

	public AppBanner(Context context, String id, String content, AppBannerUrlOpenedListener urlOpenedListener) {
		this.context = context;
		this.id = id;
		this.content = content;
		this.urlOpenedListener = urlOpenedListener;
	}

	private class AppBannerWebViewClient extends WebViewClient {
		@Override
		public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
			if (url != null && url.startsWith("http")) {
				if (urlOpenedListener != null) {
					urlOpenedListener.opened(url);
				}
                dialog.hide();
			} else {
				super.doUpdateVisitedHistory(view, url, isReload);
			}
		}
	}

	@SuppressLint("SetJavaScriptEnabled")
	public void show() {
        ((Activity) this.context).runOnUiThread(() -> {
			try {
				dialog = new Dialog(this.context);
				dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				dialog.setContentView(R.layout.cleverpush_webview_dialog);
				Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

				Button dialogButton = dialog.findViewById(R.id.btnClose);
				dialogButton.setOnClickListener(v -> this.hide());

				WebView webView = dialog.findViewById(R.id.webView);
				webView.setWebViewClient(new AppBannerWebViewClient());
				webView.getSettings().setLoadsImagesAutomatically(true);
				webView.getSettings().setJavaScriptEnabled(true);
				webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
				webView.loadData(this.content,"text/html", "UTF-8");

				dialog.show();
			} catch (WindowManager.BadTokenException e) {
				// ignored
			}
        });
	}

	public void hide() {
		dialog.dismiss();
	}

	public String getId() {
		return id;
	}
}
