package com.ukiuni.slite.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;

import com.ukiuni.slite.R;
import com.ukiuni.slite.SliteApplication;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by tito on 2015/12/13.
 */
public class ImageUtil {
    public static Bitmap createThumbnail(Bitmap srcBitmap, boolean isMovie) {
        int w = srcBitmap.getWidth();
        int h = srcBitmap.getHeight();
        float scale = Math.max((float) 500 / w, (float) 500 / h);
        int size = Math.min(w, h);
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        Bitmap thumbnail = Bitmap.createBitmap(srcBitmap, (w - size) / 2, (h - size) / 2, size, size, matrix, false);
        if (isMovie) {
            Canvas canvas = new Canvas(thumbnail);
            int circleX = thumbnail.getWidth() / 10 * 9;
            int circleY = thumbnail.getHeight() / 10 * 9;
            Bitmap playBitmap = BitmapFactory.decodeResource(SliteApplication.getInstance().getResources(), R.drawable.play);
            canvas.drawBitmap(playBitmap, new Rect(0, 0, playBitmap.getWidth(), playBitmap.getHeight()), new RectF(circleX, circleY, thumbnail.getWidth(), thumbnail.getHeight()), null);
            playBitmap.recycle();
        }
        return thumbnail;
    }

    public static Date picupDate(File file) {
        Date date = null;
        if (file.getName().toUpperCase().endsWith(".JPG") || file.getName().toUpperCase().endsWith(".JPEG")) {
            try {
                ExifInterface exif = new ExifInterface(file.getAbsolutePath());
                date = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss").parse(exif.getAttribute(ExifInterface.TAG_DATETIME));
            } catch (Exception ignored) {
            }
        } else if (file.getName().toUpperCase().endsWith(".MP4")) {
            try {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(file.getAbsolutePath());
                date = new SimpleDateFormat("yyyyMMdd'T'HHmmss'.000Z'").parse(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE));
            } catch (ParseException ignored) {
            }
        }
        if (null == date) {
            date = new Date();
        }
        return date;
    }
}
