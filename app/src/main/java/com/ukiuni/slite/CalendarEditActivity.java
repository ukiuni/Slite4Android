package com.ukiuni.slite;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.raizlabs.android.dbflow.sql.language.Select;
import com.ukiuni.slite.exception.PermissionException;
import com.ukiuni.slite.model.Calendar;
import com.ukiuni.slite.model.Content;
import com.ukiuni.slite.util.Async;
import com.ukiuni.slite.util.ConfirmDialog;
import com.ukiuni.slite.util.ForUploadFiles;
import com.ukiuni.slite.util.ImageUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

/**
 * Created by tito on 2015/10/14.
 */
public class CalendarEditActivity extends SliteBaseActivity {
    private static final String INTENT_KEY_CONTENT_ID = "INTENT_KEY_CONTENT_ID";
    private static final String INTENT_KEY_FOREDIT = "INTENT_KEY_FOREDIT";
    private Content content;
    private Calendar calendar;

    public static void start(Context context, Content content, boolean forEdit) {
        Intent intent = new Intent(context, CalendarEditActivity.class);
        intent.putExtra(INTENT_KEY_CONTENT_ID, content.id);
        intent.putExtra(INTENT_KEY_FOREDIT, forEdit);
        context.startActivity(intent);
    }

    public static void start(Context context, boolean forEdit) {
        Intent intent = new Intent(context, CalendarEditActivity.class);
        intent.putExtra(INTENT_KEY_FOREDIT, forEdit);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final boolean forEdit = getIntent().getBooleanExtra(INTENT_KEY_FOREDIT, false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        final EditText titleEditView = (EditText) findViewById(R.id.titleEditText);
        final TextView titleTextView = (TextView) findViewById(R.id.titleTextView);

        if (forEdit) {
            titleEditView.setVisibility(View.VISIBLE);
        } else {
            titleTextView.setVisibility(View.VISIBLE);
        }
        Async.start(new Async.Task() {
            @Override
            public void work(Async.Handle handle) throws Throwable {
                if (getIntent().hasExtra(INTENT_KEY_CONTENT_ID)) {
                    content = new Select().from(Content.class).byIds(getIntent().getLongExtra(INTENT_KEY_CONTENT_ID, 0)).querySingle();
                    content = SliteApplication.getInstance().getSlite().loadContent(content.accessKey);
                    Log.d("", "-----------content.article--" + content.id + ", " + content.article);
                    calendar = Calendar.parse(content.article);
                } else {
                    content = new Content();
                    calendar = new Calendar();
                }
            }

            @Override
            public void onSuccess() {
                titleEditView.setText(content.title);
                titleTextView.setText(content.title);
                final ImageView imageView = (ImageView) findViewById(R.id.imageView);
                final LinearLayout imageListPane = (LinearLayout) findViewById(R.id.imageListPane);
                final DisplayMetrics metrics = getResources().getDisplayMetrics();
                final int thumbnailSize = (int) (metrics.density * 50);

                CalendarView calendarView = (CalendarView) findViewById(R.id.calendarView);
                calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
                    @Override
                    public void onSelectedDayChange(CalendarView view, final int year, final int month, final int dayOfMonth) {
                        Calendar.Day day = calendar.pickDay(year, month, dayOfMonth);
                        Log.d("", "--------pickDay " + year + "/" + month + "/" + dayOfMonth + ", day " + (null == day ? "null" : day.images.size()));
                        imageListPane.removeAllViews();
                        imageView.setImageBitmap(null);
                        if (null != day && day.images.size() > 0) {
                            Log.d("", "---------setImage " + day.images.get(0).tumbnail);
                            Async.setImage(imageView, day.images.get(0).tumbnail);
                            for (Calendar.ThumbnailAndFile tumbnailAndFile : day.images) {
                                appendTumbnailView(tumbnailAndFile);
                            }
                        }
                        if (forEdit) {
                            ImageView appendImageView = new ImageView(CalendarEditActivity.this);
                            imageListPane.addView(appendImageView);
                            appendImageView.getLayoutParams().width = thumbnailSize;
                            appendImageView.getLayoutParams().height = thumbnailSize;
                            appendImageView.setImageResource(R.drawable.appends);
                            appendImageView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Log.d("---", "--------" + year + "/" + month + "/" + dayOfMonth);
                                    launchChooser();
                                }
                            });
                        }
                    }
                });
            }

            @Override
            public void onError(Throwable e) {
                if (e instanceof ParseException) {
                    Async.makeToast(R.string.fail_to_parse_calendar);
                } else {
                    Async.makeToast(R.string.fail_to_get_content);
                }
                finish();
            }
        });


        if (forEdit) {
            final Button saveButton = (Button) findViewById(R.id.saveButton);
            saveButton.setVisibility(View.VISIBLE);
            saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("", "-----------coiasfasd");
                    Async.start(new Async.Task() {
                        Content createdContent;

                        @Override
                        public void work(Async.Handle handle) throws Throwable {
                            Log.d("sav", "------savestart");
                            content.title = titleEditView.getText().toString();
                            content.article = calendar.toJSON();
                            Log.d("", "----------- save article = " + content.article);
                            if (null != content.accessKey) {
                                Content updatedContent = SliteApplication.getInstance().getSlite().updateContent(content);
                                updatedContent.save();
                                Log.d("sav", "------updatedContent id " + updatedContent.id + ", " + updatedContent.article);
                            } else {
                                createdContent = SliteApplication.getInstance().getSlite().createCalendar(content.title, content.article);
                                createdContent.save();
                            }
                        }

                        @Override
                        public void onSuccess() {
                            Log.d("sav", "------onSuccess");
                            if (null == content.accessKey) {
                                ContentViewActivity.start(CalendarEditActivity.this, createdContent);
                            }
                            finish();
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.e("", "---------", e);
                        }
                    }, R.string.fail_to_put_content);
                }
            });
        } else {
            View view = findViewById(R.id.viewControllPane);
            view.setVisibility(View.VISIBLE);
            final Button editButton = (Button) findViewById(R.id.editButton);
            editButton.setVisibility(View.VISIBLE);
            editButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CalendarEditActivity.start(CalendarEditActivity.this, content, true);
                }
            });
            final Button deleteButton = (Button) findViewById(R.id.deleteButton);
            deleteButton.setVisibility(View.VISIBLE);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final ConfirmDialog dialog = new ConfirmDialog(CalendarEditActivity.this, R.string.confirm_delete_content, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Async.start(new Async.Task() {
                                @Override
                                public void work(Async.Handle handle) throws Throwable {
                                    SliteApplication.getInstance().getSlite().deleteContent(content);
                                    content.delete();
                                }

                                @Override
                                public void onSuccess() {
                                    finish();
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

    private void appendTumbnailView(Calendar.ThumbnailAndFile thumbnailAndFile) {
        appendTumbnailView(thumbnailAndFile, -1);
    }

    private void appendTumbnailView(Calendar.ThumbnailAndFile thumbnailAndFile, int index) {
        LinearLayout imageListPane = (LinearLayout) findViewById(R.id.imageListPane);
        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        final int thumbnailSize = (int) (metrics.density * 50);
        ImageView thumbnailImageView = new ImageView(CalendarEditActivity.this);
        if (0 > index) {
            imageListPane.addView(thumbnailImageView);
        } else {
            imageListPane.addView(thumbnailImageView, index);
        }
        thumbnailImageView.getLayoutParams().width = thumbnailSize;
        thumbnailImageView.getLayoutParams().height = thumbnailSize;
        thumbnailImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        Async.setImage(thumbnailImageView, thumbnailAndFile.tumbnail);
        imageListPane.invalidate();
    }

    private int createRequestCode(int year, int month, int dayOfMonth) {
        return year * 10000 + month * 100 + dayOfMonth;
    }

    Uri pictureUrlForCamera;

    private void launchChooser() {
        // ギャラリーから選択
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);

        // カメラで撮影
        String filename = System.currentTimeMillis() + ".jpg";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, filename);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        pictureUrlForCamera = getContentResolver()
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent i2 = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        i2.putExtra(MediaStore.EXTRA_OUTPUT, pictureUrlForCamera);

        // ギャラリー選択のIntentでcreateChooser()
        Intent chooserIntent = Intent.createChooser(i, "Pick Image");
        // EXTRA_INITIAL_INTENTS にカメラ撮影のIntentを追加
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{i2});

        startActivityForResult(chooserIntent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        try {
            int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            if (PackageManager.PERMISSION_GRANTED != permissionCheck) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
                return;
            }

            CalendarView calendarView = (CalendarView) findViewById(R.id.calendarView);
            Date currentSelected = new Date(calendarView.getDate());
            int year = currentSelected.getYear();
            int month = currentSelected.getMonth();
            int dayOfMonth = currentSelected.getDate();
            if (0 == year || resultCode != RESULT_OK) {
                return;
            }
            Uri result = (null == data || null == data.getData()) ? pictureUrlForCamera : data.getData();
            ImageView imageView = (ImageView) findViewById(R.id.imageView);
            try {
                final ForUploadFiles forUploadFiles = ForUploadFiles.readAndCreate(result);
                Async.start(new Async.Task() {
                    Calendar.ThumbnailAndFile thumbnailAndFile;

                    @Override
                    public void work(Async.Handle handle) throws Throwable {
                        Bitmap bitmapForView = forUploadFiles.getBitmapForView();
                        Bitmap thumbnail = ImageUtil.createThumbnail(bitmapForView, forUploadFiles.isMovie());
                        bitmapForView.recycle();
                        if (null == content.accessKey) {
                            content = SliteApplication.getSlite().createCalendar(content.title, calendar.toJSON());
                        }
                        String thumbnailUrl = SliteApplication.getSlite().uploadImage(content.accessKey, thumbnail);
                        File file = forUploadFiles.cachedFile;
                        try (FileInputStream fileIn = new FileInputStream(file)) {
                            Date date = ImageUtil.picupDate(file);
                            String fileUrl = SliteApplication.getSlite().uploadImage(content.accessKey, fileIn, file.getName());
                            calendar.append(date, thumbnailUrl, fileUrl);
                            thumbnailAndFile = new Calendar.ThumbnailAndFile(thumbnailUrl, fileUrl);
                        }
                        Log.d("", "----------complete " + calendar.toJSON());
                    }

                    @Override
                    public void onSuccess() {
                        appendTumbnailView(thumbnailAndFile, 0);
                        Async.setImage((ImageView) findViewById(R.id.imageView), thumbnailAndFile.tumbnail);
                        Log.d("", "----------successss");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("", "--------", e);
                        Async.makeToast(R.string.fail_to_put_content);
                    }
                }, R.string.fail_to_put_content);
            } catch (PermissionException | IOException e) {
                Log.e("", "-----------", e);
                Async.makeToast(R.string.fail_to_load_image);
            }
        } finally {
            pictureUrlForCamera = null;
        }
    }
}
