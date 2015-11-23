package com.ukiuni.slite.util;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.ukiuni.slite.R;

/**
 * Created by tito on 2015/10/21.
 */
public class ConfirmDialog extends Dialog {
    private final View.OnClickListener okListener;
    private final int messageId;

    public ConfirmDialog(Context context, int messageId, View.OnClickListener okListener) {
        super(context);
        this.messageId = messageId;
        this.okListener = okListener;
    }

    @Override
    public void show() {
        setContentView(R.layout.confirm_dialog);
        Button okButton = (Button) findViewById(R.id.okButton);
        Button cancelButton = (Button) findViewById(R.id.cancelButton);
        TextView textView = (TextView) findViewById(R.id.message);
        textView.setText(messageId);
        okButton.setOnClickListener(this.okListener);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        super.show();
    }
}
