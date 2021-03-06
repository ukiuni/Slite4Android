package com.ukiuni.slite;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
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

public class SigninActivity extends SliteBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);
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
                        slite.setHost(host);
                        myAccount = slite.signin(mail, password);
                        myAccount.save();
                    }

                    @Override
                    public void onSuccess() {
                        SliteApplication.saveCurrentAccountAsDefault();
                        TopActivity.start(SigninActivity.this, myAccount.id);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("", "----------error", e);
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
                spinner.setAdapter(new MyAccountSpinnerAdapter(SigninActivity.this, myAccounts));
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
                slite.setMyAccount(account);

                SliteApplication.saveCurrentAccountAsDefault();
                TopActivity.start(SigninActivity.this, account.id);
            }
        });
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, SigninActivity.class);
        context.startActivity(intent);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }
}
