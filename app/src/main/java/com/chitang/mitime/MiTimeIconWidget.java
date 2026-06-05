package com.chitang.mitime;

import android.appwidget.AppWidgetManager;
import android.content.Context;

public class MiTimeIconWidget extends MiTimeWidget {
    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, manager, appWidgetId, true);
        }
    }
}
