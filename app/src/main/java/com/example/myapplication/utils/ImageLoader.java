package com.example.myapplication.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.util.LruCache;

public class ImageLoader {

    private static ImageLoader instance;
    private final LruCache<String, Bitmap> cache;
    private final ExecutorService executor;
    private final Handler mainHandler;

    private ImageLoader() {
        // 1/8 de la memoria disponible
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;

        cache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount() / 1024;
            }
        };

        executor = Executors.newFixedThreadPool(4);
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static ImageLoader getInstance() {
        if (instance == null) {
            synchronized (ImageLoader.class) {
                if (instance == null) instance = new ImageLoader();
            }
        }
        return instance;
    }

    public void load(String url, ImageView target,
                     @DrawableRes int placeholderRes,
                     @DrawableRes int errorRes) {

        if (placeholderRes != 0) target.setImageResource(placeholderRes);
        target.setTag(url);

        Bitmap cached = cache.get(url);
        if (cached != null) {
            target.setImageBitmap(cached);
            return;
        }

        executor.execute(() -> {
            Bitmap bmp = null;
            try {
                bmp = downloadBitmap(url);
                if (bmp != null) cache.put(url, bmp);
            } catch (Exception ignored) { }

            Bitmap finalBmp = bmp;
            mainHandler.post(() -> {
                if (!url.equals(target.getTag())) return;
                if (finalBmp != null) target.setImageBitmap(finalBmp);
                else if (errorRes != 0) target.setImageResource(errorRes);
            });
        });
    }

    private Bitmap downloadBitmap(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setDoInput(true);
        conn.connect();

        try (InputStream is = conn.getInputStream()) {
            return BitmapFactory.decodeStream(is);
        } finally {
            conn.disconnect();
        }
    }
}
