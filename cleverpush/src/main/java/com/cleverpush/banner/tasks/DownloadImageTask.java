package com.cleverpush.banner.tasks;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import android.widget.ImageView;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;

public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
    WeakReference<ImageView> imageView;

    public DownloadImageTask(ImageView imageView) {
        this.imageView = new WeakReference<>(imageView);
    }

    protected Bitmap doInBackground(String... args) {
        String url = args[0];
        try {
            InputStream in = new URL(url).openStream();
            return BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            return null;
        }
    }

    protected void onPostExecute(Bitmap result) {
        ImageView view = imageView.get();
        if(view != null && result != null) {
            view.setImageBitmap(result);
        }
    }

    public static void execute(ImageView imageView, String url) {
        DownloadImageTask task = new DownloadImageTask(imageView);

        task.execute(url);
    }
}