package com.ukiuni.slite;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.raizlabs.android.dbflow.sql.language.Select;
import com.ukiuni.slite.model.Content;
import com.ukiuni.slite.util.Async;

/**
 * Created by tito on 2015/10/14.
 */
public class ContentEditActivity extends SliteBaseActivity {
    private Content viewingContent;
    private static final String INTENT_KEY_CONTENT_ID = "INTENT_KEY_CONTENT_ID";

    public static void start(Context context, Content content) {
        Intent intent = new Intent(context, ContentEditActivity.class);
        intent.putExtra(INTENT_KEY_CONTENT_ID, content.id);
        context.startActivity(intent);
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, ContentEditActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_edit);
        final EditText titleView = (EditText) findViewById(R.id.titleView);
        final ImageView titleImage = (ImageView) findViewById(R.id.titleImage);
        final EditText articleView = (EditText) findViewById(R.id.markdownView);
        final ImageView accountIconImage = (ImageView) findViewById(R.id.accountIconImage);
        final TextView dateText = (TextView) findViewById(R.id.dateText);
        final TextView accountNameText = (TextView) findViewById(R.id.accuontNameText);
        Content tmpContent = null;
        if (getIntent().hasExtra(INTENT_KEY_CONTENT_ID)) {
            tmpContent = new Select().from(Content.class).byIds(getIntent().getLongExtra(INTENT_KEY_CONTENT_ID, 0)).querySingle();
            titleView.setText(tmpContent.title);
            articleView.setText(tmpContent.article);
        } else {
            tmpContent = new Content();
        }
        final Content content = tmpContent;

        final Button saveButton = (Button) findViewById(R.id.saveButton);
        saveButton.setVisibility(View.VISIBLE);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                content.title = titleView.getText().toString();
                content.article = articleView.getText().toString();
                Async.start(new Async.Task() {
                    Content createdContent;

                    @Override
                    public void work(Async.Handle handle) throws Throwable {
                        if (null != content.accessKey) {
                            Content updatedContent = SliteApplication.getInstance().getSlite().updateContent(content);
                            updatedContent.save();
                        } else {
                            createdContent = SliteApplication.getInstance().getSlite().createContent(content.title, content.article);
                            createdContent.save();
                        }
                    }

                    @Override
                    public void onSuccess() {
                        if (null == content.accessKey) {
                            ContentViewActivity.start(ContentEditActivity.this, createdContent);
                        }
                        finish();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("", "~~~~~~~~~~~", e);
                    }
                }, R.string.fail_to_put_content);
            }
        });

    }
}
