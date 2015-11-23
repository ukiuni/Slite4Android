package com.ukiuni.slite;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.raizlabs.android.dbflow.sql.language.Select;
import com.ukiuni.slite.adapter.ContentArrayAdapter;
import com.ukiuni.slite.model.Content;
import com.ukiuni.slite.model.MyAccount;
import com.ukiuni.slite.util.Async;

import java.util.List;

/**
 * Created by tito on 15/10/10.
 */
public class TopActivity extends SliteBaseActivity {

    private static final String INTENT_KEY_MYACCOUNT_ID = "INTENT_KEY_MYACCOUNT_ID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.top);
    }

    @Override
    protected void onResume() {
        super.onResume();

        final ListView contentListView = (ListView) findViewById(R.id.contentListView);
        final MyAccount myAccount = new Select().from(MyAccount.class).byIds(getIntent().getLongExtra(INTENT_KEY_MYACCOUNT_ID, 0)).querySingle();
        Async.start(new Async.Task() {
            ContentArrayAdapter adapter;

            @Override
            public void work(Async.Handle handle) throws Throwable {
                final List<Content> contents = SliteApplication.getSlite().loadMyContent();
                for (Content content : contents) {
                    content.owner.save();
                    content.loadAccount = myAccount;
                    content.editable = true;
                    content.save();
                }
                adapter = new ContentArrayAdapter(TopActivity.this, contents);
                contentListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        ContentViewActivity.start(TopActivity.this, contents.get(position));
                    }
                });
            }

            @Override
            public void onSuccess() {
                contentListView.setAdapter(adapter);
            }

            @Override
            public void onComplete() {

            }
        });
        Button createContentButton = (Button) findViewById(R.id.createContentButton);
        createContentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ContentEditActivity.start(TopActivity.this);
            }
        });
    }

    public static void start(Context context, long accountId) {
        Intent intent = new Intent(context, TopActivity.class);
        intent.putExtra(INTENT_KEY_MYACCOUNT_ID, accountId);
        context.startActivity(intent);
    }

}