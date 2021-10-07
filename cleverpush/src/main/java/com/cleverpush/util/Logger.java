package com.cleverpush.util;

import android.util.Log;

public class Logger {

    public int d(String tag, String msg) {
        Log.d(tag, msg);
        return 0;
    }

    public int i(String tag, String msg) {
        Log.i(tag, msg);
        return 0;
    }

    public int w(String tag, String msg) {
        Log.w(tag, msg);
        return 0;
    }

    public int e(String tag, String msg) {
        Log.e(tag, msg);
        return 0;
    }

    public int e(String tag, String msg, Throwable t) {
        Log.e(tag, msg, t);
        return 0;
    }
}
