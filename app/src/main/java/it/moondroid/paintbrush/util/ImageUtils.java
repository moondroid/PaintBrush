package it.moondroid.paintbrush.util;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.util.Random;

import it.moondroid.paintbrush.R;

import static android.provider.MediaStore.Images.Media;
import static android.provider.MediaStore.Images.Thumbnails;

/**
 * Created by marco.granatiero on 01/09/2014.
 */
public class ImageUtils {

    private static final String TAG = "PaintBrush";
    private static final String DIRECTORY_NAME = "PaintBrush";

    public static final int ERROR_BITMAP_UNAVAILABLE = 1;
    public static final int ERROR_MEDIA_UNMOUNTED = 2;
    public static final int ERROR_MKDIR_FAIL = 3;
    public static final int ERROR_FILE_NOT_FOUND = 4;
    public static final int ERROR_IO_EXCEPTION = 5;
    public static final int ERROR_OUT_OF_MEMORY_DECODE_BITMAP = 6;

    private static final String[] PROJECTION_ID_DATA;
    private static final Uri IMAGE_URI;
    private static final Uri THUMBNAIL_URI;
    private static final String PREFIX_PICTURE_FILENAME = "p";
    private static final String THUMBNAIL_DESCRIPTION = "it.moondroid.paintbrush";

    private static final boolean USE_THUMBNAILS_MEDIA_STORE = true;

    static {
        IMAGE_URI = Media.EXTERNAL_CONTENT_URI;
        THUMBNAIL_URI = Thumbnails.EXTERNAL_CONTENT_URI;
        String[] strArr = new String[2];
        strArr[0] = Media._ID; //"_id";
        strArr[1] = Media.DATA; //"_data";
        PROJECTION_ID_DATA = strArr;
    }

    public static interface SaveImageListener {
        void onCompleted(Uri uri);
        void onError(int i);
    }

    private static class ImageSaveProviderAsyncTask extends AsyncTask<Void, Integer, Uri> {
        private Bitmap mBitmap;
        private final Context mContext;
        private int mErrorCode;
        private final SaveImageListener mListener;
        private Dialog mProgressDialog;

        private ImageSaveProviderAsyncTask(Context context, Bitmap bitmap, SaveImageListener listener) {
            this.mErrorCode = 0;
            this.mContext = context;
            this.mBitmap = bitmap;
            this.mListener = listener;
        }

        private void setErrorCode(int errorCode) {
            this.mErrorCode = errorCode;
        }

        @Override
        protected void onPreExecute() {
            this.mProgressDialog = createProgressDialog(this.mContext, R.string.popup_saving);
            this.mProgressDialog.show();
        }

        @Override
        protected Uri doInBackground(Void ... params) {
            if (this.mBitmap == null) {
                setErrorCode(ERROR_BITMAP_UNAVAILABLE);
                return null;
            } else if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                long t1 = SystemClock.elapsedRealtime();
                String commonFileName = ImageUtils.generateMagicNumber();

                Uri uri = null;
                if (USE_THUMBNAILS_MEDIA_STORE) {
                    uri = ImageUtils.insertImageSimple(this.mContext.getContentResolver(),
                            mBitmap, new File(ImageUtils.getPictureDir(),
                                    PREFIX_PICTURE_FILENAME + commonFileName + ".jpg").getAbsolutePath(),
                            commonFileName, THUMBNAIL_DESCRIPTION, "image/jpeg");
                }

                return uri;
            } else {
                setErrorCode(ERROR_MEDIA_UNMOUNTED);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Uri result) {
            this.mProgressDialog.dismiss();
            if (result == null) {
                this.mListener.onError(this.mErrorCode);
                return;
            }
            this.mListener.onCompleted(result);
        }

    }

    private static String generateMagicNumber() {
        StringBuilder stringBuilder = new StringBuilder().append(Long.toString(System.currentTimeMillis()));
        Object[] objArr = new Object[1];
        objArr[0] = Integer.valueOf(new Random().nextInt(100));
        return stringBuilder.append(String.format("%02d", objArr)).toString();
    }

    private static final File getPictureDir() {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return null;
        }
        File dir = new File(Environment.getExternalStorageDirectory(), DIRECTORY_NAME);
        return (dir.exists() || dir.mkdir()) ? dir : null;
    }

    private static final Uri insertImageSimple(ContentResolver cr, Bitmap source, String path, String title, String description, String mimeType) {
        if (source == null) {
            Log.e(TAG, "source bitmap is null");
            return null;
        }
        String stringUrl = Media.insertImage(cr, source, title, description);
        if (stringUrl == null) {
            Log.e(TAG, "stringUrl is null");
            return null;
        }
        Uri uri = Uri.parse(stringUrl);
        Cursor c = cr.query(uri, PROJECTION_ID_DATA, null, null, null);
        long fileSize = 0;
        if (c != null) {
            if ((!c.moveToFirst()) || c.getString(c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)) == null) {
                c.close();
            } else {
                File src = new File(c.getString(c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)));
                fileSize = src.length();
                if (!src.renameTo(new File(path))) {
                    Log.e(TAG, "insertImageSimple rename fail");
                }
                c.close();
            }
        }

        //Create any metadata for image
        ContentValues values = new ContentValues();
        if (mimeType != null) {
            values.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
        }
        values.put(MediaStore.Images.Media.DATA, path);
        if (fileSize > 0) {
            values.put(MediaStore.Images.Media.SIZE, Long.valueOf(fileSize));
        }

        final long nowMillis = System.currentTimeMillis();
        values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, nowMillis);
        values.put(MediaStore.Images.ImageColumns.DATE_ADDED, nowMillis / 1000);
        values.put(MediaStore.Images.ImageColumns.DATE_MODIFIED, nowMillis / 1000);

        int result = cr.update(uri, values, null, null);
        return uri;
    }

    public static Dialog createProgressDialog(Context context, int messageId) {
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setMessage(context.getResources().getString(messageId));
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);

        return dialog;
    }

    public static void saveImageFromBitmap(Context context, Bitmap bitmap, SaveImageListener listener) {
        new ImageSaveProviderAsyncTask(context, bitmap, listener).execute(new Void[0]);
    }

}
