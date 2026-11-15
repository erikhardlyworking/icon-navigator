package dev.iconnavigator;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class ConfigStore {
    private final Context context;
    public ConfigStore(Context context) { this.context = context.getApplicationContext(); }
    public boolean exists(int id) { return context.getSharedPreferences("widgets", 0).contains("widget_" + id); }
    public WidgetConfig load(int id) {
        String stored=context.getSharedPreferences("widgets", 0).getString("widget_" + id, null);
        WidgetConfig config = stored == null ? new WidgetConfig() : decode(stored);
        config.defaultLabels = defaultLabels();
        return config;
    }
    public boolean defaultLabels() { return context.getSharedPreferences("widgets", 0).getBoolean("default_labels", false); }
    public void defaultLabels(boolean enabled) { context.getSharedPreferences("widgets", 0).edit().putBoolean("default_labels", enabled).apply(); }
    public void save(int id, WidgetConfig config) {
        config.normalize();
        context.getSharedPreferences("widgets", 0).edit().putString("widget_" + id, encode(config)).apply();
    }
    public void delete(int id) {
        context.getSharedPreferences("widgets", 0).edit().remove("widget_" + id).remove("page_" + id).apply();
    }
    public static String encode(WidgetConfig c) {
        try {
            JSONObject j = new JSONObject();
            j.put("version", 7).put("columns", c.columns).put("rows", c.rows).put("size", c.iconSize)
                    .put("paddingX", c.paddingX).put("paddingY", c.paddingY).put("outer", c.outerPadding)
                    .put("background", c.background).put("opacity", c.opacity).put("tint", c.tint)
                    .put("labels", c.labels).put("useDefaultLabels", c.useDefaultLabels).put("defaultLabels", c.defaultLabels)
                    .put("layout", c.layout.name()).put("type", c.type.name()).put("effect", c.effect.name());
            j.put("layers", new JSONArray(c.layers)).put("layerScales", new JSONArray(c.layerScales)).put("iconBorder", c.iconBorder)
                    .put("thumbCenterHeight", c.thumbCenterHeight).put("thumbLayerSpacing", c.thumbLayerSpacing).put("thumbEdgeInset", c.thumbEdgeInset)
                    .put("thumbCustomReach", c.thumbCustomReach).put("thumbInnerReach", c.thumbInnerReach).put("thumbOuterReach", c.thumbOuterReach)
                    .put("widgetBorder", c.widgetBorder).put("iconBorderColor", c.iconBorderColor)
                    .put("widgetBorderColor", c.widgetBorderColor).put("cornerRadius", c.cornerRadius)
                    .put("backgroundArt", c.backgroundArt.name()).put("artOpacity", c.artOpacity)
                    .put("exaggeration", c.exaggeration).put("gradientEnd", c.gradientEnd).put("gradientAngle", c.gradientAngle)
                    .put("backgroundImage", c.backgroundImage).put("iconOpacity", c.iconOpacity)
                    .put("textColor", c.textColor).put("automaticTextColor", c.automaticTextColor).put("labelShadow", c.labelShadow);
            JSONArray entries = new JSONArray();
            for (WidgetConfig.IconEntry e : c.icons) entries.put(new JSONObject().put("id", e.id)
                    .put("component", e.component).put("label", e.label).put("image", e.image)
                    .put("size", e.size));
            j.put("icons", entries);
            return j.toString();
        } catch (JSONException e) { throw new IllegalStateException("Cannot encode widget settings", e); }
    }
    public static WidgetConfig decode(String json) {
        WidgetConfig c = new WidgetConfig();
        try {
            JSONObject j = new JSONObject(json);
            c.columns = j.optInt("columns", 3); c.rows = j.optInt("rows", 4);
            c.iconSize = (float) j.optDouble("size", 40); c.paddingX = (float) j.optDouble("paddingX", 4);
            c.paddingY = (float) j.optDouble("paddingY", 4); c.outerPadding = (float) j.optDouble("outer", 0);
            c.background = j.optInt("background", 0xff000000); c.opacity = j.optInt("opacity", 0);
            c.iconOpacity = j.optInt("iconOpacity", 100); c.textColor = j.optInt("textColor", 0xffffffff);
            c.automaticTextColor = j.optBoolean("automaticTextColor", true); c.labelShadow = j.optBoolean("labelShadow", true);
            c.tint = j.optInt("tint", 0xffeeeeee); c.labels = j.optBoolean("labels");
            c.useDefaultLabels = j.optBoolean("useDefaultLabels", !j.has("labels"));
            c.defaultLabels = j.optBoolean("defaultLabels", false);
            c.thumbCenterHeight = j.optInt("thumbCenterHeight", 0);
            c.thumbLayerSpacing = j.optInt("thumbLayerSpacing", 100);
            c.thumbCustomReach = j.optBoolean("thumbCustomReach", false);
            c.thumbInnerReach = j.optInt("thumbInnerReach", 25);
            c.thumbOuterReach = j.optInt("thumbOuterReach", 100);
            c.thumbEdgeInset = (float) j.optDouble("thumbEdgeInset", 0);
            String legacyLayout = j.optString("layout", "GRID");
            WidgetConfig.Type migrated = legacyLayout.equals("LEFT_HAND") ? WidgetConfig.Type.LEFT_THUMB
                    : legacyLayout.equals("RIGHT_HAND") ? WidgetConfig.Type.RIGHT_THUMB : WidgetConfig.Type.GRID_COMPACT;
            try { migrated = WidgetConfig.Type.valueOf(j.optString("type", migrated.name())); } catch (IllegalArgumentException ignored) { }
            c.selectType(migrated, false);
            if (c.thumb()) c.layout = legacyLayout.equals("LEFT_HAND") ? WidgetConfig.Layout.LEFT_HAND
                    : legacyLayout.equals("RIGHT_HAND") ? WidgetConfig.Layout.RIGHT_HAND : migrated.layout;
            try { c.effect = WidgetConfig.Effect.valueOf(j.optString("effect", "ORIGINAL")); } catch (IllegalArgumentException ignored) { }
            c.exaggeration = j.optInt("exaggeration", 0);
            JSONArray layers = j.optJSONArray("layers");
            if (layers != null) { c.layers.clear(); for (int i = 0; i < Math.min(WidgetConfig.MAX_LAYERS, layers.length()); i++) c.layers.add(layers.optInt(i, 1)); }
            JSONArray scales = j.optJSONArray("layerScales");
            c.layerScales.clear();
            if (scales != null) for (int i = 0; i < Math.min(WidgetConfig.MAX_LAYERS, scales.length()); i++) c.layerScales.add(scales.optInt(i, 100));
            c.gradientEnd = j.optInt("gradientEnd", 0xff345b72); c.gradientAngle = j.optInt("gradientAngle", 45);
            c.backgroundImage = j.optString("backgroundImage", "");
            c.iconBorder = (float) j.optDouble("iconBorder", 0); c.widgetBorder = (float) j.optDouble("widgetBorder", 0);
            c.iconBorderColor = j.optInt("iconBorderColor", 0xffdddddd); c.widgetBorderColor = j.optInt("widgetBorderColor", 0xffdddddd);
            c.cornerRadius = (float) j.optDouble("cornerRadius", 16); c.artOpacity = j.optInt("artOpacity", 25);
            try { c.backgroundArt = WidgetConfig.BackgroundArt.valueOf(j.optString("backgroundArt", "NONE")); } catch (IllegalArgumentException ignored) { }
            JSONArray entries = j.optJSONArray("icons");
            if (entries != null) for (int i = 0; i < Math.min(entries.length(), WidgetConfig.MAX_ICONS); i++) {
                JSONObject entry = entries.optJSONObject(i);
                if (entry == null || entry.optString("component").isEmpty()) continue;
                WidgetConfig.IconEntry e = new WidgetConfig.IconEntry(entry.optString("id", "restored-" + i),
                        entry.optString("component"), entry.optString("label", "App"));
                e.image = entry.optString("image");
                e.size = (float) entry.optDouble("size", 0);
                c.icons.add(e);
            }
        } catch (JSONException ignored) { /* Malformed data recovers to safe defaults. */ }
        c.normalize();
        return c;
    }
}
