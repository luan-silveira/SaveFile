package br.com.luansilveira.savefile.utils;

import android.graphics.Bitmap;
import android.util.LruCache;

public class ImageCache extends LruCache<String, Bitmap> {

    private static ImageCache mInstance;

    public ImageCache() {
        super((int) Runtime.getRuntime().maxMemory());
    }

    /**
     * @param maxSize for caches that do not override {@link #sizeOf}, this is
     *                the maximum number of entries in the cache. For all other caches,
     *                this is the maximum sum of the sizes of the entries in this cache.
     */
    public ImageCache(int maxSize) {
        super(maxSize);
    }

    public static ImageCache getCache() {
        if (mInstance == null) mInstance = new ImageCache();
        return mInstance;
    }

    @Override
    protected int sizeOf(String key, Bitmap value) {
        return value.getByteCount();
    }

    protected void addToCache(String key, Bitmap bitmap) {
        if (key == null || bitmap == null) return;
        if (this.get(key) == null) this.put(key, bitmap);
    }

    protected Bitmap getFromCache(String key) {
        return this.get(key);
    }

}
