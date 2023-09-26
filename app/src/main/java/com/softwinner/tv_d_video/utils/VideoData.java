package com.softwinner.tv_d_video.utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import com.softwinner.tv_d_video.model.FileVideo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;

import androidx.loader.content.CursorLoader;

@SuppressLint("SimpleDateFormat")
public class VideoData {

    public interface Callback {
        void onSuccess(List<FileVideo> mVideoInfo);
    }

    private Callback callback;
    public Context mContext;

    public VideoData(Callback callback, Context context) {
        this.callback = callback;
        this.mContext = context;
    }

    public void run() {
        new ReaderTask().execute();
    }

    public class ReaderTask extends AsyncTask<Void, Void, List<FileVideo>> {

        @Override
        protected List<FileVideo> doInBackground(Void... params) {

//            String[] projection = {
//                    MediaStore.Files.FileColumns._ID,
//                    MediaStore.Files.FileColumns.DATA,
//                    MediaStore.Files.FileColumns.DATE_ADDED,
//                    MediaStore.Files.FileColumns.MEDIA_TYPE,
//                    MediaStore.Files.FileColumns.MIME_TYPE,
//                    MediaStore.Files.FileColumns.TITLE
//            };
//            String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "="
//                    + MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO
//                    + " OR "
//                    + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
//                    + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;
//            Uri queryUri = MediaStore.Files.getContentUri("external");
//
//            CursorLoader cursorLoader = new CursorLoader(
//                    mContext,
//                    queryUri,
//                    projection,
//                    selection,
//                    null, // Selection args (none).
//                    MediaStore.Files.FileColumns.DATE_ADDED + " DESC" // Sort order.
//            );
//
//            Cursor cursor = cursorLoader.loadInBackground();

            Cursor cursor = mContext.getContentResolver().query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    null,
                    null,
                    null,
                    MediaStore.Video.Media.TITLE
            );

            List<FileVideo> list = new ArrayList<>();

            if (cursor.moveToFirst()) {
                do {
                    String url = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
//                    if (url.indexOf("/mnt/extsd/Movie") != -1 || url.indexOf("/storage/emulated/0/Movie") != -1) {
                        String mTilte = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE));
                        long duration = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION));
                        SimpleDateFormat formatter = new SimpleDateFormat("mm:ss");
                        String mTotalTime = formatter.format(duration);
                        String mSize = FileSizeUtils.FormetFileSize(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)));
                        String mPath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
                        int mId = cursor.getInt(cursor.getColumnIndex(MediaStore.Video.Media._ID));
                        list.add(new FileVideo(mTilte, mSize, mTotalTime, mId, mPath));
//                    }
                } while (cursor.moveToNext());
                cursor.close();
            }
            return list;

        }

        @Override
        protected void onPostExecute(List<FileVideo> list) {
            super.onPostExecute(list);
            callback.onSuccess(list);
        }
    }
}
