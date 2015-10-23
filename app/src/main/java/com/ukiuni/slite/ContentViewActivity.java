package com.ukiuni.slite;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.raizlabs.android.dbflow.sql.language.Select;
import com.ukiuni.slite.model.Content;
import com.ukiuni.slite.util.Async;
import com.ukiuni.slite.util.ConfirmDialog;

import java.text.SimpleDateFormat;

import us.feras.mdv.MarkdownView;

/**
 * Created by tito on 2015/10/14.
 */
public class ContentViewActivity extends SliteBaseActivity {
    private static final String INTENT_KEY_CONTENT_ACCESS_KEY = "INTENT_KEY_CONTENT_ACCESS_KEY";
    private static final String INTENT_KEY_CONTENT_EDITABLE = "INTENT_KEY_CONTENT_EDITABLE";
    private static final String INTENT_KEY_CONTENT_ID = "INTENT_KEY_CONTENT_ID";
    private Content viewingContent;
    private Content savedContent;

    public static void start(Context context, Content content) {
        Intent intent = new Intent(context, ContentViewActivity.class);
        intent.putExtra(INTENT_KEY_CONTENT_ACCESS_KEY, content.accessKey);
        intent.putExtra(INTENT_KEY_CONTENT_EDITABLE, content.editable);
        intent.putExtra(INTENT_KEY_CONTENT_ID, content.id);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_view);
    }

    @Override
    protected void onResume() {
        super.onResume();
        final TextView titleView = (TextView) findViewById(R.id.titleView);
        final ImageView titleImage = (ImageView) findViewById(R.id.titleImage);
        final MarkdownView markdownView = (MarkdownView) findViewById(R.id.markdownView);
        final ImageView accountIconImage = (ImageView) findViewById(R.id.accountIconImage);
        final TextView dateText = (TextView) findViewById(R.id.dateText);
        final TextView accountNameText = (TextView) findViewById(R.id.accuontNameText);

        Async.start(new Async.Task() {
            Content content;

            @Override
            public void work(Async.Handle handle) throws Throwable {
                String accessKey = getIntent().getStringExtra(INTENT_KEY_CONTENT_ACCESS_KEY);

                savedContent = new Select().from(Content.class).byIds(getIntent().getLongExtra(INTENT_KEY_CONTENT_ID, 0)).querySingle();
                content = SliteApplication.getInstance().getSlite().loadContent(accessKey);
                content.editable = getIntent().getBooleanExtra(INTENT_KEY_CONTENT_EDITABLE, false);
                content.loadAccount = savedContent.loadAccount;
                content.save();
            }

            @Override
            public void onSuccess() {
                titleView.setText(content.title);
                if (null != content.imageUrl) {
                    Async.setImage(titleImage, content.imageUrl, true);
                }
                markdownView.loadMarkdown(content.article, "file:///android_asset/markdown.css");
                Async.setImage(accountIconImage, content.owner.iconUrl);
                accountNameText.setText(content.owner.name);
                dateText.setText(new SimpleDateFormat("yyyy/MM/dd").format(content.updatedAt));
                viewingContent = content;
            }

            @Override
            public void onError(Throwable e) {
                Log.v("", "error ----- ", e);
            }
        }, R.string.fail_to_get_content);
        if (getIntent().getBooleanExtra(INTENT_KEY_CONTENT_EDITABLE, false)) {
            final Button editButton = (Button) findViewById(R.id.editButton);
            editButton.setVisibility(View.VISIBLE);
            editButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ContentEditActivity.start(ContentViewActivity.this, viewingContent);
                }
            });
            final Button deleteButton = (Button) findViewById(R.id.deleteButton);
            deleteButton.setVisibility(View.VISIBLE);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ConfirmDialog dialog = new ConfirmDialog(ContentViewActivity.this, R.string.confirm_delete_content, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Async.start(new Async.Task() {
                                @Override
                                public void work(Async.Handle handle) throws Throwable {
                                    SliteApplication.getInstance().getSlite().deleteContent(viewingContent);
                                    viewingContent.delete();
                                }

                                @Override
                                public void onError(Throwable e) {
                                    Log.e("", "---------", e);
                                }
                            }, R.string.fail_to_delete_content);
                        }
                    });
                    dialog.show();
                }
            });
        }
    }
}
