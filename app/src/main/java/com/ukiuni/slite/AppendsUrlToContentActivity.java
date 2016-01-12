package com.ukiuni.slite;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
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

import com.raizlabs.android.dbflow.sql.builder.Condition;
import com.raizlabs.android.dbflow.sql.language.OrderBy;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.ukiuni.slite.exception.PermissionException;
import com.ukiuni.slite.model.Calendar;
import com.ukiuni.slite.model.Content;
import com.ukiuni.slite.model.Content$Table;
import com.ukiuni.slite.util.Async;
import com.ukiuni.slite.util.ForUploadFiles;
import com.ukiuni.slite.util.ImageUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AppendsUrlToContentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appends_content);
        final EditText editText = (EditText) findViewById(R.id.text);
        final ImageView imageView = (ImageView) findViewById(R.id.image);
        final Spinner contentSpinner = (Spinner) findViewById(R.id.contentSpinner);

        final List<ForUploadFiles> uploadSets = new ArrayList<>();

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
                uploadSets.add(ForUploadFiles.createEmpty());
            } else if (imageUri != null) {
                try {
                    ForUploadFiles uploadSet = ForUploadFiles.readAndCreate(imageUri);
                    uploadSets.add(uploadSet);
                    imageView.setImageBitmap(uploadSet.getBitmapForView());
                    imageView.setVisibility(View.VISIBLE);
                } catch (PermissionException | IOException e) {
                    Log.e("", "-------content url load Error--", e);
                    finish();
                    return;
                }
            }
        } else if (intent.getAction().equals(Intent.ACTION_SEND_MULTIPLE)) {
            List<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            for (Uri imageUri : imageUris) {
                try {
                    ForUploadFiles uploadSet = ForUploadFiles.readAndCreate(imageUri);
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
                if (null != getIntent().getStringExtra(Intent.EXTRA_TEXT)) {
                    contents = new Select().from(Content.class).where(Condition.column(Content$Table.TYPE).isNot("calendar")).orderBy(OrderBy.columns(Content$Table.UPDATEDAT).descending()).queryList();
                } else {
                    contents = new Select().from(Content.class).orderBy(OrderBy.columns(Content$Table.UPDATEDAT).descending()).queryList();
                }
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

    protected void uploadFileAsync(final Content targetContent, final List<ForUploadFiles> uploadSets, final EditText editText) {
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
                            Date date = ImageUtil.picupDate(file);
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

                for (ForUploadFiles uploadSet : uploadSets) {
                    Throwable throwedException = null;
                    index++;
                    for (int retryCount = 0; retryCount < 3; retryCount++) {
                        try {
                            Bitmap bitmapForView = uploadSet.getBitmapForView();
                            if (null != bitmapForView) {
                                final Async.Status status = Async.showNotifiction(R.string.uploading, uploadSet.cachedFile.getName() + "(" + (index) + "/" + uploadSets.size() + ")");
                                Bitmap thumbnail = ImageUtil.createThumbnail(bitmapForView, uploadSet.isMovie());
                                bitmapForView.recycle();
                                String tumbnailUrl = slite.uploadImage(targetContent.accessKey, thumbnail);

                                try (FileInputStream fileIn = new FileInputStream(uploadSet.cachedFile)) {
                                    final long fileSize = uploadSet.cachedFile.length();
                                    String contentUrl = slite.uploadImage(targetContent.accessKey, fileIn, uploadSet.cachedFile.getName(), new Slite.Progress() {
                                        @Override
                                        public void sended(long current) {
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
                            Log.e("", "--------------- error retry " + retryCount, e);
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
                Log.e("", "fail to put content", e);
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


}
