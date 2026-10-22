package com.cleverpush.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public final class BitmapUtils {

    private static final int BUFFER_SIZE = 16 * 1024;

    private BitmapUtils() {
    }

    /**
     * Downloads and decodes an image using sampled bitmap decoding.
     *
     * @param imageUrl  URL of the image
     * @param reqWidth  requested width in pixels
     * @param reqHeight requested height in pixels
     * @param timeout   connection and read timeout in milliseconds
     * @return decoded bitmap, or null if decoding fails
     */
    public static Bitmap decodeSampledBitmapFromUrl(
            String imageUrl,
            int reqWidth,
            int reqHeight,
            int timeout
    ) throws IOException {
        if (TextUtils.isEmpty(imageUrl)
                || "null".equalsIgnoreCase(imageUrl)
                || reqWidth <= 0
                || reqHeight <= 0) {
            return null;
        }

        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection)
                    new URL(imageUrl).openConnection();

            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setDoInput(true);
            connection.connect();

            try (InputStream input = connection.getInputStream()) {
                return decodeStream(input, reqWidth, reqHeight);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Returns the largest power-of-two sample size that keeps the decoded
     * bitmap at least as large as the requested dimensions, where possible.
     */
    public static int calculateInSampleSize(
            int srcWidth,
            int srcHeight,
            int reqWidth,
            int reqHeight
    ) {
        if (srcWidth <= 0 || srcHeight <= 0
                || reqWidth <= 0 || reqHeight <= 0) {
            return 1;
        }

        int inSampleSize = 1;

        while (inSampleSize <= Integer.MAX_VALUE / 2) {
            int nextSampleSize = inSampleSize * 2;

            if (srcWidth / nextSampleSize < reqWidth
                    || srcHeight / nextSampleSize < reqHeight) {
                break;
            }

            inSampleSize = nextSampleSize;
        }

        return inSampleSize;
    }

    /**
     * Decodes an input stream into a sampled bitmap.
     *
     * The stream is consumed but not closed by this method.
     */
    public static Bitmap decodeStream(
            InputStream inputStream,
            int reqWidth,
            int reqHeight
    ) throws IOException {
        if (inputStream == null || reqWidth <= 0 || reqHeight <= 0) {
            return null;
        }

        return decodeByteArray(readBytes(inputStream), reqWidth, reqHeight);
    }

    /**
     * Decodes a local image file into a sampled bitmap.
     */
    public static Bitmap decodeFile(
            String path,
            int reqWidth,
            int reqHeight
    ) {
        if (TextUtils.isEmpty(path) || reqWidth <= 0 || reqHeight <= 0) {
            return null;
        }

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, bounds);

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSizeForFit(
                bounds.outWidth,
                bounds.outHeight,
                reqWidth,
                reqHeight
        );
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        return scaleToFit(bitmap, reqWidth, reqHeight);
    }

    /**
     * Decodes an image byte array into a sampled bitmap.
     */
    public static Bitmap decodeByteArray(
            byte[] data,
            int reqWidth,
            int reqHeight
    ) {
        if (data == null || data.length == 0
                || reqWidth <= 0 || reqHeight <= 0) {
            return null;
        }

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;

        BitmapFactory.decodeByteArray(data, 0, data.length, bounds);

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSizeForFit(
                bounds.outWidth,
                bounds.outHeight,
                reqWidth,
                reqHeight
        );
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap bitmap = BitmapFactory.decodeByteArray(
                data, 0, data.length, options
        );

        return scaleToFit(bitmap, reqWidth, reqHeight);
    }

    /**
     * Calculates a sample size that minimizes decoding while allowing the
     * final bitmap to fit within the requested dimensions.
     */
    private static int sampleSizeForFit(
            int srcWidth,
            int srcHeight,
            int reqWidth,
            int reqHeight
    ) {
        if (srcWidth <= 0 || srcHeight <= 0
                || reqWidth <= 0 || reqHeight <= 0) {
            return 1;
        }

        float scale = Math.min(
                reqWidth / (float) srcWidth,
                reqHeight / (float) srcHeight
        );

        int sampleWidth = srcWidth;
        int sampleHeight = srcHeight;

        if (scale < 1f) {
            sampleWidth = Math.max(
                    1, Math.round(srcWidth * scale)
            );
            sampleHeight = Math.max(
                    1, Math.round(srcHeight * scale)
            );
        }

        return calculateInSampleSize(
                srcWidth,
                srcHeight,
                sampleWidth,
                sampleHeight
        );
    }

    /**
     * Calculates target dimensions for an image displayed across a fraction
     * of the screen width. Preserves aspect ratio and caps height at the
     * screen height.
     *
     * @return width and height in pixels, in that order
     */
    public static int[] targetSizeForWidthFraction(
            Context context,
            float widthFraction,
            float sourceWidth,
            float sourceHeight
    ) {
        if (context == null) {
            return new int[]{1, 1};
        }

        DisplayMetrics metrics =
                context.getResources().getDisplayMetrics();

        float fraction = Math.max(0f, Math.min(1f, widthFraction));

        int reqWidth = Math.max(
                1, Math.round(metrics.widthPixels * fraction)
        );
        int reqHeight = Math.max(1, metrics.heightPixels);

        if (sourceWidth > 0f && sourceHeight > 0f) {
            int aspectHeight = Math.max(
                    1,
                    Math.round(reqWidth * (sourceHeight / sourceWidth))
            );

            reqHeight = Math.min(aspectHeight, metrics.heightPixels);
        }

        return new int[]{reqWidth, reqHeight};
    }

    /**
     * Returns the larger platform-defined notification large-icon dimension.
     */
    public static int notificationLargeIconPx(Context context) {
        if (context == null) {
            return 1;
        }

        Resources resources = context.getResources();

        int width = resources.getDimensionPixelSize(
                android.R.dimen.notification_large_icon_width
        );

        int height = resources.getDimensionPixelSize(
                android.R.dimen.notification_large_icon_height
        );

        return Math.max(1, Math.max(width, height));
    }

    /**
     * Scales a freshly decoded bitmap down to fit the requested dimensions,
     * preserving aspect ratio. The original bitmap is recycled if a
     * different scaled bitmap is created.
     */
    static Bitmap scaleToFit(
            Bitmap bitmap,
            int reqWidth,
            int reqHeight
    ) {
        if (bitmap == null || reqWidth <= 0 || reqHeight <= 0) {
            return bitmap;
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= reqWidth && height <= reqHeight) {
            return bitmap;
        }

        float scale = Math.min(
                reqWidth / (float) width,
                reqHeight / (float) height
        );

        int dstWidth = Math.max(1, Math.round(width * scale));
        int dstHeight = Math.max(1, Math.round(height * scale));

        Bitmap scaled = Bitmap.createScaledBitmap(
                bitmap, dstWidth, dstHeight, true
        );

        if (scaled != bitmap) {
            bitmap.recycle();
        }

        return scaled;
    }

    /**
     * Reads all bytes from an input stream.
     *
     * The stream is consumed but not closed by this method.
     */
    private static byte[] readBytes(InputStream inputStream)
            throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;

        while ((read = inputStream.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }

        return output.toByteArray();
    }
}
