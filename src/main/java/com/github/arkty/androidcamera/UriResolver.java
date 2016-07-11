package com.github.arkty.androidcamera;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Author: Andrey Khitryy
 * Email: andrey.khitryy@gmail.com
 */

public class UriResolver {

    private static final String TAG = "UriResolver";

    private Context context;

    public UriResolver(Context context) {
        this.context = context;
    }

    public String getAbsolutePath(final Uri uri) {

        String filename = null;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            filename = getAbsolutePathKitKat(uri);
        }

        if(filename == null) {
            Log.v(TAG, "Not kitkat or null");

            if ("content".equalsIgnoreCase(uri.getScheme())) {
                Log.v(TAG, "It's content");

                // Return the remote address
                if (isGooglePhotosUri(uri)) {
                    Log.v(TAG, "It's google photos");
                    return uri.getLastPathSegment();
                }
                else {
                    filename = getDataColumn(context, uri, null, null);
                }
            }

            // File
            else if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }
        }

        if(filename == null) {
            if(uri.getAuthority() != null) {
                return writeToTempfile(uri);
            }
        }
        return filename;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private String getAbsolutePathKitKat(final Uri uri) {
        if(DocumentsContract.isDocumentUri(context, uri)) {
            Log.v(TAG, "It's document uri");
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                Log.v(TAG, "External");
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                Log.v(TAG, "Downloads");

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                Log.v(TAG, "Media");
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        else {
            Log.e(TAG, "Not a document Uri: " + uri.getPath());

        }
        return null;
    }

    private boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    private boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    private String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    private String writeToTempfile(Uri uri) {
        InputStream is = null;
        try {
            is = context.getContentResolver().openInputStream(uri);

            File file = createTempFile();
            FileOutputStream output = new FileOutputStream(file);

            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int len = 0;
            while ((len = is.read(buffer)) != -1) {
                output.write(buffer, 0, len);
            }
            output.close();
            return file.getAbsolutePath();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(is != null)
                    is.close();
            } catch (IOException e) {

            }
        }
        return null;
    }

    private File createTempFile() throws IOException {
        File tempFile = File.createTempFile("pickedPhoto", ".jpg", context.getCacheDir());
        tempFile.setWritable(true, false);
        return tempFile;
    }
}
