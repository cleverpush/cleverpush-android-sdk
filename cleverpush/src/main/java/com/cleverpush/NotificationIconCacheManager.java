package com.cleverpush;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class NotificationIconCacheManager {

    private static final int MAX_ICONS = 10;
    private static LruCache<String, Bitmap> iconCache;

    public static void init(Context context) {
        if (iconCache == null) {
            final int cacheSize = 4 * 1024 * 1024; // 4MB
            iconCache = new LruCache<String, Bitmap>(cacheSize) {
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    return bitmap.getByteCount();
                }
            };
        }
    }

    public static void cacheIcon(Context context, String key, Bitmap bitmap) {
        if (getIconFromMemory(key) == null) {
            iconCache.put(key, bitmap);
            saveIconToDisk(context, key, bitmap);
            trimCache();
        }
    }

    public static Bitmap getIcon(Context context, String key) {
        Bitmap memoryIcon = getIconFromMemory(key);
        if (memoryIcon != null) {
            return memoryIcon;
        }
        Bitmap diskIcon = getIconFromDisk(context, key);
        if (diskIcon != null) {
            iconCache.put(key, diskIcon); // restore to memory
        }
        return diskIcon;
    }

    private static Bitmap getIconFromMemory(String key) {
        return iconCache.get(key);
    }

    private static void trimCache() {
        if (iconCache.size() > MAX_ICONS) {
            Iterator<String> iterator = iconCache.snapshot().keySet().iterator();
            while (iconCache.size() > MAX_ICONS && iterator.hasNext()) {
                String oldestKey = iterator.next();
                iconCache.remove(oldestKey);
            }
        }
    }

    private static String getSanitizedFileName(String url) {
        try {
            // Create MD5 hash of the URL to get a consistent, safe filename
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(url.getBytes());
            
            // Convert the hash to a hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // Fallback to a simple sanitization if MD5 is not available
            return url.replaceAll("[^a-zA-Z0-9]", "_");
        }
    }

    private static void saveIconToDisk(Context context, String key, Bitmap bitmap) {
        try {
            String sanitizedKey = getSanitizedFileName(key);
            File file = new File(context.getCacheDir(), sanitizedKey + ".png");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Bitmap getIconFromDisk(Context context, String key) {
        String sanitizedKey = getSanitizedFileName(key);
        File file = new File(context.getCacheDir(), sanitizedKey + ".png");
        if (file.exists()) {
            return BitmapFactory.decodeFile(file.getAbsolutePath());
        }
        return null;
    }
}
