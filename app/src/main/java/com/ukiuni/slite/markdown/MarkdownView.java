package com.ukiuni.slite.markdown;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.ukiuni.slite.util.IO;

/**
 * Created by tito on 2015/11/14.
 */
public class MarkdownView extends WebView {
    public static String CACHED_CSS = null;
    public static String CACHED_MARDKE = null;

    public MarkdownView(Context context) {
        super(context);
    }

    public MarkdownView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MarkdownView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MarkdownView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void loadMarkdown(String text) {
        if (null == CACHED_CSS) {
            CACHED_CSS = IO.assetToString(getContext().getAssets(), "markdown.css");
        }
        loadMarkdown(text, CACHED_CSS, null);
    }

    public void loadMarkdown(String text, OnLoadedListener loadedListener) {
        if (null == CACHED_CSS) {
            CACHED_CSS = IO.assetToString(getContext().getAssets(), "markdown.css");
        }
        loadMarkdown(text, CACHED_CSS, loadedListener);
    }

    public static interface OnLoadedListener {
        public void onLoaded();
    }

    public void loadMarkdown(String text, String css, final OnLoadedListener loadedListener) {
        if (null == CACHED_MARDKE) {
            CACHED_MARDKE = IO.assetToString(getContext().getAssets(), "marked.js");
        }
        this.getSettings().setJavaScriptEnabled(true);
        this.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (null != loadedListener) {
                    loadedListener.onLoaded();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Uri uri = Uri.parse(url);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                getContext().startActivity(intent);
                return true;
            }
        });
        String html = "<!doctype html><head><style type=\"text/css\">" +
                "<!--\n" +
                css + "\n" +
                "-->\n" +
                "</style>" +
                "<script>var isElectron = false;</script>" + //src="https://slite.ukiuni.com/js/marked.js
                "<script>" + CACHED_MARDKE + "</script></head><body><div id=\"article\"></div><script>" +
                "    document.getElementById('article').innerHTML =" +
                "      marked(\"" + text.replace("\"", "\\\"").replace("\r", "").replace("\n", "\\n") + "\");" +
                "  </script>" +
                "</body>" +
                "</html>";
        this.loadDataWithBaseURL(null, html, "text/html; charset=utf-8", "UTF-8", null);

    }
}
