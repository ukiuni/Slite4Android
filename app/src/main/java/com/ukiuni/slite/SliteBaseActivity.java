package com.ukiuni.slite;

import android.support.v7.app.AppCompatActivity;

/**
 * Created by tito on 2015/10/18.
 */
public class SliteBaseActivity extends AppCompatActivity {
    @Override
    protected void onResume() {
        super.onResume();
        SliteApplication.getInstance().setCurrentActivity(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        SliteApplication.getInstance().removeCurrentActivity();
    }
}
