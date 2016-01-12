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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by tito on 15/09/26.
 */
public class GcmIntentService extends IntentService {
    private static final int NOTIFICATION_ID = 100;

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
                if ("message".equals(extras.getString("type"))) {
                    sendMessageNotification(extras.getString("message"));
                }
                Log.i(TAG, "Received: --- " + extras.toString());
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void sendNotification(String msg) {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.notify_icon)
                        .setContentTitle(getString(R.string.message_has_come))
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
                        .setContentText(msg);

        NotificationManager notificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    private void sendMessageNotification(String messageJson) {

        String messageBody = "";
        long targetAccountId = 0;
        String fromAccountName = "";
        String channelName = "";
        String channelAccessKey = "";
        try {
            JSONObject messageJSObj = new JSONObject(messageJson);
            messageBody = messageJSObj.getString("body");
            targetAccountId = messageJSObj.getLong("toAccountId");
            fromAccountName = messageJSObj.getJSONObject("fromAccount").getString("name");
            channelName = messageJSObj.getJSONObject("channel").getString("name");
            channelAccessKey = messageJSObj.getJSONObject("channel").getString("accessKey");
        } catch (JSONException e) {
            Log.e("fail", "---------fail to parse notify message", e);
            throw new RuntimeException(e);
        }
        Activity activity = SliteApplication.getInstance().getCurrentActivity();
        if (null != activity && activity instanceof MessageActivity && channelAccessKey.equals(((MessageActivity) activity).getChannelAccessKey())) {
            return;
        }
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        long[] pattern = {0, 200, 50, 200, 500, 200, 50, 200, 500};

        vibrator.vibrate(pattern, -1);
        NotificationManager notificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);
        PendingIntent intent = MessageActivity.createPendingActivity(getApplicationContext(), targetAccountId, channelAccessKey);
        String title = messageBody;
        String message = fromAccountName + "@" + channelName;
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.notify_icon)
                        .setColor(0x66AAFF)
                        .setContentTitle(title)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(message))
                        .setContentText(message);

        builder.setContentIntent(intent);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    public static final void hideNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }
}
