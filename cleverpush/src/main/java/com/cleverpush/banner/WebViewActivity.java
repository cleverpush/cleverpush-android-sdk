package com.cleverpush.banner;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;

import com.cleverpush.R;

public class WebViewActivity extends Activity {

    private String url;

    public static void launch(Activity activity, String url) {
        Intent intent = new Intent(activity, WebViewActivity.class);
        intent.putExtra("url", url);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        this.getActionBar().hide();
        handleBundleData(getIntent().getExtras());

    }

    private void handleBundleData(Bundle extras) {
        if (extras.containsKey("url")) {
            url = extras.getString("url");
            init();
        }
    }

    private void init() {
        WebView webView = findViewById(R.id.webView);
        ImageView closeButton = findViewById(R.id.ivClose);
        webView.loadUrl(url);

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
}
