package com.ukiuni.slite.util;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.ukiuni.slite.SliteApplication;

import java.io.IOException;

/**
 * Created by tito on 2015/11/29.
 */
public class RegistrationIntentService extends IntentService {
    private static final String TAG = "Registration service";

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public RegistrationIntentService() {
        super("Registration service");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken("913587589937", GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            Log.d(TAG, "token=" + token);
            SliteApplication.getSlite().registDevice(token);
        } catch (IOException e) {
            Log.d(TAG, "IO Exception ", e);
        }
    }
}
