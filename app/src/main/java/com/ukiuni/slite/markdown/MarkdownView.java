package com.ukiuni.slite.markdown;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

/**
 * Created by tito on 2015/11/14.
 */
public class MarkdownView extends WebView {

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

    public void loadMarkdown(String text, String css) {
        this.getSettings().setJavaScriptEnabled(true);
        String html = "<!doctype html><head><style type=\"text/css\">" +
                "<!--\n" +
                css + "\n" +
                "-->\n" +
                "</style><script src=\"https://slite.ukiuni.com/js/marked.js\"></script></head><body><div id=\"article\"></div><script>" +
                "    document.getElementById('article').innerHTML =" +
                "      marked(\"" + text.replace("\"", "\\\"").replace("\r", "").replace("\n", "\\n") + "\");" +
                "  </script>" +
                "</body>" +
                "</html>";
        this.loadData(html, "text/html; charset=utf-8", "UTF-8");
    }
}
