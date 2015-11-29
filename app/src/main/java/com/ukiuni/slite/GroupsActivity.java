package com.ukiuni.slite;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.raizlabs.android.dbflow.sql.builder.Condition;
import com.raizlabs.android.dbflow.sql.language.Delete;
import com.ukiuni.slite.adapter.ChannelArrayAdapter;
import com.ukiuni.slite.adapter.ContentArrayAdapter;
import com.ukiuni.slite.model.Channel;
import com.ukiuni.slite.model.Content;
import com.ukiuni.slite.model.Group;
import com.ukiuni.slite.model.Group$Table;
import com.ukiuni.slite.model.MyAccount;
import com.ukiuni.slite.util.Async;

import java.util.Arrays;
import java.util.List;

/**
 * Created by tito on 15/10/10.
 */
public class GroupsActivity extends SliteBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.groups);
    }

    @Override
    protected void onResume() {
        super.onResume();

        final Spinner spinner = (Spinner) findViewById(R.id.myGroupsSpinner);
        final MyAccount myAccount = SliteApplication.currentAccount();
        final String spinnerSelectedPreferenceKey = "selectedSpinerIndexwith" + myAccount.id;
        spinner.setSelection(SliteApplication.pref.getInt(spinnerSelectedPreferenceKey, 0));
        final ListView contentListView = (ListView) findViewById(R.id.contentListView);
        final ListView channelListView = (ListView) findViewById(R.id.channelListView);
        Async.start(new Async.Task() {
            MyGroupSpinnerAdapter adapter;

            @Override
            public void work(Async.Handle handle) throws Throwable {
                final List<Group> groups = SliteApplication.getSlite().loadMyGroups();
                new Delete().from(Group.class).where(Condition.column(Group$Table.LOCALOWNER_LOCAL_OWNER_ID).eq(SliteApplication.currentAccount().id)).query();
                for (Group group : groups) {
                    group.localOwner = myAccount;
                    group.save();
                }
                adapter = new MyGroupSpinnerAdapter(GroupsActivity.this, groups);
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, final int position, long id) {
                        SliteApplication.saveDefaultGroup(adapter.getItem(position).id);
                        Async.start(new Async.Task() {
                            Group group;

                            @Override
                            public void work(Async.Handle handle) throws Throwable {
                                group = SliteApplication.getSlite().loadGroup(groups.get(position).accessKey);
                                if (null != group.contents) {
                                    for (Content content : group.contents) {
                                        content.loadAccount = myAccount;
                                        content.save();
                                    }
                                }
                            }

                            @Override
                            public void onSuccess() {
                                if (null == group.contents || 0 == group.contents.size()) {
                                    Content content = new Content();
                                    content.title = getString(R.string.no_content);
                                    contentListView.setAdapter(new ContentArrayAdapter(GroupsActivity.this, Arrays.asList(content)));
                                    contentListView.setOnItemClickListener(null);
                                } else {
                                    contentListView.setAdapter(new ContentArrayAdapter(GroupsActivity.this, group.contents));
                                    contentListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                        @Override
                                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                            ContentViewActivity.start(GroupsActivity.this, group.contents.get(position));
                                        }
                                    });
                                }
                                if (null == group.channels || 0 == group.channels.size()) {
                                    Channel channel = new Channel();
                                    channel.name = getString(R.string.no_channel);
                                    channelListView.setAdapter(new ChannelArrayAdapter(GroupsActivity.this, Arrays.asList(channel)));
                                    channelListView.setOnItemClickListener(null);
                                } else {
                                    channelListView.setAdapter(new ChannelArrayAdapter(GroupsActivity.this, group.channels));
                                    channelListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                        @Override
                                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                            MessageActivity.start(GroupsActivity.this, group.channels.get(position).accessKey);
                                        }
                                    });
                                }
                                SharedPreferences.Editor edit = SliteApplication.pref.edit();
                                edit.putInt(spinnerSelectedPreferenceKey, position);
                                edit.commit();
                            }

                            @Override
                            public void onError(Throwable e) {
                                Log.d("", "-----get group error ", e);
                            }
                        }, R.string.fail_to_access_to_server);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
            }

            @Override
            public void onSuccess() {
                spinner.setAdapter(adapter);
                long selectedGroupId = SliteApplication.loadDefaultGroup();
                for (int i = 0; i < adapter.getCount(); i++) {
                    if (selectedGroupId == adapter.getItem(i).id) {
                        spinner.setSelection(i);
                        break;
                    }
                }
            }

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {
                Log.v("", "onError --------- ", e);
            }
        });
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, GroupsActivity.class);
        context.startActivity(intent);
    }

    private static class MyGroupSpinnerAdapter extends BaseAdapter {

        private Context context;
        private List<Group> groups;

        public MyGroupSpinnerAdapter(Context context, List<Group> groups) {
            this.context = context;
            this.groups = groups;
        }

        @Override
        public int getCount() {
            return groups.size();
        }

        @Override
        public Group getItem(int position) {
            return groups.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView,
                            ViewGroup parent) {

            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                convertView = inflater.inflate(R.layout.myaccount_spinner, null);
            }
            Group myGroup = groups.get(position);
            TextView textView = (TextView) convertView.findViewById(R.id.accountNameText);
            textView.setText(myGroup.name);
            ImageView imageView = (ImageView) convertView.findViewById(R.id.accountIconImage);
            Async.setImage(imageView, myGroup.iconUrl);

            return convertView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getView(position, convertView, parent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_group) {
            return false;
        }
        return super.onOptionsItemSelected(item);
    }

}