package com.ukiuni.slite;

import android.app.Activity;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

/**
 * Created by tito on 15/09/26.
 */
public class GcmIntentService extends IntentService {

    static final private String TAG = GcmIntentService.class.getSimpleName();

    public GcmIntentService() {
        super(GcmIntentService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);

        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle

            if (GoogleCloudMessaging.
                    MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                sendNotification("Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.
                    MESSAGE_TYPE_DELETED.equals(messageType)) {
                sendNotification("Deleted messages on server: " +
                        extras.toString());
                // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.
                    MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                // Post notification of received message.

                sendNotification("Received: " + extras.get("type"));
                Log.i(TAG, "Received: --- " + extras.toString());
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    // Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.
    private void sendNotification(String msg) {
        int NOTIFICATION_ID = 1;
        Activity activity = SliteApplication.getInstance().getCurrentActivity();
        String kumaChannelAccessKey = "v2g19-RWViCOcKJJ-95IxO5_8p_htBfWrZNrt_uWXmZrKgHstxL-FzUg21mpDfyP";
        if (null != activity && activity instanceof MessageActivity && kumaChannelAccessKey.equals(((MessageActivity) activity).getChannelAccessKey())) {
            return;
        }
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        long[] pattern = {0, 200, 50, 200, 500, 200, 50, 200, 500};

        vibrator.vibrate(pattern, -1);
        NotificationManager notificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);
        MessageActivity.createPendingActivity(getApplicationContext(), kumaChannelAccessKey);//TODO
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(android.R.drawable.sym_def_app_icon)//TODO
                        .setContentTitle(getString(R.string.message_has_come))
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
                        .setContentText(msg);

        builder.setContentIntent(contentIntent);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}
