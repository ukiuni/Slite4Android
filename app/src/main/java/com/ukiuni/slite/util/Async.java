package com.ukiuni.slite.util;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.ukiuni.slite.R;
import com.ukiuni.slite.SliteApplication;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by tito on 2015/10/11.
 */
public class Async {
    private static Toast toast;
    private static Context context;
    private static Handler handler;
    private static final int TIME_BEFORE_OPEN_DIALOG = 500;

    public static void init(Context context) {
        Async.context = context;
        Async.handler = new Handler();
    }

    public static void pushToOriginalThread(Runnable runnable) {
        handler.post(runnable);
    }

    public static interface Status {
        public void increaseProgress(int percent);

        public void updateMessgae(String message);

        public void updateTitle(int title);
    }

    public static Async.Status showNotifiction(int title, String message) {
        return showNotifiction(context.getString(title), message, null);
    }

    public static Async.Status showNotifiction(String title, String message, PendingIntent pendingIntent) {
        final NotificationManager notifyManager = (NotificationManager) Async.context.getSystemService(Context.NOTIFICATION_SERVICE);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(Async.context);
        builder.setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.notify_icon);
        final int id = 123123123;//(open only one notification)new Random().nextInt();
        if (null != pendingIntent) {
            builder.setContentIntent(pendingIntent);
        }
        return new Async.Status() {
            public void increaseProgress(int percent) {
                builder.setProgress(100, percent, false);
                notifyManager.notify(id, builder.build());
                if (100 <= percent) {
                    builder.setProgress(0, 0, false);
                }
            }

            @Override
            public void updateTitle(int title) {
                builder.setContentTitle(context.getString(title));
                notifyManager.notify(id, builder.build());
            }

            public void updateMessgae(String message) {
                builder.setContentText(message);
                notifyManager.notify(id, builder.build());
            }
        };
    }

    public static Handle start(final Task task, final int... errorStringId) {
        final Handle handle = new Handle();
        ProgressDialog tmpProgress = null;
        final Activity currentActivity = SliteApplication.getInstance().getCurrentActivity();
        if (null != currentActivity) {
            tmpProgress = new ProgressDialog(currentActivity);
        }
        final ProgressDialog progressDialog = tmpProgress;
        final boolean completed = false;
        final Thread execThread = new Thread() {
            @Override
            public void run() {
                final Thread threadInstance = this;
                try {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            task.preExecute();
                        }
                    });
                    task.work(handle);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            task.onSuccess();
                        }
                    });
                } catch (final Throwable e) {
                    if (errorStringId.length > 0) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                makeToast(errorStringId[0]);
                            }
                        });
                    }
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            task.onError(e);
                        }
                    });
                } finally {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (threadInstance) {
                                if (null != progressDialog) {
                                    if (progressDialog.isShowing()) {
                                        progressDialog.hide();
                                    }
                                    progressDialog.dismiss();
                                }
                                handle.complete();
                            }
                            task.onComplete();
                        }
                    });
                }
            }
        };
        new Thread() {
            @Override
            public void run() {
                try {
                    sleep(500);
                } catch (InterruptedException ignored) {
                }
                handler.post(new Thread() {
                    @Override
                    public void run() {
                        synchronized (execThread) {
                            if (handle.isCompleted() || execThread.isInterrupted() || null == currentActivity) {
                                return;
                            }
                            if (null != progressDialog) {
                                progressDialog.show();
                                progressDialog.setContentView(R.layout.progressdialog);
                                progressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialog) {
                                        execThread.interrupt();
                                    }
                                });
                                progressDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                               // progressDialog.setProgressDrawable(context.getDrawable(R.drawable.tranzient));

                            }
                        }
                    }
                });
            }
        }.start();
        handle.setThread(execThread);
        execThread.start();

        return handle;
    }

    private static ExecutorService imageLoadExecutor = Executors.newFixedThreadPool(5);

    public static void setImage(final ImageView titleImage, final String imageUrl, final boolean... withBlur) {
        if (null == imageUrl) {
            return;
        }
        if (findCacheAndAttach(titleImage, imageUrl)) {
            return;
        }
        imageLoadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    URL newurl = new URL(imageUrl + "?sessionKey=" + SliteApplication.currentAccount().sessionKey);
                    final Bitmap iconBitmap = BitmapFactory.decodeStream(newurl.openConnection().getInputStream());
                    if (withBlur.length > 0 && withBlur[0]) {
                        RenderScript renderScript = RenderScript.create(context);
                        Allocation alloc = Allocation.createFromBitmap(renderScript, iconBitmap);
                        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(renderScript, alloc.getElement());
                        blur.setRadius(5f);
                        blur.setInput(alloc);
                        blur.forEach(alloc);
                        alloc.copyTo(iconBitmap);
                    }
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            titleImage.setImageBitmap(iconBitmap);
                        }
                    });
                    saveCache(iconBitmap, imageUrl);
                } catch (Exception ignored) {
                    Log.e("", "---------- imageAttachFailed", ignored);
                }
            }
        });
    }

    private static void saveCache(Bitmap iconBitmap, String url) {
        try (FileOutputStream out = new FileOutputStream(generateCachePath(url))) {
            iconBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            weakHashMap.put(url, iconBitmap);
        } catch (Exception e) {
            Log.e("", "Fail to cache", e);
        }
    }

    private static WeakHashMap<String, Bitmap> weakHashMap = new WeakHashMap<String, Bitmap>();

    private static boolean findCacheAndAttach(final ImageView titleImage, String imageUrl) {
        if (weakHashMap.containsKey(imageUrl)) {
            titleImage.setImageBitmap(weakHashMap.get(imageUrl));
            return true;
        }
        final File cacheFile = findCacheFromCacheDir(imageUrl);
        if (null != cacheFile) {
            imageLoadExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    final Bitmap bitmap = BitmapFactory.decodeFile(cacheFile.getAbsolutePath());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            titleImage.setImageBitmap(bitmap);
                        }
                    });
                }
            });
        }
        return false;
    }

    private static File findCacheFromCacheDir(String imageUrl) {
        File file = generateCachePath(imageUrl);
        if (file.exists()) {
            return file;
        }
        return null;
    }

    private static File generateCachePath(String imageUrl) {
        return new File(context.getCacheDir(), IO.toBase64String(imageUrl));
    }


    public static void makeToast(int messageId) {
        makeToast(context.getResources().getString(messageId));
    }

    public static void makeToast(final String message) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (toast != null) {
                    toast.cancel();
                }
                toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
                toast.show();
            }
        });
    }

    public static class Task {
        public void preExecute() {

        }

        public void work(Handle handle) throws Throwable {
        }

        public void onSuccess() {
        }

        public void onError(Throwable e) {
        }

        public void onComplete() {
        }
    }

    public static class Handle {
        private Thread thread;
        private int progressPercent;
        private boolean completed = false;


        public void cancel() {
            this.thread.interrupt();
        }

        public int getProgressPercent() {
            return progressPercent;
        }

        public void setProgressPercent(int progressPercent) {
            this.progressPercent = progressPercent;
        }

        public void setThread(Thread thread) {
            this.thread = thread;
        }

        public boolean isCompleted() {
            return this.completed;
        }

        public void complete() {
            this.completed = true;
        }
    }
}
