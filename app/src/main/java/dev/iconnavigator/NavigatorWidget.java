package dev.iconnavigator;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.SizeF;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NavigatorWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) update(context, id);
    }
    @Override public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager, int id, Bundle options) {
        update(context, id);
    }
    @Override public void onDeleted(Context context, int[] ids) {
        for (int id : ids) new ConfigStore(context).delete(id);
    }
    @Override public void onRestored(Context context, int[] oldIds, int[] newIds) {
        ConfigStore store = new ConfigStore(context);
        for (int i = 0; i < oldIds.length; i++) {
            store.save(newIds[i], store.load(oldIds[i])); store.delete(oldIds[i]); update(context, newIds[i]);
        }
    }
    public static boolean owns(Context context, int id) {
        return WidgetProviders.type(context, id) != null;
    }
    public static void update(Context context, int id) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        WidgetConfig config = WidgetProviders.load(context, id);
        Bundle options = manager.getAppWidgetOptions(id);
        java.util.ArrayList<SizeF> sizes = options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES);
        Map<SizeF, RemoteViews> variants = new LinkedHashMap<>();
        if (sizes != null) for (SizeF size : sizes) {
            if (size.getWidth() > 0 && size.getHeight() > 0 && variants.size() < 4)
                variants.put(size, render(context, id, config, 0, size.getWidth(), size.getHeight()));
        }
        if (variants.isEmpty()) {
            float width = Math.max(1, options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, config.type.width));
            float height = Math.max(1, options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, config.type.height));
            manager.updateAppWidget(id, render(context, id, config, 0, width, height));
        } else manager.updateAppWidget(id, new RemoteViews(variants));
    }
    static RemoteViews render(Context context, int id, WidgetConfig config, int page, float width, float height) {
        RemoteViews root = new RemoteViews(context.getPackageName(), R.layout.widget_content);
        root.setInt(R.id.widget_root, "setBackgroundColor", android.graphics.Color.TRANSPARENT);
        // Hosts may reapply onto a view carrying listeners from an older version.
        // Clear actions while absorbing blank taps, so they cannot reach a host's
        // configuration click listener. The host still owns long-press settings.
        for (int target : new int[]{R.id.widget_root, R.id.backdrop, R.id.widget_icons, R.id.empty}) {
            root.setOnClickPendingIntent(target, null);
        }
        root.removeAllViews(R.id.widget_icons);
        var frame = LayoutEngine.frame(config, width, height, page);
        boolean empty = config.icons.isEmpty();
        root.setViewVisibility(R.id.empty, empty ? View.VISIBLE : View.GONE);
        root.setTextViewText(R.id.empty, context.getString(R.string.configure));
        List<LayoutEngine.Placement> placements = frame.placements();
        IconRenderer renderer = new IconRenderer(context);
        var metrics = context.getResources().getDisplayMetrics();
        // Four responsive variants together use <= one screen of pixels, with headroom
        // below the host's 1.5-screen limit. Shared bitmaps avoid inline Binder copies.
        float budget = Math.max(4096, Math.min(600000, metrics.widthPixels * (float) metrics.heightPixels / 4));
        float backdropScale = Math.min(metrics.density, (float) Math.sqrt(budget * .25 / Math.max(1, width * height)));
        android.graphics.Bitmap backdrop = android.graphics.Bitmap.createBitmap(Math.max(1, Math.round(width * backdropScale)),
                Math.max(1, Math.round(height * backdropScale)), android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas backgroundCanvas = new android.graphics.Canvas(backdrop); backgroundCanvas.scale(backdropScale, backdropScale);
        BackdropRenderer.draw(backgroundCanvas, config, renderer, width, height);
        root.setImageViewBitmap(R.id.backdrop, backdrop.asShared());
        double area = 0;
        for (var p : placements) area += config.tileWidth(p.size()) * config.tileHeight(p.size());
        // Bound bitmap memory across up to four responsive variants.
        float scale = (float) Math.min(metrics.density, Math.sqrt(budget * .65 / Math.max(1, area)));
        for (LayoutEngine.Placement p : placements) {
            float tileW = config.tileWidth(p.size());
            float tileH = config.tileHeight(p.size());
            if (p.x() + tileW / 2 <= 0 || p.x() - tileW / 2 >= width) continue;
            RemoteViews icon = new RemoteViews(context.getPackageName(), R.layout.widget_icon);
            icon.setImageViewBitmap(R.id.icon, renderer.tile(config, p, scale).asShared());
            icon.setViewLayoutWidth(R.id.icon, tileW, TypedValue.COMPLEX_UNIT_DIP);
            icon.setViewLayoutHeight(R.id.icon, tileH, TypedValue.COMPLEX_UNIT_DIP);
            icon.setViewLayoutMargin(R.id.icon, RemoteViews.MARGIN_LEFT, p.x() - tileW / 2, TypedValue.COMPLEX_UNIT_DIP);
            icon.setViewLayoutMargin(R.id.icon, RemoteViews.MARGIN_TOP, p.y() - tileH / 2, TypedValue.COMPLEX_UNIT_DIP);
            WidgetConfig.IconEntry entry = config.icons.get(p.index());
            icon.setContentDescription(R.id.icon, entry.label);
            ComponentName component = ComponentName.unflattenFromString(entry.component);
            if (component != null) {
                Intent launch = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(component)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                icon.setOnClickPendingIntent(R.id.icon, PendingIntent.getActivity(context, 0, launch,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            }
            root.addView(R.id.widget_icons, icon);
        }
        return root;
    }
}
