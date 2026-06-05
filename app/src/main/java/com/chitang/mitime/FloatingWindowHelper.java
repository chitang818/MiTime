package com.chitang.mitime;

import android.content.Context;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

public final class FloatingWindowHelper {
    private static final String TAG = "MiTime";
    public static final String KEY_TIME_FLOATING_WINDOW = "miui_time_floating_window";

    private FloatingWindowHelper() {
    }

    public static boolean isEnabled(Context context) {
        return Settings.System.getInt(
                context.getContentResolver(),
                KEY_TIME_FLOATING_WINDOW,
                0
        ) == 1;
    }

    public static ToggleResult toggle(Context context) {
        return setEnabled(context, !isEnabled(context));
    }

    public static ToggleResult setEnabled(Context context, boolean enabled) {
        if (!PermissionHelper.canWriteSystemSettings(context)) {
            Log.w(TAG, "setEnabled blocked: WRITE_SETTINGS app-op is not allowed");
            return ToggleResult.permissionMissing(isEnabled(context));
        }

        boolean beforeEnabled = isEnabled(context);
        boolean wrote = false;
        RuntimeException writeException = null;
        try {
            wrote = Settings.System.putInt(
                    context.getContentResolver(),
                    KEY_TIME_FLOATING_WINDOW,
                    enabled ? 1 : 0
            );
        } catch (RuntimeException exception) {
            Log.w(TAG, "Failed to write " + KEY_TIME_FLOATING_WINDOW, exception);
            writeException = exception;
        }

        boolean verifiedEnabled = isEnabled(context);
        if (verifiedEnabled != enabled) {
            SystemClock.sleep(250);
            verifiedEnabled = isEnabled(context);
        }

        Log.i(TAG, "setEnabled target=" + enabled
                + ", before=" + beforeEnabled
                + ", putIntReturned=" + wrote
                + ", verified=" + verifiedEnabled
                + ", exception=" + (writeException == null ? "none" : writeException.getClass().getSimpleName()));

        if (verifiedEnabled == enabled) {
            return ToggleResult.success(verifiedEnabled);
        }

        if (isPrivateSettingBlocked(writeException)) {
            return ToggleResult.privateSettingBlocked(verifiedEnabled);
        }
        return ToggleResult.writeFailed(verifiedEnabled);
    }

    private static boolean isPrivateSettingBlocked(RuntimeException exception) {
        return exception instanceof IllegalArgumentException
                && exception.getMessage() != null
                && exception.getMessage().contains("private secure settings");
    }

    public enum UiState {
        ENABLED,
        DISABLED,
        PERMISSION_MISSING
    }

    public static UiState readUiState(Context context) {
        if (!PermissionHelper.canWriteSystemSettings(context)) {
            return UiState.PERMISSION_MISSING;
        }
        return isEnabled(context) ? UiState.ENABLED : UiState.DISABLED;
    }

    public static final class ToggleResult {
        public final boolean success;
        public final boolean permissionMissing;
        public final boolean privateSettingBlocked;
        public final boolean enabled;

        private ToggleResult(
                boolean success,
                boolean permissionMissing,
                boolean privateSettingBlocked,
                boolean enabled
        ) {
            this.success = success;
            this.permissionMissing = permissionMissing;
            this.privateSettingBlocked = privateSettingBlocked;
            this.enabled = enabled;
        }

        static ToggleResult success(boolean enabled) {
            return new ToggleResult(true, false, false, enabled);
        }

        static ToggleResult permissionMissing(boolean actualEnabled) {
            return new ToggleResult(false, true, false, actualEnabled);
        }

        static ToggleResult privateSettingBlocked(boolean actualEnabled) {
            return new ToggleResult(false, false, true, actualEnabled);
        }

        static ToggleResult writeFailed(boolean actualEnabled) {
            return new ToggleResult(false, false, false, actualEnabled);
        }
    }
}
