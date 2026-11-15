package dev.iconnavigator;

import java.util.ArrayList;
import java.util.List;

/** Shared, deterministic geometry. Resizing never changes slot counts or order. */
public final class LayoutEngine {
    private LayoutEngine() {}
    public record Placement(int index, float x, float y, float size, float depth, float alpha) {}
    public record Frame(List<Placement> placements, int capacity, float contentHeight, float contentWidth,
                        float iconScale, float spacingScale) {
        // Legacy page requests are ignored; every configured position stays on one surface.
        public int pages() { return 1; }
        public int page() { return 0; }
        public boolean controls() { return false; }
        public boolean sideControls() { return false; }
    }
    private record Slot(float x, float y, int layer) {}
    private record Design(List<Slot> slots) {}
    private record Fit(float[] x, float[] y, float[] sizes) {}
    private interface Fitter { Fit fit(float scale, float gapScale); }

    public static Frame frame(WidgetConfig c, float width, float height, int ignoredPage) {
        width = Math.max(1, width); height = Math.max(1, height);
        int capacity = Math.max(1, c.capacity());
        float pad = Math.min(c.outerPadding + c.widgetBorder, Math.min(width, height) * .3f);
        if (c.thumb()) return thumbFrame(c, width, height, pad, capacity);
        List<Placement> out = new ArrayList<>();
        float cellW = Math.max(1, width - 2 * pad) / c.columns;
        float cellH = Math.max(1, height - 2 * pad) / c.rows;
        for (int i = 0; i < Math.min(c.icons.size(), capacity); i++) {
            float requested = c.icons.get(i).size > 0 ? c.icons.get(i).size : c.iconSize;
            float size = Math.max(Math.min(2, Math.min(cellW, cellH)), Math.min(requested, Math.min(cellW - 2 * c.paddingX,
                    cellH - 2 * c.paddingY - (c.showLabels() ? 16 : 0))));
            out.add(new Placement(i, pad + (i % c.columns + .5f) * cellW,
                    pad + (i / c.columns + .5f) * cellH, size, 1, 1));
        }
        return new Frame(out, capacity, height, width, 1, 1);
    }
    private static Frame thumbFrame(WidgetConfig c, float width, float height, float pad, int capacity) {
        int count = Math.min(c.icons.size(), capacity);
        if (count == 0) return new Frame(List.of(), capacity, height, width, 1, 1);
        var slots = thumbDesign(c, count).slots;
        float[] u = new float[count], v = new float[count], requested = new float[count];
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (Slot slot : slots) {
            minX = Math.min(minX, slot.x); maxX = Math.max(maxX, slot.x);
            minY = Math.min(minY, slot.y); maxY = Math.max(maxY, slot.y);
        }
        for (int i = 0; i < count; i++) {
            Slot slot = slots.get(i);
            u[i] = maxX - minX < .00001f ? 0 : (maxX - slot.x) / (maxX - minX);
            v[i] = maxY - minY < .00001f ? 0 : (maxY - slot.y) / (maxY - minY);
            var entry = c.icons.get(i);
            float scale = slot.layer < c.layerScales.size() ? c.layerScales.get(slot.layer) / 100f : 1;
            float size = entry.size > 0 ? entry.size : c.iconSize * scale;
            float reach = (slot.layer + 1f) / c.layers.size();
            requested[i] = Math.max(1, Math.min(48, size * (1 - .12f * reach * c.exaggeration / 100f)));
        }
        float edgeInset = Math.min(c.thumbEdgeInset, (width - 2 * pad) * .75f);
        float availableW = width - 2 * pad - edgeInset, availableH = height - 2 * pad;
        Fitter fitter = c.thumbCustomReach
                ? (scale, gap) -> fitReach(c, slots, requested, availableW, availableH, scale, gap)
                : (scale, gap) -> fit(c, u, v, requested, availableW, availableH, scale, gap);
        float iconScale = 1, spacingScale = 1;
        Fit fit = fitter.fit(1, 1);
        if (fit == null) {
            fit = fitter.fit(1, 0);
            float low = 0, high = 1;
            if (fit != null) {
                // Squeeze configurable gaps before changing any artwork size.
                for (int step = 0; step < 22; step++) {
                    float middle = (low + high) / 2;
                    Fit candidate = fitter.fit(1, middle);
                    if (candidate != null) { low = middle; fit = candidate; } else high = middle;
                }
                spacingScale = low;
            } else {
                // One common multiplier preserves layer scales and individual size ratios.
                spacingScale = 0;
                for (int step = 0; step < 25; step++) {
                    float middle = (low + high) / 2;
                    Fit candidate = fitter.fit(middle, 0);
                    if (candidate != null) { low = middle; fit = candidate; } else high = middle;
                }
                iconScale = low;
            }
        }
        List<Placement> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            float x = pad + fit.x[i], y = pad + fit.y[i];
            // A single point has no angular span to rotate; still honor grip height.
            if (count == 1 && !c.thumbCustomReach) y += (availableH - c.tileHeight(fit.sizes[i])) * c.thumbCenterHeight / 100f;
            if (c.layout == WidgetConfig.Layout.LEFT_HAND) x = width - x;
            out.add(new Placement(i, x, y, fit.sizes[i], 1, 1));
        }
        return new Frame(out, capacity, height, width, iconScale, spacingScale);
    }
    private static Fit fitReach(WidgetConfig c, List<Slot> slots, float[] requested,
                                float width, float height, float scale, float gapScale) {
        int count = requested.length;
        float[] sizes = new float[count], widths = new float[count], heights = new float[count];
        float halfW = 0, halfH = 0;
        double inner = Double.POSITIVE_INFINITY, outer = 0;
        for (int i = 0; i < count; i++) {
            sizes[i] = requested[i] * scale;
            widths[i] = c.tileWidth(sizes[i]); heights[i] = c.tileHeight(sizes[i]);
            if (widths[i] > width || heights[i] > height) return null;
            halfW = Math.max(halfW, widths[i] / 2); halfH = Math.max(halfH, heights[i] / 2);
            double radius = Math.hypot(slots.get(i).x, slots.get(i).y);
            inner = Math.min(inner, radius); outer = Math.max(outer, radius);
        }
        float originX = width - halfW;
        float originY = halfH + (height - 2 * halfH) * (100 - c.thumbCenterHeight) / 100f;
        double turn = Math.atan2(c.thumbCenterHeight, 100 - c.thumbCenterHeight);
        double[] cosines = new double[count], sines = new double[count];
        double reach = Double.POSITIVE_INFINITY;
        for (int i = 0; i < count; i++) {
            Slot slot = slots.get(i);
            double angle = Math.atan2(slot.y, slot.x) - turn;
            double cosine = Math.max(0, Math.cos(angle)), sine = Math.sin(angle);
            cosines[i] = cosine; sines[i] = sine;
            if (cosine > 1e-8) reach = Math.min(reach, (width - 2 * halfW) / cosine);
            if (sine > 1e-8) reach = Math.min(reach, (originY - halfH) / sine);
            if (sine < -1e-8) reach = Math.min(reach, (height - halfH - originY) / -sine);
        }
        reach = Math.max(0, reach);
        float[] x = new float[count], y = new float[count];
        for (int i = 0; i < count; i++) {
            Slot slot = slots.get(i);
            // Bounds define the available reach independently of the chosen endpoints.
            // Otherwise fitting would cancel an edited outer radius by stretching it back.
            double unitRadius = Math.hypot(slot.x, slot.y);
            double fraction = unitRadius - inner < 1e-5 ? 0 : outer - unitRadius < 1e-5 ? 1
                    : (unitRadius - inner) / (outer - inner);
            double radius = reach * (c.thumbInnerReach + fraction * (c.thumbOuterReach - c.thumbInnerReach)) / 100.0;
            x[i] = (float) (originX - radius * cosines[i]);
            y[i] = (float) (originY - radius * sines[i]);
        }
        if (overlaps(x, y, widths, heights, 2 * c.paddingX * gapScale, 2 * c.paddingY * gapScale)) return null;
        return new Fit(x, y, sizes);
    }
    private static boolean overlaps(float[] x, float[] y, float[] widths, float[] heights, float gapX, float gapY) {
        for (int i = 0; i < x.length; i++) for (int j = 0; j < i; j++)
            if (Math.abs(x[i] - x[j]) + .00001f < (widths[i] + widths[j]) / 2 + gapX
                    && Math.abs(y[i] - y[j]) + .00001f < (heights[i] + heights[j]) / 2 + gapY) return true;
        return false;
    }
    private static Fit fit(WidgetConfig c, float[] u, float[] v, float[] requested,
                           float width, float height, float scale, float gapScale) {
        int count = requested.length;
        float[] sizes = new float[count], widths = new float[count], heights = new float[count];
        for (int i = 0; i < count; i++) {
            sizes[i] = requested[i] * scale;
            widths[i] = c.tileWidth(sizes[i]); heights[i] = c.tileHeight(sizes[i]);
            if (widths[i] > width || heights[i] > height) return null;
        }
        float[] x = fitAxis(u, widths, width), y = fitAxis(v, heights, height);
        float gapX = 2 * c.paddingX * gapScale, gapY = 2 * c.paddingY * gapScale;
        if (overlaps(x, y, widths, heights, gapX, gapY)) return null;
        return new Fit(x, y, sizes);
    }
    private static float[] fitAxis(float[] centers, float[] sizes, float available) {
        // Fit the occupied bounds, not the unused remainder of a layer template.
        float low = 0, high = available;
        for (int step = 0; step < 25; step++) {
            float stretch = (low + high) / 2, min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
            for (int i = 0; i < centers.length; i++) {
                min = Math.min(min, centers[i] * stretch - sizes[i] / 2);
                max = Math.max(max, centers[i] * stretch + sizes[i] / 2);
            }
            if (max - min <= available) low = stretch; else high = stretch;
        }
        float edge = Float.MAX_VALUE;
        for (int i = 0; i < centers.length; i++) edge = Math.min(edge, centers[i] * low - sizes[i] / 2);
        float[] out = new float[centers.length];
        for (int i = 0; i < centers.length; i++) out[i] = centers[i] * low - edge;
        return out;
    }
    private static Design thumbDesign(WidgetConfig c, int visibleCount) {
        List<Slot> slots = new ArrayList<>();
        // Unit geometry establishes symmetric rays; pixel/dp sizing is fitted separately.
        double sepX = 1, sepY = 1;
        double previousRadius = 0;
        for (int layer = 0; layer < c.layers.size() && slots.size() < visibleCount; layer++) {
            int count = Math.min(c.layers.get(layer), visibleCount - slots.size());
            double[] xs = new double[count], ys = new double[count];
            for (int j = 0; j < count; j++) {
                // Odd layers share 0/45/90 degrees. 2/5/9 share the 22.5 degree rays.
                double angle = Math.toRadians(count == 1 ? 45 : count == 2 ? 22.5 + j * 45 : 90.0 * j / (count - 1));
                xs[j] = Math.cos(angle); ys[j] = Math.sin(angle);
            }
            double radius = previousRadius + 1;
            for (int j = 0; j < count; j++) for (int k = 0; k < j; k++)
                radius = Math.max(radius, Math.min(sepX / Math.max(1e-9, Math.abs(xs[j] - xs[k])),
                        sepY / Math.max(1e-9, Math.abs(ys[j] - ys[k]))));
            // Move the complete ring past every collision interval. Counts and angles stay fixed.
            boolean moved;
            do {
                moved = false;
                for (int j = 0; j < count; j++) for (Slot old : slots) {
                    double lowX = xs[j] < 1e-9 ? (Math.abs(old.x) < sepX ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY) : (old.x - sepX) / xs[j];
                    double highX = xs[j] < 1e-9 ? Double.POSITIVE_INFINITY : (old.x + sepX) / xs[j];
                    double lowY = ys[j] < 1e-9 ? (Math.abs(old.y) < sepY ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY) : (old.y - sepY) / ys[j];
                    double highY = ys[j] < 1e-9 ? Double.POSITIVE_INFINITY : (old.y + sepY) / ys[j];
                    double low = Math.max(lowX, lowY), high = Math.min(highX, highY);
                    if (radius > low + 1e-5 && radius < high - 1e-5) { radius = high + .01; moved = true; }
                }
            } while (moved);
            for (int j = 0; j < count; j++) {
                float x = (float) (radius * xs[j]), y = (float) (radius * ys[j]);
                slots.add(new Slot(x, y, layer));
            }
            previousRadius = radius;
        }
        if (c.thumbCustomReach || (c.thumbCenterHeight == 0 && c.thumbLayerSpacing == 100)) return new Design(slots);
        // Rotate the fan around the selected grip height. Use atan2 so the
        // outer endpoints divide the vertical span in the requested proportion.
        double turn = Math.atan2(c.thumbCenterHeight, 100 - c.thumbCenterHeight);
        double cosine = Math.cos(turn), sine = Math.sin(turn);
        double innerRadius = Math.hypot(slots.get(0).x, slots.get(0).y);
        List<Slot> adjusted = new ArrayList<>();
        for (Slot slot : slots) {
            double radius = Math.hypot(slot.x, slot.y);
            double scale = (innerRadius + (radius - innerRadius) * c.thumbLayerSpacing / 100.0) / radius;
            adjusted.add(new Slot((float) ((slot.x * cosine + slot.y * sine) * scale),
                    (float) ((slot.y * cosine - slot.x * sine) * scale), slot.layer));
        }
        return new Design(adjusted);
    }
    public static int hitTest(WidgetConfig c, List<Placement> items, float x, float y) {
        for (int i = items.size() - 1; i >= 0; i--) {
            Placement p = items.get(i);
            if (Math.abs(x - p.x) <= c.tileWidth(p.size) / 2 && Math.abs(y - p.y) <= c.tileHeight(p.size) / 2)
                return p.index;
        }
        return -1;
    }
}
