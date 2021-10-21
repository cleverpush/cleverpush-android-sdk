package com.cleverpush.listener;

import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Message;
import android.view.KeyEvent;
import android.webkit.ClientCertRequest;
import android.webkit.HttpAuthHandler;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SafeBrowsingResponse;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

public interface WebViewClientListener {

    void shouldOverrideUrlLoading(WebView view, String url);

    void shouldOverrideUrlLoading(WebView view, WebResourceRequest request);

    void onPageStarted(WebView view, String url, Bitmap favicon);

    void onPageFinished(WebView view, String url);

    void onLoadResource(WebView view, String url);

    void onPageCommitVisible(WebView view, String url);

    void shouldInterceptRequest(WebView view, String url);

    void shouldInterceptRequest(WebView view, WebResourceRequest request);

    void onTooManyRedirects(WebView view, Message cancelMsg, Message continueMsg);

    void onReceivedError(WebView view, int errorCode, String description, String failingUrl);

    void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error);

    void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse);

    void onFormResubmission(WebView view, Message dontResend, Message resend);

    void doUpdateVisitedHistory(WebView view, String url, boolean isReload);

    void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error);

    void onReceivedClientCertRequest(WebView view, ClientCertRequest request);

    void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm);

    void shouldOverrideKeyEvent(WebView view, KeyEvent event);

    void onUnhandledKeyEvent(WebView view, KeyEvent event);

    void onScaleChanged(WebView view, float oldScale, float newScale);

    void onReceivedLoginRequest(WebView view, String realm, String account, String args);

    void onRenderProcessGone(WebView view, RenderProcessGoneDetail detail);

    void onSafeBrowsingHit(WebView view, WebResourceRequest request, int threatType, SafeBrowsingResponse callback);
}
