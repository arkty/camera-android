package com.github.arkty.androidcamera;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

/**
 * Author: Andrey Khitryy
 * Email: andrey.khitryy@gmail.com
 */

public class GalleryActivity extends AppCompatActivity implements ImageProcessor.Callback{

    private static final String TAG = "GalleryActivity";

    private static final String EXTRA_REQUIRED_SIZE_PX = "requiredSizePx";
    private static final String EXTRA_REQUIRED_SIZE_BYTES = "requiredSizeBytes";
    public static final String EXTRA_PHOTO_FILE_PATH = "extraPhotoFilePath";

    private static final int REQUEST_GALLERY = 128;
    public static final int RESULT_ERROR = 1;

    public static void startForResult(AppCompatActivity context, int requestCode, int requiredSizeBytes, int requiredSizePx) {
        Intent i = new Intent(context, com.github.arkty.androidcamera.GalleryActivity.class);
        i.putExtra(EXTRA_REQUIRED_SIZE_PX, requiredSizePx);
        i.putExtra(EXTRA_REQUIRED_SIZE_BYTES, requiredSizeBytes);
        context.startActivityForResult(i, requestCode);
    }

    private int requiredSizePx;
    private int requiredSizeBytes;

    private UriResolver uriResolver;
    private Dialog progress;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = getIntent();
        requiredSizePx = i.getIntExtra(EXTRA_REQUIRED_SIZE_PX, 0);
        requiredSizeBytes = i.getIntExtra(EXTRA_REQUIRED_SIZE_BYTES, 0);

        uriResolver = new UriResolver(this);

        progress = new ProgressDialog.Builder(this)
                .setMessage("Пожалуйста подождите..")
                .setCancelable(false)
                .create();

        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_GALLERY) {
            if(resultCode == RESULT_OK) {
                String filepath = uriResolver.getAbsolutePath(data.getData());
                if(filepath != null) {
                    returnResult(filepath);
                }
                else {
                    setResult(RESULT_ERROR);
                    finish();
                }
            }
            else {
                setResult(RESULT_CANCELED);
                finish();
            }
        }
    }

    private void returnResult(String filepath) {
        progress.show();
        new ImageProcessor(this, filepath, this, requiredSizePx, requiredSizeBytes).start();
    }

    @Override
    public void onImageProcessed(String filename) {
        if(progress != null && progress.isShowing())
            progress.dismiss();
        Intent i = new Intent();
        if(filename != null) {
            i.putExtra(EXTRA_PHOTO_FILE_PATH, filename);
            setResult(RESULT_OK, i);
        }
        else {
            setResult(RESULT_ERROR);
        }
        finish();
    }
}
