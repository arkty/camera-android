package com.github.arkty.androidcamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Author: Andrey Khitryy
 * Email: andrey.khitryy@gmail.com
 */

public class ImageProcessor extends Thread {

    private static final String TAG = "ImageProcessor";

    private final Context context;
    private final String filename;
    private final WeakReference<Callback> callback;
    private final int requiredSizePx;
    private final int requiredSizeBytes;

    public ImageProcessor(Context context, String filename, Callback callback, int requiredSizePx,
                          int requiredSizeBytes) {
        this.context = context;
        this.filename = filename;
        this.callback = new WeakReference<>(callback);
        this.requiredSizePx = requiredSizePx;
        this.requiredSizeBytes = requiredSizeBytes;
    }

    @Override
    public void run() {
        Bitmap bitmap;
        int scaleFactor = 1;

        if(requiredSizePx == 0 && requiredSizeBytes == 0) {
            returnResult(filename);
            return;
        }

        if(requiredSizePx > 0) {
            Log.v(TAG, "requiredSizePx = " + requiredSizePx);
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(filename, bmOptions);

            int imageW = bmOptions.outWidth;
            int imageH = bmOptions.outHeight;
            Log.v(TAG, "image = (" + imageW + ", " + imageH + ")");

            while(imageW / scaleFactor / 2 >= requiredSizePx &&
                    imageH / scaleFactor / 2 >= requiredSizePx) {
                scaleFactor *= 2;
            }

            scaleFactor = Math.min(imageW / requiredSizePx, imageH / requiredSizePx);
            Log.v(TAG, "scale = " + scaleFactor);
        }

        long originalSize = getBitmapSize();
        Log.v(TAG, "originalSize = " + originalSize);

        long originalSizeScaled = originalSize / (scaleFactor * scaleFactor);
        Log.v(TAG, "originalSizeScaled = " + originalSizeScaled);

        if(requiredSizeBytes > 0 && originalSizeScaled > requiredSizeBytes) {

            Log.v(TAG, "requiredSizeBytes = " + requiredSizeBytes);

            while(originalSizeScaled / scaleFactor >= requiredSizeBytes) {
                scaleFactor *= 2;
            }
        }

        Log.v(TAG, "scale = " + scaleFactor);

        if(scaleFactor > 1) {
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            bitmap = BitmapFactory.decodeFile(filename, bmOptions);
        }
        else {
            bitmap = BitmapFactory.decodeFile(filename);
        }
        File resultFile = null;

        try {
            if(filename.contains(context.getCacheDir().getAbsolutePath())) {
                resultFile = new File(filename);
            } else {
                resultFile = File.createTempFile("image", ".png", context.getCacheDir());
            }
            FileOutputStream compressed = new FileOutputStream(resultFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 97, compressed);
            compressed.flush();
            compressed.close();
            bitmap.recycle();
        } catch (FileNotFoundException e) {

        } catch (IOException e) {

        }

        final String resultFilename = resultFile != null ? resultFile.getAbsolutePath() : filename;
        Log.v(TAG, "resultSize = " + resultFile.length());
        Log.v(TAG, "resultFilename = " + resultFilename);

       returnResult(resultFilename);
    }

    private long getBitmapSize() {
        return new File(filename).length();
    }

    private void returnResult(final String filename) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if(callback.get() != null)
                    callback.get().onImageProcessed(filename);
            }
        });
    }

    public interface Callback {
        void onImageProcessed(String filename);
    }
}
