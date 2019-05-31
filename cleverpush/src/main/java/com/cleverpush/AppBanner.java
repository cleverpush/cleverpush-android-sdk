package com.cleverpush;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.widget.Button;

import java.util.Objects;

public class AppBanner {
	private Context context;
	private String id;
	private String content;
	private Dialog dialog;

	public AppBanner(Context context, String id, String content) {
		this.context = context;
		this.id = id;
		this.content = content;
	}

	@SuppressLint("SetJavaScriptEnabled")
	public void show() {
		dialog = new Dialog(this.context);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.cleverpush_webview_dialog);
		Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

		Button dialogButton = dialog.findViewById(R.id.btnClose);
		dialogButton.setOnClickListener(v -> this.hide());

		WebView webView = dialog.findViewById(R.id.webView);
		webView.getSettings().setLoadsImagesAutomatically(true);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
		webView.loadData(this.content,"text/html", "UTF-8");

		dialog.show();
	}

	public void hide() {
		dialog.dismiss();
	}

	public String getId() {
		return id;
	}
}
