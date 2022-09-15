package com.cleverpush.util;

import android.util.Log;

import com.cleverpush.listener.LogListener;

public class Logger {

    private static LogListener logListener = null;

    public static void setLogListener(LogListener logListener) {
        Logger.logListener = logListener;
    }

    private static void executeLogListener(String tag, String msg, Throwable t) {
        if (logListener == null) {
            return;
        }

        String message = tag + ": " + msg;
        if (t != null) {
            message += ": " + t.getMessage();
        }
        logListener.log(message);
    }

    public static int d(String tag, String msg) {
        Log.d(tag, msg);
        executeLogListener(tag, msg, null);
        return 0;
    }

    public static int d(String tag, String msg, Throwable t) {
        Log.d(tag, msg, t);
        executeLogListener(tag, msg, t);
        return 0;
    }

    public static int i(String tag, String msg) {
        Log.i(tag, msg);
        executeLogListener(tag, msg,  null);
        return 0;
    }

    public static int w(String tag, String msg) {
        Log.w(tag, msg);
        executeLogListener(tag, msg, null);
        return 0;
    }

    public static int e(String tag, String msg) {
        Log.e(tag, msg);
        executeLogListener(tag, msg, null);
        return 0;
    }

    public static int e(String tag, String msg, Throwable t) {
        Log.e(tag, msg, t);
        executeLogListener(tag, msg, t);
        return 0;
    }
}