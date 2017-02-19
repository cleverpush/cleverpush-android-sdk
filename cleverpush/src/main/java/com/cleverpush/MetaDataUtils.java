package com.cleverpush;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

public class MetaDataUtils {
    public static String getChannelId(Context paramContext) {
        ApplicationInfo localApplicationInfo = null;
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
