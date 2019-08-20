package br.com.luansilveira.savefile.utils;

import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.provider.MediaStore;

public class ThumbnailTask extends AsyncTask<Void, Void, Bitmap> {

    private OnLoadListener listener;

    private String filename;

    public ThumbnailTask(String filename) {
        this.filename = filename;
    }

    @Override
    protected Bitmap doInBackground(Void... params) {
        Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(this.filename, MediaStore.Images.Thumbnails.MINI_KIND);
        ImageCache.getCache().addToCache(this.filename, bitmap);

        return bitmap;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (this.listener != null) this.listener.onLoad(bitmap);
    }

    public ThumbnailTask setOnLoadListener(OnLoadListener listener) {
        this.listener = listener;
        return this;
    }

    public interface OnLoadListener {
        void onLoad(Bitmap b);
    }
}
