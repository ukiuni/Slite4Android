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
    private static final String PREF_KEY_SELECTED_GROUP_ID = "PREF_KEY_SELECTED_GROUP_ID";
    public static SharedPreferences pref;
    public static final String PREF_KEY_MYACCOUNT_ID = "PREF_KEY_MYACCOUNT_ID";

    private static SliteApplication instance;
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
        FlowManager.init(this);
        // FlowLog.setMinimumLoggingLevel(FlowLog.Level.V);
        this.pref = getSharedPreferences(SliteApplication.class.getName(), MODE_PRIVATE);
        if (this.pref.contains(PREF_KEY_MYACCOUNT_ID)) {
            MyAccount myAccount = new Select().from(MyAccount.class).byIds(this.pref.getLong(PREF_KEY_MYACCOUNT_ID, 0)).querySingle();
            this.slite.setMyAccount(myAccount);
        }
        Async.init(this);
        instance = this;
    }

    public void setCurrentActivity(Activity currentActivity) {
        this.currentActivity = currentActivity;
    }

    public Activity getCurrentActivity() {
        return this.currentActivity;
    }

    public void removeCurrentActivity() {
        this.currentActivity = null;
    }

    public static MyAccount currentAccount() {
        return getInstance().getSlite().currentAccount();
    }

    public static void saveCurrentAccountAsDefault() {
        if (null != getInstance().getSlite().currentAccount()) {
            SharedPreferences.Editor editor = SliteApplication.pref.edit();
            editor.putLong(SliteApplication.PREF_KEY_MYACCOUNT_ID, getInstance().getSlite().currentAccount().id);
            editor.commit();
        }
    }

    public static void saveDefaultGroup(long groupId) {
        if (null != getInstance().getSlite().currentAccount()) {
            SharedPreferences.Editor editor = SliteApplication.pref.edit();
            editor.putLong(SliteApplication.PREF_KEY_SELECTED_GROUP_ID + "_" + currentAccount().id, groupId);
            editor.commit();
        }
    }

    public static long loadDefaultGroup() {
        if (null != getInstance().getSlite().currentAccount()) {
            return pref.getLong(SliteApplication.PREF_KEY_SELECTED_GROUP_ID + "_" + currentAccount().id, 0);
        }
        return 0;
    }
}
