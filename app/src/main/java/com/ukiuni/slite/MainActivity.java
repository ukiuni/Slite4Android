package com.ukiuni.slite;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
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

import com.raizlabs.android.dbflow.sql.language.OrderBy;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.ukiuni.slite.model.MyAccount;
import com.ukiuni.slite.model.MyAccount$Table;
import com.ukiuni.slite.util.Async;

import java.util.List;

public class MainActivity extends SliteBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.signinWithNewAccountButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String mail = ((EditText) findViewById(R.id.mail)).getText().toString();
                final String password = ((EditText) findViewById(R.id.password)).getText().toString();
                final String host = ((EditText) findViewById(R.id.host)).getText().toString();
                final Handler handler = new Handler();
                Async.start(new Async.Task() {
                    MyAccount myAccount;

                    @Override
                    public void work(Async.Handle handle) throws Throwable {
                        Slite slite = SliteApplication.getInstance().getSlite();
                        if (null != host) {
                            slite.setHost(host);
                        }
                        myAccount = slite.signin(mail, password);
                        myAccount.save();
                    }

                    @Override
                    public void onSuccess() {
                        TopActivity.start(MainActivity.this, myAccount.id);
                    }
                }, R.string.fail_to_signin);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        final Spinner spinner = (Spinner) findViewById(R.id.myAccountSpinner);
        Async.start(new Async.Task() {
            List<MyAccount> myAccounts;

            @Override
            public void work(Async.Handle handle) throws Throwable {
                myAccounts = new Select().from(MyAccount.class).orderBy(OrderBy.columns(MyAccount$Table.LASTLOGINEDAT).descending()).queryList();
            }

            @Override
            public void onSuccess() {
                if (null == myAccounts || myAccounts.isEmpty()) {
                    return;
                }
                spinner.setAdapter(new MyAccountSpinnerAdapter(MainActivity.this, myAccounts));
                findViewById(R.id.select_account_view).setVisibility(View.VISIBLE);
            }
        });
        Button signinWithSelectedAccountButton = (Button) findViewById(R.id.signinWithSpinnerButton);
        signinWithSelectedAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyAccount account = new Select().from(MyAccount.class).byIds(spinner.getSelectedItemId()).querySingle();
                Slite slite = SliteApplication.getInstance().getSlite();
                slite.setHost(account.host);
                slite.setSessionKey(account.sessionKey);

                TopActivity.start(MainActivity.this, account.id);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private static class MyAccountSpinnerAdapter extends BaseAdapter {

        private Context context;
        private List<MyAccount> myAccounts;

        public MyAccountSpinnerAdapter(Context context, List<MyAccount> myAccounts) {
            this.context = context;
            this.myAccounts = myAccounts;
        }

        @Override
        public int getCount() {
            return myAccounts.size();
        }

        @Override
        public Object getItem(int position) {
            return myAccounts.get(position);
        }

        @Override
        public long getItemId(int position) {
            return myAccounts.get(position).id;
        }

        @Override
        public View getView(int position, View convertView,
                            ViewGroup parent) {

            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                convertView = inflater.inflate(R.layout.myaccount_spinner, null);
            }
            MyAccount myAccount = myAccounts.get(position);
            TextView textView = (TextView) convertView.findViewById(R.id.accountNameText);
            textView.setText(myAccount.name);
            ImageView imageView = (ImageView) convertView.findViewById(R.id.accountIconImage);
            Async.setImage(imageView, myAccount.iconUrl);

            return convertView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getView(position, convertView, parent);
        }
    }
}
