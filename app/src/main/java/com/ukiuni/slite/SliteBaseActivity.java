package com.ukiuni.slite;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

/**
 * Created by tito on 2015/10/18.
 */
public class SliteBaseActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        final int abTitleId = getResources().getIdentifier("action_bar_title", "id", "android");
        findViewById(abTitleId).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                TopActivity.start(SliteBaseActivity.this);
            }
        });
    }

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.menu_change_account) {
            SigninActivity.start(this);
            return true;
        } else if (id == R.id.menu_home) {
            TopActivity.start(this);
            return true;
        } else if (id == R.id.menu_group) {
            GroupsActivity.start(this);
            return true;
        } else if (id == R.id.settings) {
            MyAccountPreferenceActivity.start(this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
