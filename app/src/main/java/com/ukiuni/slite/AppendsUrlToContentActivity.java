package com.ukiuni.slite;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.raizlabs.android.dbflow.sql.language.OrderBy;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.ukiuni.slite.model.Content;
import com.ukiuni.slite.model.Content$Table;
import com.ukiuni.slite.util.Async;

import java.util.List;

public class AppendsUrlToContentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appends_content);
        final EditText editText = (EditText) findViewById(R.id.text);
        final Spinner contentSpinner = (Spinner) findViewById(R.id.contentSpinner);
        Intent intent = getIntent();
        if (intent.getAction().equals(Intent.ACTION_SEND)) {
            String url = intent.getStringExtra(Intent.EXTRA_TEXT);
            String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
            String text = "[" + url + "](" + url + ")\n";
            if (null != subject) {
                text = subject + "\n[" + url + "](" + url + ")\n";
            }
            text += "\n";
            editText.setText(text);
        }
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.app_name);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("", "----------click");
            }
        });

        toolbar.inflateMenu(R.menu.menu_main);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // アイテムクリック時の処理
                return true;
            }
        });
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setHomeButtonEnabled(false);
        toolbar.setTitle("This is title");

        toolbar.setSubtitle("This is sub-title");
        Async.start(new Async.Task() {
            List<Content> contents;

            @Override
            public void work(Async.Handle handle) throws Throwable {
                contents = new Select().from(Content.class).orderBy(OrderBy.columns(Content$Table.CREATEDAT).descending()).queryList();
            }

            @Override
            public void onSuccess() {
                contentSpinner.setAdapter(new ContentSpinnerAdapter(AppendsUrlToContentActivity.this, contents));
            }
        }, R.string.fail_to_get_content);
        Button button = (Button) findViewById(R.id.appendsToContentButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Async.start(new Async.Task() {
                    @Override
                    public void work(Async.Handle handle) throws Throwable {
                        Slite slite = new Slite();
                        Content targetContent = (Content) contentSpinner.getSelectedItem();
                        slite.setHost(targetContent.loadAccount.host);
                        slite.setMyAccount(targetContent.loadAccount);
                        Content content = slite.appendContent(targetContent.accessKey, editText.getText().toString());
                        content.save();
                    }

                    @Override
                    public void onSuccess() {
                        Async.makeToast(R.string.success_to_put_content);
                        finish();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("", "-----------error", e);
                    }
                }, R.string.fail_to_put_content);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private static class ContentSpinnerAdapter extends BaseAdapter {

        private Context context;
        private List<Content> contents;

        public ContentSpinnerAdapter(Context context, List<Content> contents) {
            this.context = context;
            this.contents = contents;
        }

        @Override
        public int getCount() {
            return contents.size();
        }

        @Override
        public Object getItem(int position) {
            return contents.get(position);
        }

        @Override
        public long getItemId(int position) {
            return contents.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                convertView = inflater.inflate(R.layout.myaccount_spinner, null);
            }
            Content content = contents.get(position);
            TextView textView = (TextView) convertView.findViewById(R.id.accountNameText);
            textView.setText(content.title);
            ImageView imageView = (ImageView) convertView.findViewById(R.id.accountIconImage);
            Async.setImage(imageView, content.imageUrl);

            return convertView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getView(position, convertView, parent);
        }
    }
}
