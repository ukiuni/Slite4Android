package com.ukiuni.slite;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import com.ukiuni.slite.util.IO;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

public class AppendsUrlToContentActivity extends AppCompatActivity {
    private Bitmap bitmapForView;
    private boolean isMovie;
    File cachedFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_appends_content);
        final EditText editText = (EditText) findViewById(R.id.text);
        final ImageView imageView = (ImageView) findViewById(R.id.image);
        final Spinner contentSpinner = (Spinner) findViewById(R.id.contentSpinner);
        Intent intent = getIntent();
        if (intent.getAction().equals(Intent.ACTION_SEND)) {
            String url = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (null != url) {
                String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
                String text = "[" + url + "](" + url + ")\n";
                if (null != subject) {
                    text = subject + "\n[" + url + "](" + url + ")\n";
                }
                text += "\n";
                editText.setText(text);
                editText.setVisibility(View.VISIBLE);
            }
            Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (imageUri != null) {
                try {
                    int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
                    if (PackageManager.PERMISSION_GRANTED != permissionCheck) {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                0);
                        finish();
                        return;
                    }
                    File file = new File(IO.getPath(getApplication(), imageUri));
                    cachedFile = new File(getCacheDir(), file.getName());
                    IO.copy(file, cachedFile);
                    bitmapForView = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

                    if (bitmapForView != null) {
                        imageView.setImageBitmap(bitmapForView);
                        imageView.setVisibility(View.VISIBLE);
                    } else {
                        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                        retriever.setDataSource(this, imageUri);
                        bitmapForView = retriever.getFrameAtTime(0);

                        imageView.setImageBitmap(bitmapForView);
                        imageView.setVisibility(View.VISIBLE);
                        isMovie = true;
                    }
                } catch (Exception e) {
                    Log.d("", "fail to save to cache", e);
                    Async.makeToast(R.string.fail_to_load_image);
                }
            }
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
                contents = new Select().from(Content.class).orderBy(OrderBy.columns(Content$Table.UPDATEDAT).descending()).queryList();
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
                finish();
                Async.start(new Async.Task() {
                    @Override
                    public void work(Async.Handle handle) throws Throwable {
                        Slite slite = new Slite();
                        Content targetContent = (Content) contentSpinner.getSelectedItem();
                        slite.setHost(targetContent.loadAccount.host);
                        slite.setMyAccount(targetContent.loadAccount);
                        if (null != bitmapForView) {
                            int w = bitmapForView.getWidth();
                            int h = bitmapForView.getHeight();
                            float scale = Math.max((float) 500 / w, (float) 500 / h);
                            int size = Math.min(w, h);
                            Matrix matrix = new Matrix();
                            matrix.postScale(scale, scale);
                            Bitmap thumbnail = Bitmap.createBitmap(bitmapForView, (w - size) / 2, (h - size) / 2, size, size, matrix, true);
                            if (isMovie) {
                                Canvas canvas = new Canvas(thumbnail);
                                int circleX = thumbnail.getWidth() / 10 * 9;
                                int circleY = thumbnail.getHeight() / 10 * 9;
                                Bitmap playBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.play);
                                canvas.drawBitmap(playBitmap, new Rect(0, 0, playBitmap.getWidth(), playBitmap.getHeight()), new RectF(circleX, circleY, thumbnail.getWidth(), thumbnail.getHeight()), null);
                                playBitmap.recycle();
                            }
                            String tumbnailUrl = slite.uploadImage(targetContent.accessKey, thumbnail);

                            try (FileInputStream fileIn = new FileInputStream(cachedFile)) {
                                final long fileSize = cachedFile.length();
                                String contentUrl = slite.uploadImage(targetContent.accessKey, fileIn, cachedFile.getName(), new Slite.Progress() {
                                    @Override
                                    public void sended(int current) {
                                        if (status != null) {
                                            status.increaseProgress((int) (current / fileSize));
                                        }
                                    }
                                });
                                editText.setText("[![" + cachedFile.getName() + "](" + tumbnailUrl + "){col-xs-12}](" + contentUrl + "){targetOverray col-xs-12 col-sm-3}");
                            }

                            cachedFile.delete();
                            cachedFile = null;
                        }
                        Content content = slite.appendContent(targetContent.accessKey, editText.getText().toString());
                        content.save();
                    }

                    private Async.Status status;

                    @Override
                    public void preExecute() {
                        if (null != cachedFile) {
                            status = Async.showNotifiction(R.string.uploading, cachedFile.getName());
                        }
                    }

                    @Override
                    public void onSuccess() {
                        Async.makeToast(R.string.success_to_put_content);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Async.makeToast(R.string.fail_to_put_content);
                        Intent intent = new Intent(AppendsUrlToContentActivity.this, AppendsUrlToContentActivity.class);
                        intent.setAction(Intent.ACTION_SEND);
                        intent.putExtra(Intent.EXTRA_TEXT, editText.getText().toString());
                        startActivity(intent);
                    }

                    @Override
                    public void onComplete() {
                        if (status != null) {
                            status.increaseProgress(100);
                            status.updateTitle(R.string.success_to_put_content);
                        }
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
