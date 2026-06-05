package com.chitang.mitime;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

public final class PermissionHelper {
    private PermissionHelper() {
    }

    public static boolean canWriteSystemSettings(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        return Settings.System.canWrite(context);
    }
}
