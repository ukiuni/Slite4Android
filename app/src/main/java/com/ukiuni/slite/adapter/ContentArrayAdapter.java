package com.ukiuni.slite.adapter;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ukiuni.slite.model.Content;

import java.util.List;

/**
 * Created by tito on 2015/11/23.
 */
public class ContentArrayAdapter extends ArrayAdapter<Content> {

    private final List<Content> contents;

    public ContentArrayAdapter(Activity context, List<Content> contents) {
        super(context, 0, contents);
        this.contents = contents;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder vh;
        if (convertView == null) {
            convertView = ((Activity) getContext()).getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);
            vh = new ViewHolder();
            vh.title = (TextView) convertView.findViewById(android.R.id.text1);
            convertView.setTag(vh);
        } else {
            vh = (ViewHolder) convertView.getTag();
        }

        vh.title.setText(getItem(position).title);

        return convertView;
    }

    private class ViewHolder {
        public TextView title;
        public ImageView topImageUrl;
        public TextView accountName;
        public ImageView accountIcon;
        public TextView createdAt;
    }
}