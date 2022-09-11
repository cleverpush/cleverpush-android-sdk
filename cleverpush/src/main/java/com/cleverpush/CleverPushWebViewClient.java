package com.cleverpush;

import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Message;
import com.cleverpush.util.Logger;
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
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;

import com.cleverpush.listener.WebViewClientListener;

import java.util.Map;

public class CleverPushWebViewClient extends WebViewClient {

    private Map<String, ?> params;
    private WebViewClientListener webViewClientListener;
    private CleverPush cleverPush;

    public CleverPushWebViewClient(Map<String, ?> params, WebViewClientListener webViewClientListener, CleverPush cleverPush) {
        this.params = params;
        this.webViewClientListener = webViewClientListener;
        this.cleverPush = cleverPush;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        webViewClientListener.shouldOverrideUrlLoading(view, url);
        return super.shouldOverrideUrlLoading(view, url);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        webViewClientListener.shouldOverrideUrlLoading(view, request);
        return super.shouldOverrideUrlLoading(view, request);
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        webViewClientListener.onPageStarted(view, url, favicon);
        super.onPageStarted(view, url, favicon);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        webViewClientListener.onPageFinished(view, url);
        super.onPageFinished(view, url);
    }

    @Override
    public void onLoadResource(WebView view, String url) {
        webViewClientListener.onLoadResource(view, url);
        super.onLoadResource(view, url);
    }

    @Override
    public void onPageCommitVisible(WebView view, String url) {
        webViewClientListener.onPageCommitVisible(view, url);
        super.onPageCommitVisible(view, url);
    }

    @Nullable
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        webViewClientListener.shouldInterceptRequest(view, request);
        return super.shouldInterceptRequest(view, request);
    }

    @Nullable
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        webViewClientListener.shouldInterceptRequest(view, url);
        return super.shouldInterceptRequest(view, url);
    }

    @Override
    public void onTooManyRedirects(WebView view, Message cancelMsg, Message continueMsg) {
        webViewClientListener.onTooManyRedirects(view, cancelMsg, continueMsg);
        super.onTooManyRedirects(view, cancelMsg, continueMsg);
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        webViewClientListener.onReceivedError(view, request, error);
        super.onReceivedError(view, request, error);
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        webViewClientListener.onReceivedError(view, errorCode, description, failingUrl);
        super.onReceivedError(view, errorCode, description, failingUrl);
    }

    @Override
    public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
        webViewClientListener.onReceivedHttpError(view, request, errorResponse);
        super.onReceivedHttpError(view, request, errorResponse);
    }

    @Override
    public void onFormResubmission(WebView view, Message dontResend, Message resend) {
        webViewClientListener.onFormResubmission(view, dontResend, resend);
        super.onFormResubmission(view, dontResend, resend);
    }

    @Override
    public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
        Logger.e("MainActivity", url);
        webViewClientListener.doUpdateVisitedHistory(view, url, isReload);
        if (params == null) {
            cleverPush.trackPageView(url);
        } else {
            cleverPush.trackPageView(url, params);
        }
        super.doUpdateVisitedHistory(view, url, isReload);
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        webViewClientListener.onReceivedSslError(view, handler, error);
        super.onReceivedSslError(view, handler, error);
    }

    @Override
    public void onReceivedClientCertRequest(WebView view, ClientCertRequest request) {
        webViewClientListener.onReceivedClientCertRequest(view, request);
        super.onReceivedClientCertRequest(view, request);
    }

    @Override
    public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
        webViewClientListener.onReceivedHttpAuthRequest(view, handler, host, realm);
        super.onReceivedHttpAuthRequest(view, handler, host, realm);
    }

    @Override
    public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
        webViewClientListener.shouldOverrideKeyEvent(view, event);
        return super.shouldOverrideKeyEvent(view, event);
    }

    @Override
    public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
        webViewClientListener.onUnhandledKeyEvent(view, event);
        super.onUnhandledKeyEvent(view, event);
    }

    @Override
    public void onScaleChanged(WebView view, float oldScale, float newScale) {
        webViewClientListener.onScaleChanged(view, oldScale, newScale);
        super.onScaleChanged(view, oldScale, newScale);
    }

    @Override
    public void onReceivedLoginRequest(WebView view, String realm, @Nullable String account, String args) {
        webViewClientListener.onReceivedLoginRequest(view, realm, account, args);
        super.onReceivedLoginRequest(view, realm, account, args);
    }

    @Override
    public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
        webViewClientListener.onRenderProcessGone(view, detail);
        return super.onRenderProcessGone(view, detail);
    }

    @Override
    public void onSafeBrowsingHit(WebView view, WebResourceRequest request, int threatType, SafeBrowsingResponse callback) {
        webViewClientListener.onSafeBrowsingHit(view, request, threatType, callback);
        super.onSafeBrowsingHit(view, request, threatType, callback);
    }
}
