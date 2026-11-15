package dev.iconnavigator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Platform-independent widget configuration. Dimensions are logical dp. */
public final class WidgetConfig {
    public static final int MAX_ICONS = 400;
    public static final int MAX_LAYERS = 6, MAX_LAYER_ICONS = 16;
    public enum Layout { GRID, LEFT_HAND, RIGHT_HAND }
    public enum Type {
        GRID_COMPACT("Grid", Layout.GRID, 160, 240, 3, 4, "NavigatorWidget"),
        GRID_SQUARE("Grid · square", Layout.GRID, 240, 240, 4, 4, "SquareWidget"),
        GRID_WIDE("Grid · wide", Layout.GRID, 320, 160, 6, 2, "WideWidget"),
        GRID_HORIZONTAL("Grid · horizontal strip", Layout.GRID, 320, 64, 8, 1, "HorizontalWidget"),
        GRID_VERTICAL("Grid · vertical strip", Layout.GRID, 64, 400, 1, 10, "VerticalWidget"),
        LEFT_THUMB("Left thumb", Layout.LEFT_HAND, 260, 340, 3, 4, "LeftThumbWidget"),
        RIGHT_THUMB("Right thumb", Layout.RIGHT_HAND, 380, 420, 3, 4, "RightThumbWidget"),
        THUMB("Thumb icon selector", Layout.RIGHT_HAND, 160, 240, 3, 4, "ThumbWidget");
        public final String title, provider;
        public final Layout layout;
        public final int width, height, columns, rows;
        Type(String title, Layout layout, int width, int height, int columns, int rows, String provider) {
            this.title = title; this.layout = layout; this.width = width; this.height = height;
            this.columns = columns; this.rows = rows; this.provider = provider;
        }
    }
    public enum BackgroundArt { NONE, ICON_WASH, ICON_COLLAGE, GRADIENT, IMAGE }
    public enum Effect { ORIGINAL, GRAYSCALE, TINT }
    public Type type = Type.GRID_COMPACT;
    public Layout layout = Layout.GRID;
    public int columns = 3, rows = 4, exaggeration = 0;
    public final List<Integer> layers = new ArrayList<>(List.of(2, 5, 7, 9));
    public final List<Integer> layerScales = new ArrayList<>(List.of(150, 121, 105, 100));
    public int thumbCenterHeight = 17, thumbLayerSpacing = 67;
    public boolean thumbCustomReach = true;
    public int thumbInnerReach = 34, thumbOuterReach = 95;
    public float thumbEdgeInset = 0;
    public float iconBorder = 0, widgetBorder = 0, cornerRadius = 16;
    public int iconBorderColor = 0xffdddddd, widgetBorderColor = 0xffdddddd, artOpacity = 25;
    public BackgroundArt backgroundArt = BackgroundArt.NONE;
    public int gradientEnd = 0xff345b72, gradientAngle = 45;
    public String backgroundImage = "";
    public float iconSize = 40, paddingX = 4, paddingY = 4, outerPadding = 0;
    public int iconOpacity = 100, textColor = 0xffffffff;
    public boolean automaticTextColor = true, labelShadow = true;
    public int background = 0xff000000, opacity = 0, tint = 0xffeeeeee;
    public boolean labels = false;
    public boolean useDefaultLabels = true, defaultLabels = false;
    public Effect effect = Effect.ORIGINAL;
    public final List<IconEntry> icons = new ArrayList<>();

    public void selectType(Type selected, boolean resetDimensions) {
        Layout previous = layout;
        type = selected; layout = type.layout == Layout.GRID ? Layout.GRID
                : previous != Layout.GRID ? previous : type.layout;
        if (resetDimensions && previous == Layout.GRID && layout != Layout.GRID && iconSize == 40) iconSize = 47;
        if (resetDimensions) { columns = type.columns; rows = type.rows; }
    }
    public static Type[] offeredTypes() { return new Type[]{Type.GRID_COMPACT, Type.THUMB}; }
    public String title() { return thumb() ? "Thumb icon selector" : "Grid"; }
    public void setLayerCount(int count) {
        count = clamp(count, 1, MAX_LAYERS);
        while (layers.size() > count) layers.remove(layers.size() - 1);
        while (layers.size() < count) layers.add(Math.min(MAX_LAYER_ICONS, layers.size() * 2 + 3));
        normalize();
    }
    public boolean thumb() { return layout != Layout.GRID; }
    public int capacity() { return thumb() ? layers.stream().mapToInt(Integer::intValue).sum() : columns * rows; }
    public boolean showLabels() { return useDefaultLabels ? defaultLabels : labels; }
    public boolean labelsAt(float size) { return showLabels() && size >= 34; }
    public float tileWidth(float size) { return Math.max(size, labelsAt(size) ? 34 : size); }
    public float tileHeight(float size) { return size + (labelsAt(size) ? 16 : 0); }
    public int backgroundArgb() { return (Math.round(opacity * 2.55f) << 24) | (background & 0xffffff); }
    public int foregroundColor() {
        if (!automaticTextColor) return textColor;
        if (effect == Effect.TINT) return tint;
        int r = (background >> 16) & 255, g = (background >> 8) & 255, b = background & 255;
        return opacity >= 70 && (.2126f * r + .7152f * g + .0722f * b) > 160 ? 0xff202020 : 0xffffffff;
    }
    public static String appKey(String component) {
        int slash = component.indexOf('/'); return slash > 0 ? component.substring(0, slash) : component;
    }
    public boolean containsApp(String component, IconEntry except) {
        String key = appKey(component);
        for (var e : icons) if (e != except && appKey(e.component).equals(key)) return true;
        return false;
    }
    public boolean addIcon(IconEntry entry) {
        if (icons.size() >= MAX_ICONS || containsApp(entry.component, null)) return false;
        icons.add(entry); return true;
    }
    public void normalize() {
        columns = clamp(columns, 1, 12); rows = clamp(rows, 1, 12);
        if (layers.isEmpty()) layers.add(2);
        while (layers.size() > MAX_LAYERS) layers.remove(layers.size() - 1);
        for (int i = 0; i < layers.size(); i++) layers.set(i, clamp(layers.get(i), 1, MAX_LAYER_ICONS));
        while (layerScales.size() > layers.size()) layerScales.remove(layerScales.size() - 1);
        while (layerScales.size() < layers.size()) layerScales.add(100);
        for (int i = 0; i < layerScales.size(); i++) layerScales.set(i, clamp(layerScales.get(i), 50, 150));
        thumbCenterHeight = clamp(thumbCenterHeight, 0, 100);
        thumbLayerSpacing = clamp(thumbLayerSpacing, 25, 150);
        thumbInnerReach = clamp(thumbInnerReach, 1, 99);
        thumbOuterReach = clamp(thumbOuterReach, thumbInnerReach + 1, 100);
        thumbEdgeInset = bounded(thumbEdgeInset, 0, 96);
        iconSize = bounded(iconSize, 12, 96); paddingX = bounded(paddingX, 0, 32);
        paddingY = bounded(paddingY, 0, 32); outerPadding = bounded(outerPadding, 0, 64);
        iconOpacity = clamp(iconOpacity, 20, 100);
        exaggeration = clamp(exaggeration, 0, 200); opacity = clamp(opacity, 0, 100);
        iconBorder = bounded(iconBorder, 0, 8); widgetBorder = bounded(widgetBorder, 0, 12);
        cornerRadius = bounded(cornerRadius, 0, 48); artOpacity = clamp(artOpacity, 0, 100);
        gradientAngle = clamp(gradientAngle, 0, 360);
        HashSet<String> seen = new HashSet<>();
        icons.removeIf(e -> e.component == null || e.component.isBlank() || !seen.add(appKey(e.component)));
        while (icons.size() > MAX_ICONS) icons.remove(icons.size() - 1);
        for (var e : icons) e.size = bounded(e.size, 0, 128);
    }
    public void move(int from, int to) {
        if (from >= 0 && from < icons.size() && to >= 0 && to < icons.size() && from != to)
            icons.add(to, icons.remove(from));
    }
    public static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static float bounded(float value, float min, float max) {
        return Float.isFinite(value) ? Math.max(min, Math.min(max, value)) : min;
    }
    public static final class IconEntry {
        public final String id;
        public String component, label, image = "";
        public float size;
        public IconEntry(String id, String component, String label) { this.id = id; this.component = component; this.label = label; }
    }
}
