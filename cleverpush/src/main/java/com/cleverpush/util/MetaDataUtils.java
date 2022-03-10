package com.cleverpush.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

public class MetaDataUtils {
    static Bundle getManifestMetaBundle(Context context) {
        ApplicationInfo ai;
        try {
            ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            return ai.metaData;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("CleverPush", "Manifest application info not found", e);
        }
        return null;
    }

    public static boolean getManifestMetaBoolean(Context context, String metaName) {
        Bundle bundle = getManifestMetaBundle(context);
        if (bundle != null) {
            return bundle.getBoolean(metaName);
        }
        return false;
    }

    public static String getChannelId(Context paramContext) {
        ApplicationInfo localApplicationInfo;
        try {
            localApplicationInfo = paramContext.getPackageManager().getApplicationInfo(paramContext.getPackageName(), 128);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        if ((localApplicationInfo == null) || (localApplicationInfo.metaData == null))
        {
            return null;
        }
        return localApplicationInfo.metaData.getString("CLEVERPUSH_CHANNEL_ID");
    }
}
