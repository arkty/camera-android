package com.github.arkty.androidcamera;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

/**
 * Author: Andrey Khitryy
 * Email: andrey.khitryy@gmail.com
 */

public class ImagePickHelper {

    public static final int REASON_CANCELLED = 0;
    public static final int REASON_ERROR = 1;
    public static final int REASON_PERMISSIONS = 2;

    private static final int REQUEST_CAMERA = 173;
    private static final int REQUEST_GALLERY = 179;

    private AppCompatActivity activity;
    private OnPickListener listener;

    private String galleryTitle = "Выбрать из галереи";
    private String cameraTitle = "Сделать снимок";
    private String needMorePermissionsMessage = "Для того, чтобы сделать фото, приложению нужны права для доступа к камере и внутреннему хранилищу.\n" +
            "Вы можете предоставить доступ в настройках Android.";

    private int requiredSizePx = 0;
    private int requiredSizeBytes = 0;
    private boolean saveToGallery = false;
    private String outputFilename = null;

    public ImagePickHelper(AppCompatActivity activity, OnPickListener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    public void setRequiredSizePx(int requiredSizePx) {
        this.requiredSizePx = requiredSizePx;
    }

    public void setRequiredSizeBytes(int requiredSizeBytes) {
        this.requiredSizeBytes = requiredSizeBytes;
    }

    public void setSaveToGallery(boolean saveToGallery) {
        this.saveToGallery = saveToGallery;
    }

    public void setOutputFilename(String outputFilename) {
        this.outputFilename = outputFilename;
    }

    public void setNeedMorePermissionsMessage(int message) {
        this.needMorePermissionsMessage = activity.getString(message);
    }

    public void setTitles(int gallery, int camera) {
        this.galleryTitle = activity.getString(gallery);
        this.cameraTitle = activity.getString(camera);
    }

    public void pickImage() {

        new AlertDialog.Builder(activity).setItems(new String[]{galleryTitle, cameraTitle}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which == 0) {
                    GalleryActivity.startForResult(activity, REQUEST_GALLERY, requiredSizeBytes, requiredSizePx);
                }
                else {
                    CameraActivity.startForResult(activity, REQUEST_CAMERA, requiredSizeBytes, requiredSizePx, saveToGallery, outputFilename);
                }
            }
        }).create().show();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_CAMERA) {
            if(resultCode == CameraActivity.RESULT_OK) {
                String filename = data.getExtras().getString(CameraActivity.EXTRA_PHOTO_FILE_PATH);
                listener.onImagePicked(filename);
            }
            else if(resultCode == CameraActivity.RESULT_CAMERA_PERMISSION_DENIED ||
                    resultCode == CameraActivity.RESULT_STORAGE_PERMISSION_DENIED) {
                new AlertDialog.Builder(activity).setMessage(needMorePermissionsMessage).create().show();
                listener.onPickCancelled(REASON_PERMISSIONS);
            }
            else if(resultCode == CameraActivity.RESULT_CANCELED) {
                listener.onPickCancelled(REASON_CANCELLED);
            }
            else {
                listener.onPickCancelled(REASON_ERROR);
            }

        }
        else if(requestCode == REQUEST_GALLERY) {
            if(resultCode == GalleryActivity.RESULT_OK) {
                String filename = data.getExtras().getString(CameraActivity.EXTRA_PHOTO_FILE_PATH);
                listener.onImagePicked(filename);
            }
            else if(resultCode == GalleryActivity.RESULT_CANCELED) {
                listener.onPickCancelled(REASON_CANCELLED);
            }
            else {
                listener.onPickCancelled(REASON_ERROR);
            }
        }
    }

    public interface OnPickListener {
        void onImagePicked(String filename);
        void onPickCancelled(int reason);
    }
}
