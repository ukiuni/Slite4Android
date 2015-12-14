package com.ukiuni.slite;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ukiuni.slite.markdown.MarkdownView;
import com.ukiuni.slite.model.Account;
import com.ukiuni.slite.model.Message;
import com.ukiuni.slite.util.Async;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by tito on 15/10/10.
 */
public class MessageActivity extends SliteBaseActivity {

    private static final String INTENT_KEY_CHANNEL_ACCESS_KEY = "INTENT_KEY_CHANNEL_ACCESS_KEY";
    private Slite.MessageHandle messageHandle;
    public ArrayList<Message> messages = new ArrayList<Message>();
    public ArrayList<Account> member = new ArrayList<Account>();
    public ArrayList<Account> joiningAccounts = new ArrayList<Account>();
    private String channelAccessKey;
    private LinearLayout messagesView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private EditText sendText;
    private Button sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.message);
        this.channelAccessKey = getIntent().getStringExtra(INTENT_KEY_CHANNEL_ACCESS_KEY);
        this.messagesView = (LinearLayout) findViewById(R.id.messagesView);
        this.swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh);
        this.sendText = (EditText) findViewById(R.id.sendText);
        this.sendButton = (Button) findViewById(R.id.sendButton);
    }

    @Override
    protected void onResume() {
        super.onResume();
        messageHandle = new Slite.MessageHandle() {
            @Override
            public void onJoin(Account account) {
                joiningAccounts.add(account);
            }

            @Override
            public void onMessage(Message message) {
                messages.add(message);
                messagesView.addView(createMessageView(message));
                scrollMyListViewToBottom();
            }

            @Override
            public void onHistoricalMessage(Message message) {

                boolean appended = false;
                for (int i = 0; i < messages.size(); i++) {
                    if (messages.get(i).createdAt.after(message.createdAt)) {
                        messagesView.addView(createMessageView(message), i);
                        messages.add(i, message);
                        appended = true;
                        break;
                    }
                }
                if (!appended) {
                    messages.add(message);
                }
                scrollMyListViewToTop();
            }

            @Override
            public void onReave(Account account) {
                joiningAccounts.remove(account);
            }

            @Override
            public void onError(Exception e) {

            }

            @Override
            public void onDisconnect() {

            }

            private void scrollMyListViewToBottom() {
                messagesView.post(new Runnable() {
                    @Override
                    public void run() {
                    }
                });
            }

            private void scrollMyListViewToTop() {
                messagesView.post(new Runnable() {
                    @Override
                    public void run() {
                    }
                });
            }
        };
        try {
            SliteApplication.getSlite().listenChannel(channelAccessKey, messageHandle);
            this.swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    if (messages.isEmpty()) {
                        return;
                    }
                    messageHandle.requestOlder(messages.get(0).id);
                    Async.pushToOriginalThread(new Runnable() {
                        @Override
                        public void run() {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    });
                }
            });
        } catch (IOException e) {
            Async.makeToast(R.string.fail_to_access_to_server);
        }
        sendButton.setOnClickListener(new View.OnClickListener() {
            private String lastSentedText;

            @Override
            public void onClick(View v) {
                final String messageBody = sendText.getText().toString();
                if ("".equals(messageBody) || messageBody.equals(lastSentedText)) {
                    return;
                }
                Async.start(new Async.Task() {
                    @Override
                    public void work(Async.Handle handle) throws Throwable {
                        SliteApplication.getSlite().sendMessage(channelAccessKey, messageBody);
                    }

                    @Override
                    public void onSuccess() {
                        sendText.setText("", TextView.BufferType.NORMAL);
                        lastSentedText = null;
                    }
                }, R.string.fail_to_put_message);
                lastSentedText = messageBody;
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (null != messageHandle) {
            messageHandle.disconnect();
        }
        messages.clear();
    }

    public static void start(Context context, String channelAccessKey) {
        Intent intent = new Intent(context, MessageActivity.class);
        intent.putExtra(INTENT_KEY_CHANNEL_ACCESS_KEY, channelAccessKey);
        context.startActivity(intent);
    }

    private View createMessageView(Message message) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View convertView = inflater.inflate(R.layout.message_row, null);
        convertView.setVisibility(View.GONE);
        TextView textView = (TextView) convertView.findViewById(R.id.accountNameText);
        textView.setText(message.owner.name);
        ImageView imageView = (ImageView) convertView.findViewById(R.id.accountIconImage);
        Async.setImage(imageView, message.owner.iconUrl);
        MarkdownView bodyTextView = (MarkdownView) convertView.findViewById(R.id.bodyText);
        final View finalConvertView = convertView;
        bodyTextView.loadMarkdown(message.body, new MarkdownView.OnLoadedListener() {
            @Override
            public void onLoaded() {
                finalConvertView.setVisibility(View.VISIBLE);
            }
        });

        return convertView;
    }
}