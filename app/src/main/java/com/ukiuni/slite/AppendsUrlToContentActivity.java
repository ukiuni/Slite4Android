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
import android.media.ExifInterface;
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
import com.ukiuni.slite.exception.PermissionException;
import com.ukiuni.slite.model.Calendar;
import com.ukiuni.slite.model.Content;
import com.ukiuni.slite.model.Content$Table;
import com.ukiuni.slite.util.Async;
import com.ukiuni.slite.util.IO;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class AppendsUrlToContentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appends_content);
        final EditText editText = (EditText) findViewById(R.id.text);
        final ImageView imageView = (ImageView) findViewById(R.id.image);
        final Spinner contentSpinner = (Spinner) findViewById(R.id.contentSpinner);

        final List<UploadSet> uploadSets = new ArrayList<>();

        Intent intent = getIntent();
        if (intent.getAction().equals(Intent.ACTION_SEND)) {
            String url = intent.getStringExtra(Intent.EXTRA_TEXT);
            Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (null != url) {
                String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
                String text = "[" + url + "](" + url + ")\n";
                if (null != subject) {
                    text = subject + "\n[" + url + "](" + url + ")\n";
                }
                text += "\n";
                editText.setText(text);
                editText.setVisibility(View.VISIBLE);
                uploadSets.add(new UploadSet());
            } else if (imageUri != null) {
                try {
                    UploadSet uploadSet = new UploadSet().readAndCreate(imageUri);
                    uploadSets.add(uploadSet);
                    imageView.setImageBitmap(uploadSet.getBitmapForView());
                    imageView.setVisibility(View.VISIBLE);
                } catch (PermissionException | IOException e) {
                    finish();
                    return;
                }
            }
        } else if (intent.getAction().equals(Intent.ACTION_SEND_MULTIPLE)) {
            List<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            for (Uri imageUri : imageUris) {
                try {
                    UploadSet uploadSet = new UploadSet().readAndCreate(imageUri);
                    uploadSets.add(uploadSet);
                    imageView.setImageBitmap(uploadSet.getBitmapForView());
                    imageView.setVisibility(View.VISIBLE);
                } catch (PermissionException | IOException e) {
                    finish();
                    return;
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
                Content targetContent = (Content) contentSpinner.getSelectedItem();
                uploadFileAsync(targetContent, uploadSets, editText);
            }
        });
    }

    public static interface ToStringer {
        public void append(File file, String tumbnailUrl, String fileUrl);

        public Content send(Slite slite) throws IOException;
    }

    protected void uploadFileAsync(final Content targetContent, final List<UploadSet> uploadSets, final EditText editText) {
        Async.start(new Async.Task() {
            @Override
            public void work(Async.Handle handle) throws Throwable {
                Slite slite = new Slite();
                slite.setHost(targetContent.loadAccount.host);
                slite.setMyAccount(targetContent.loadAccount);
                final boolean isCalendar = Content.TYPE_CALENDAR.equals(targetContent.type);
                ToStringer toStringer;
                if (null == uploadSets.get(0).bitmapCacheFile) {
                    toStringer = new ToStringer() {
                        @Override
                        public void append(File file, String tumbnailUrl, String fileUrl) {
                        }

                        @Override
                        public Content send(Slite slite) throws IOException {
                            return slite.appendContent(targetContent.accessKey, editText.getText().toString());
                        }
                    };
                } else if (isCalendar) {
                    final Calendar calendar = Calendar.parse(slite.loadContent(targetContent.accessKey).article);
                    toStringer = new ToStringer() {

                        @Override
                        public void append(File file, String tumbnailUrl, String fileUrl) {
                            Date date;
                            if (file.getName().toUpperCase().endsWith(".JPG") || file.getName().toUpperCase().endsWith(".JPEG")) {
                                try {
                                    ExifInterface exif = new ExifInterface(file.getAbsolutePath());
                                    date = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss").parse(exif.getAttribute(ExifInterface.TAG_DATETIME));
                                } catch (IOException | ParseException e) {
                                    date = new Date();
                                }
                            } else if (file.getName().toUpperCase().endsWith(".MP4")) {
                                try {
                                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                                    retriever.setDataSource(file.getAbsolutePath());
                                    date = new SimpleDateFormat("yyyyMMdd'T'HHmmss'.000Z'").parse(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE));
                                } catch (ParseException e) {
                                    date = new Date();
                                }
                            } else {
                                date = new Date();
                            }
                            calendar.append(date, tumbnailUrl, fileUrl);
                        }

                        @Override
                        public Content send(Slite slite) throws IOException {
                            String json = calendar.toJSON();
                            targetContent.article = json;
                            return slite.updateContent(targetContent);
                        }
                    };
                } else {
                    toStringer = new ToStringer() {
                        String appendString = "";

                        @Override
                        public void append(File file, String tumbnailUrl, String fileUrl) {
                            appendString = "[![" + file.getName() + "](" + tumbnailUrl + "){col-xs-12}](" + fileUrl + "){targetOverray col-xs-12 col-sm-3}" + appendString;
                        }

                        @Override
                        public Content send(Slite slite) throws IOException {
                            return slite.appendContent(targetContent.accessKey, appendString);
                        }
                    };
                }
                int index = 1;

                for (UploadSet uploadSet : uploadSets) {
                    Throwable throwedException = null;
                    for (int retryCount = 0; retryCount < 3; retryCount++) {
                        try {
                            Bitmap bitmapForView = uploadSet.getBitmapForView();
                            if (null != bitmapForView) {
                                final Async.Status status = Async.showNotifiction(R.string.uploading, uploadSet.cachedFile.getName() + "(" + (index++) + "/" + uploadSets.size() + ")");
                                int w = bitmapForView.getWidth();
                                int h = bitmapForView.getHeight();
                                float scale = Math.max((float) 500 / w, (float) 500 / h);
                                int size = Math.min(w, h);
                                Matrix matrix = new Matrix();
                                matrix.postScale(scale, scale);
                                Bitmap thumbnail = Bitmap.createBitmap(bitmapForView, (w - size) / 2, (h - size) / 2, size, size, matrix, true);
                                if (uploadSet.isMovie) {
                                    Canvas canvas = new Canvas(thumbnail);
                                    int circleX = thumbnail.getWidth() / 10 * 9;
                                    int circleY = thumbnail.getHeight() / 10 * 9;
                                    Bitmap playBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.play);
                                    canvas.drawBitmap(playBitmap, new Rect(0, 0, playBitmap.getWidth(), playBitmap.getHeight()), new RectF(circleX, circleY, thumbnail.getWidth(), thumbnail.getHeight()), null);
                                    bitmapForView.recycle();
                                    playBitmap.recycle();
                                }
                                String tumbnailUrl = slite.uploadImage(targetContent.accessKey, thumbnail);

                                try (FileInputStream fileIn = new FileInputStream(uploadSet.cachedFile)) {
                                    final long fileSize = uploadSet.cachedFile.length();
                                    String contentUrl = slite.uploadImage(targetContent.accessKey, fileIn, uploadSet.cachedFile.getName(), new Slite.Progress() {
                                        @Override
                                        public void sended(int current) {
                                            if (status != null) {
                                                status.increaseProgress((int) (((double) current / (double) fileSize) * 100));
                                            }
                                        }
                                    });
                                    toStringer.append(uploadSet.cachedFile, tumbnailUrl, contentUrl);
                                }
                                uploadSet.cachedFile.delete();

                                status.increaseProgress(100);
                                status.updateTitle(R.string.success_to_put_content);
                            }
                            throwedException = null;
                            break;
                        } catch (Exception e) {
                            throwedException = e;
                        }
                    }
                    if (null != throwedException) {
                        throw throwedException;
                    }
                }
                Content content = toStringer.send(slite);
                content.save();
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
        }, R.string.fail_to_put_content);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.menu_group) {
            return false;
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

    private class UploadSet {
        private ImageView imageView;
        private File cachedFile;
        private File bitmapCacheFile;
        private boolean isMovie = false;

        public UploadSet() {
        }

        public Bitmap getBitmapForView() {
            if (null == bitmapCacheFile) {
                return null;
            }
            return BitmapFactory.decodeFile(bitmapCacheFile.getAbsolutePath());
        }

        public boolean isMovie() {
            return isMovie;
        }

        public File getCachedFile() {
            return cachedFile;
        }

        public UploadSet readAndCreate(Uri imageUri) throws PermissionException, IOException {
            try {
                Bitmap bitmapForView;
                int permissionCheck = ContextCompat.checkSelfPermission(AppendsUrlToContentActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
                if (PackageManager.PERMISSION_GRANTED != permissionCheck) {
                    ActivityCompat.requestPermissions(AppendsUrlToContentActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            0);
                    throw new PermissionException();
                }
                File file = new File(IO.getPath(getApplication(), imageUri));
                cachedFile = new File(getCacheDir(), file.getName());
                IO.copy(file, cachedFile);
                bitmapForView = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                if (null == bitmapForView) {
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(AppendsUrlToContentActivity.this, imageUri);
                    bitmapForView = retriever.getFrameAtTime(0);
                    isMovie = true;
                }
                bitmapCacheFile = new File(getCacheDir(), UUID.randomUUID().toString() + ".png");
                try (FileOutputStream fout = new FileOutputStream(bitmapCacheFile)) {
                    bitmapForView.compress(Bitmap.CompressFormat.PNG, 100, fout);
                } catch (Exception e) {
                    bitmapCacheFile = null;
                    throw e;
                }
            } catch (Exception e) {
                Async.makeToast(R.string.fail_to_load_image);
                throw new IOException(e);
            }
            return this;
        }
    }
}
