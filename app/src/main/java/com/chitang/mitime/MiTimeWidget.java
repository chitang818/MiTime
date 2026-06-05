package com.chitang.mitime;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

public class MiTimeWidget extends AppWidgetProvider {
    private static final String TAG = "MiTime";
    public static final String ACTION_TOGGLE_WIDGET = "com.chitang.mitime.action.TOGGLE_WIDGET";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_TOGGLE_WIDGET.equals(intent.getAction())) {
            Log.i(TAG, "Widget toggle action received");
            FloatingWindowHelper.toggle(context);
            updateAllWidgets(context);
            return;
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, manager, appWidgetId, false);
        }
    }

    public static void updateAllWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] wideIds = manager.getAppWidgetIds(new ComponentName(context, MiTimeWidget.class));
        for (int id : wideIds) {
            updateWidget(context, manager, id, false);
        }

        int[] iconIds = manager.getAppWidgetIds(new ComponentName(context, MiTimeIconWidget.class));
        for (int id : iconIds) {
            updateWidget(context, manager, id, true);
        }
    }

    static void updateWidget(Context context, AppWidgetManager manager, int appWidgetId, boolean iconOnly) {
        RemoteViews views = new RemoteViews(
                context.getPackageName(),
                iconOnly ? R.layout.widget_mitime_icon : R.layout.widget_mitime
        );

        FloatingWindowHelper.UiState state = FloatingWindowHelper.readUiState(context);
        boolean enabled = state == FloatingWindowHelper.UiState.ENABLED;
        boolean canWrite = state != FloatingWindowHelper.UiState.PERMISSION_MISSING;

        views.setTextViewText(R.id.widgetTitle, context.getString(R.string.time_floating_window));
        views.setTextViewText(
                R.id.widgetStatus,
                context.getString(canWrite
                        ? (enabled ? R.string.status_enabled_short : R.string.status_disabled_short)
                        : R.string.status_write_settings_off_short)
        );
        views.setImageViewResource(R.id.widgetIcon, enabled ? R.drawable.ic_tile_on : R.drawable.ic_tile_off);
        views.setInt(R.id.widgetRoot, "setBackgroundResource",
                enabled ? R.drawable.bg_widget_on : R.drawable.bg_widget_off);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                new Intent(context, iconOnly ? MiTimeIconWidget.class : MiTimeWidget.class)
                        .setAction(ACTION_TOGGLE_WIDGET),
                pendingIntentFlags()
        );
        views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent);
        manager.updateAppWidget(appWidgetId, views);
    }

    static int pendingIntentFlags() {
        return PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
    }
}
