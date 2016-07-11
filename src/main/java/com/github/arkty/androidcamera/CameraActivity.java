package com.github.arkty.androidcamera;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Author: Andrey Khitryy
 * Email: andrey.khitryy@gmail.com
 */
public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";

    private static final String EXTRA_REQUIRED_SIZE_PX = "requiredSizePx";
    private static final String EXTRA_REQUIRED_SIZE_BYTES = "requiredSizeBytes";

    private static final String EXTRA_SAVE_IN_GALLERY = "saveInGallery";
    private static final String EXTRA_OUTPUT_FILENAME = "outputFilename";

    public static final String EXTRA_PHOTO_FILE_PATH = "extraPhotoFilePath";

    public static final int RESULT_CAMERA_PERMISSION_DENIED = 1;
    public static final int RESULT_STORAGE_PERMISSION_DENIED = 2;
    public static final int RESULT_ERROR = 3;

    public static void startForResult(AppCompatActivity context, int requestCode, int requiredSizeBytes, int requiredSizePx, boolean saveInGallery, String outputFilename) {
        Intent i = new Intent(context, CameraActivity.class);
        i.putExtra(EXTRA_REQUIRED_SIZE_BYTES, requiredSizeBytes);
        i.putExtra(EXTRA_REQUIRED_SIZE_PX, requiredSizePx);
        i.putExtra(EXTRA_SAVE_IN_GALLERY, saveInGallery);
        i.putExtra(EXTRA_OUTPUT_FILENAME, outputFilename);

        context.startActivityForResult(i, requestCode);
    }

    private final int REQUEST_CAMERA = 0;

    private final int REQUEST_CAMERA_PERMISSION = 1;
    private final int REQUEST_FILEREAD_PERMISSION = 2;

    private int requiredSizeBytes;
    private int requiredSizePx;

    private boolean saveInGallery;
    private String outputFilename;

    private Uri outputFileUri;
    private File outputFile;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent i = getIntent();
        requiredSizeBytes = i.getIntExtra(EXTRA_REQUIRED_SIZE_BYTES, 0);
        requiredSizePx = i.getIntExtra(EXTRA_REQUIRED_SIZE_PX, 0);
        saveInGallery = i.getBooleanExtra(EXTRA_SAVE_IN_GALLERY, false);
        outputFilename = i.getStringExtra(EXTRA_OUTPUT_FILENAME);

        if(saveInGallery) {
            if(outputFilename == null)
                outputFilename = generateFilename();
            outputFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), outputFilename);
        }
        else {
            try {
                outputFile = createTempFile();
            } catch (IOException e) {
                if(BuildConfig.DEBUG)
                    e.printStackTrace();
                setResult(RESULT_ERROR);
                finish();
            }
        }


        if(isPermissionGranted(Manifest.permission.CAMERA)) {
            takePhoto();
        }
        else {
            requestPermission(Manifest.permission.CAMERA, REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_CAMERA){
            if(resultCode == AppCompatActivity.RESULT_OK) {
                if (data != null) {
                    if(isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        returnResult();
                    }
                    else {
                        requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_FILEREAD_PERMISSION);
                    }
                } else {
                    returnResult();
                }
            }
            else {
                setResult(RESULT_CANCELED);
                finish();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePhoto();
                } else {
                    Log.e(TAG, "User didn't allow to use camera.");
                    setResult(RESULT_CAMERA_PERMISSION_DENIED);
                    finish();
                }
                break;
            }
            case REQUEST_FILEREAD_PERMISSION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    returnResult();
                } else {
                    Log.e(TAG, "User didn't allow to read external storage.");
                    setResult(RESULT_STORAGE_PERMISSION_DENIED);
                    finish();
                }
                break;
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        outputFileUri = Uri.fromFile(outputFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    private void returnResult() {
        final Dialog progress = new ProgressDialog.Builder(this)
                .setMessage("Пожалуйста подождите..")
                .setCancelable(false)
                .create();
        progress.show();

        new ImageProcessor(this, outputFile.getAbsolutePath(), new ImageProcessor.Callback() {
            @Override
            public void onImageProcessed(String filename) {
                progress.dismiss();
                Intent i = new Intent();
                if(filename != null) {
                    i.putExtra(EXTRA_PHOTO_FILE_PATH, filename);
                    setResult(RESULT_OK, i);
                }
                else {
                    setResult(RESULT_ERROR);
                }
            }
        }, requiredSizePx, requiredSizeBytes).start();
    }

    private boolean isPermissionGranted(String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;

        return true;
    }

    private void requestPermission(String permission, int code) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            requestPermissions(new String[]{permission}, code);
    }

    private File createTempFile() throws IOException {
        File tempFile = File.createTempFile("photo", ".jpg", getCacheDir());
        tempFile.setWritable(true, false);
        return tempFile;
    }

    private String generateFilename() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return "JPEG_" + timeStamp + "_.jpg";
    }
}