package com.ukiuni.slite.model;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by tito on 2015/12/08.
 */
public class Calendar {
    public final Map<String, Day> calendarMap = new TreeMap<>();

    public void append(Date date, String tumbnail, String file) {
        Log.d("", "-------bef--- " + calendarMap.size());
        String dateKey = new SimpleDateFormat("yyyy/M/d").format(date);
        Day day = calendarMap.get(dateKey);
        if (null == day) {
            day = new Day();
            day.dateKey = dateKey;
            calendarMap.put(dateKey, day);
            day.images = new ArrayList<>();
        }
        day.images.add(new ThumbnailAndFile(tumbnail, file));
        Log.d("", "-------aft--- " + calendarMap.size());
    }

    public Day pickDay(int year, int month, int dateOfMonth) {
        return calendarMap.get(year + "/" + (month + 1) + "/" + dateOfMonth);
    }

    public static Calendar parse(String article) throws IOException {
        try {
            Calendar calendar = new Calendar();
            JSONObject articleJ = new JSONObject(article);
            for (Iterator<String> it = articleJ.keys(); it.hasNext(); ) {
                String dateKey = it.next();
                JSONObject dayJ = articleJ.getJSONObject(dateKey);
                Day day = new Day();
                day.dateKey = dateKey;
                if (dayJ.has("description")) {
                    day.description = dayJ.getString("description");
                }

                if (dayJ.has("images")) {
                    JSONArray imagesJ = dayJ.getJSONArray("images");
                    for (int i = 0; i < imagesJ.length(); i++) {
                        JSONObject imageJ = imagesJ.getJSONObject(i);
                        ThumbnailAndFile tumbnailAndFile = new ThumbnailAndFile(imageJ.getString("thumbnail"), imageJ.getString("file"));
                        day.images.add(tumbnailAndFile);
                    }
                }
                calendar.calendarMap.put(dateKey, day);
            }
            return calendar;
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    public String toJSON() throws IOException {
        try {
            JSONObject articleJ = new JSONObject();
            for (String dateKey : calendarMap.keySet()) {
                Day day = calendarMap.get(dateKey);
                JSONObject dayJ = new JSONObject();
                dayJ.put("description", day.description);
                JSONArray imagesJ = new JSONArray();
                for (ThumbnailAndFile tumbnailAndFile : day.images) {
                    JSONObject imageJ = new JSONObject();
                    imageJ.put("thumbnail", tumbnailAndFile.tumbnail);
                    imageJ.put("file", tumbnailAndFile.file);
                    imagesJ.put(imageJ);
                }
                dayJ.put("images", imagesJ);
                articleJ.put(dateKey, dayJ);
            }
            return articleJ.toString();
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    public static class Day {
        public String dateKey;
        public String description;
        public List<ThumbnailAndFile> images = new ArrayList<>();
    }

    public static class ThumbnailAndFile {
        public ThumbnailAndFile(String tumbnail, String file) {
            this.tumbnail = tumbnail;
            this.file = file;
        }

        public String tumbnail;
        public String file;
    }
}
