package com.ukiuni.slite;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.ukiuni.slite.model.MyAccount;
import com.ukiuni.slite.util.Async;

/**
 * Created by tito on 2015/12/20.
 */
public class MyAccountPreferenceActivity extends SliteBaseActivity {
    Switch notificationSwitch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.myaccount_preference);
        notificationSwitch = (Switch) findViewById(R.id.notificationSwitch);
        notificationSwitch.setChecked(null != SliteApplication.currentAccount().pushDeviceKey);
        notificationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Async.start(new Async.Task() {
                        @Override
                        public void work(Async.Handle handle) throws Throwable {
                            GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(SliteApplication.getInstance());
                            try {
                                String deviceId = gcm.register("913587589937");
                                if (null != deviceId) {
                                    String key = SliteApplication.getSlite().registDevice(deviceId);
                                    MyAccount myAccount = SliteApplication.currentAccount();
                                    myAccount.pushDeviceKey = key;
                                    myAccount.save();
                                }
                            } catch (Throwable e) {
                                Async.makeToast(R.string.fail_to_setup);
                            }
                        }
                    });
                } else {
                    Async.start(new Async.Task() {
                        @Override
                        public void work(Async.Handle handle) throws Throwable {
                            try {
                                MyAccount myAccount = SliteApplication.currentAccount();
                                if (null != myAccount.pushDeviceKey) {
                                    SliteApplication.getSlite().deleteDevice(myAccount.pushDeviceKey);
                                    myAccount.pushDeviceKey = null;
                                    myAccount.save();
                                }
                            } catch (Throwable e) {
                                Async.makeToast(R.string.fail_to_setup);
                            }
                        }
                    });
                }
            }
        });

    }

    public static void start(Context context) {
        Intent intent = new Intent(context, MyAccountPreferenceActivity.class);
        context.startActivity(intent);
    }
}
