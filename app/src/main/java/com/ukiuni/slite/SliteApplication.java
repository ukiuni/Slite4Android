package com.ukiuni.slite;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;

import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.ukiuni.slite.model.MyAccount;
import com.ukiuni.slite.util.Async;


/**
 * Created by tito on 2015/10/11.
 */
public class SliteApplication extends Application {
    public static final Slite slite = new Slite();
    public static SharedPreferences pref;
    public static final String PREF_KEY_MYACCOUNT_ID = "PREF_KEY_MYACCOUNT_ID";

    public static SliteApplication instance;
    private Activity currentActivity;

    public static SliteApplication getInstance() {
        return instance;
    }

    public static Slite getSlite() {
        return slite;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.pref = getSharedPreferences(SliteApplication.class.getName(), MODE_PRIVATE);
        if (this.pref.contains(PREF_KEY_MYACCOUNT_ID)) {
            MyAccount myAccount = new Select().from(MyAccount.class).byIds(this.pref.getLong(PREF_KEY_MYACCOUNT_ID, 0)).querySingle();
            slite.setMyAccount(myAccount);
        }
        FlowManager.init(this);
        Async.init(this);
        instance = this;
    }

    public void setCurrentActivity(Activity currentActivity) {
        this.currentActivity = currentActivity;
        Async.setCurrentActivity(currentActivity);
    }

    public void removeCurrentActivity() {
        this.currentActivity = null;
        Async.removeCurrentActivity();
    }
}
