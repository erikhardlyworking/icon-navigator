package dev.iconnavigator;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class PinReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        int id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0);
        if (id == 0 || !NavigatorWidget.owns(context, id)) return;
        String snapshot = intent.getStringExtra("snapshot");
        if (snapshot != null) {
            new ConfigStore(context).save(id, ConfigStore.decode(snapshot));
            NavigatorWidget.update(context, id);
        }
    }
}
