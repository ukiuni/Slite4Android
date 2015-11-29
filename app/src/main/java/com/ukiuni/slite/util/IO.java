package com.ukiuni.slite.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by tito on 2015/11/14.
 */
public class IO {
    public static String asString(InputStream in) {
        BufferedInputStream bin = new BufferedInputStream(in);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024 * 1024];
        try {
            int readed = in.read(buffer);
            while (readed >= 0) {
                bout.write(buffer, 0, readed);
                readed = in.read(buffer);
            }
            return new String(bout.toByteArray(), "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            closeQuietry(bin);

        }
    }

    public static String pathToString(String path) {
        FileInputStream in = null;
        try {
            in = new FileInputStream(new File(path));
            return asString(in);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            closeQuietry(in);
        }
    }

    public static String assetToString(AssetManager asset, String path) {
        InputStream in = null;
        try {
            in = asset.open(path);
            return asString(in);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            closeQuietry(in);
        }
    }

    public static void closeQuietry(Closeable closeable) {
        try {
            closeable.close();
        } catch (Exception ignore) {
        }
    }

    public static String getPath(Context context, Uri uri) {
        ContentResolver contentResolver = context.getContentResolver();
        String[] columns = {MediaStore.Images.Media.DATA};
        Cursor cursor = contentResolver.query(uri, columns, null, null, null);
        cursor.moveToFirst();
        String path = cursor.getString(0);
        cursor.close();
        return path;
    }

    public static void copy(File from, File to) throws IOException {
        try (FileInputStream fromIn = new FileInputStream(from); FileOutputStream toOut = new FileOutputStream(to)) {
            copy(fromIn, toOut);
        }
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        for (int readed = in.read(buffer); readed > 0; readed = in.read(buffer)) {
            out.write(buffer, 0, readed);
        }
    }
}
