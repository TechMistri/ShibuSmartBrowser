package com.zyphora.browser;

import android.webkit.WebView;

public class Tab {
    private WebView webView;
    private String url;
    private String title;

    public Tab(WebView webView, String url, String title) {
        this.webView = webView;
        this.url = url;
        this.title = title;
    }

    public WebView getWebView() {
        return webView;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
