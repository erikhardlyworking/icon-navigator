package dev.iconnavigator;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import java.util.ArrayList;

public final class WidgetProviders {
    private WidgetProviders() {}
    public static ComponentName component(Context context, WidgetConfig.Type type) {
        return new ComponentName(context.getPackageName(), "dev.iconnavigator." + type.provider);
    }
    public static WidgetConfig.Type type(Context context, int id) {
        var info = AppWidgetManager.getInstance(context).getAppWidgetInfo(id);
        if (info != null) for (var type : WidgetConfig.Type.values()) if (component(context, type).equals(info.provider)) return type;
        return null;
    }
    public static int[] ids(Context context) {
        ArrayList<Integer> ids = new ArrayList<>();
        for (var type : WidgetConfig.Type.values()) for (int id : AppWidgetManager.getInstance(context).getAppWidgetIds(component(context, type))) ids.add(id);
        return ids.stream().mapToInt(Integer::intValue).toArray();
    }
    public static WidgetConfig load(Context context, int id) {
        ConfigStore store = new ConfigStore(context); WidgetConfig c = store.load(id);
        var type = type(context, id); if (type != null) c.selectType(type, !store.exists(id));
        return c;
    }
}
