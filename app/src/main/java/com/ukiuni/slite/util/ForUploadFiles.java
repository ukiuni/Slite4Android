package com.ukiuni.slite.util;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.ImageView;

import com.ukiuni.slite.R;
import com.ukiuni.slite.SliteApplication;
import com.ukiuni.slite.exception.PermissionException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by tito on 2015/12/13.
 */
public class ForUploadFiles {
    public ImageView imageView;
    public File cachedFile;
    public File bitmapCacheFile;
    private boolean isMovie = false;

    private ForUploadFiles() {
    }

    public Bitmap getBitmapForView() {
        if (null == bitmapCacheFile) {
            return null;
        }
        return BitmapFactory.decodeFile(bitmapCacheFile.getAbsolutePath());
    }

    public boolean isMovie() {
        return isMovie;
    }

    public File getCachedFile() {
        return cachedFile;
    }

    public static ForUploadFiles readAndCreate(Uri imageUri) throws PermissionException, IOException {
        try {
            ForUploadFiles forUploadFiles = new ForUploadFiles();
            Bitmap bitmapForView;
            int permissionCheck = ContextCompat.checkSelfPermission(SliteApplication.getInstance(), Manifest.permission.READ_EXTERNAL_STORAGE);
            if (PackageManager.PERMISSION_GRANTED != permissionCheck) {
                ActivityCompat.requestPermissions(SliteApplication.getInstance().getCurrentActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
                throw new PermissionException();
            }
            File file = new File(IO.getPath(SliteApplication.getInstance(), imageUri));
            forUploadFiles.cachedFile = new File(SliteApplication.getInstance().getCacheDir(), file.getName());
            IO.copy(file, forUploadFiles.cachedFile);
            bitmapForView = MediaStore.Images.Media.getBitmap(SliteApplication.getInstance().getContentResolver(), imageUri);
            if (null == bitmapForView) {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(SliteApplication.getInstance(), imageUri);
                bitmapForView = retriever.getFrameAtTime(0);
                forUploadFiles.isMovie = true;
            }
            forUploadFiles.bitmapCacheFile = new File(SliteApplication.getInstance().getCacheDir(), UUID.randomUUID().toString() + ".png");
            try (FileOutputStream fout = new FileOutputStream(forUploadFiles.bitmapCacheFile)) {
                bitmapForView.compress(Bitmap.CompressFormat.PNG, 100, fout);
            } catch (Exception e) {
                forUploadFiles.bitmapCacheFile = null;
                throw e;
            }
            return forUploadFiles;
        } catch (Exception e) {
            Async.makeToast(R.string.fail_to_load_image);
            throw new IOException(e);
        }
    }

    public static ForUploadFiles createEmpty() {
        return new ForUploadFiles();
    }
}
