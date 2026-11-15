package dev.iconnavigator;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.util.LruCache;
import android.widget.Toast;
import java.io.File;

public final class IconRenderer {
    private final Context context;
    private final LruCache<String, Bitmap> cache = new LruCache<>(12 * 1024 * 1024) {
        @Override protected int sizeOf(String key, Bitmap value) { return value.getAllocationByteCount(); }
    };
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF destination = new RectF();
    private WidgetConfig.Effect lastEffect;
    private int lastTint;
    private ColorFilter cachedFilter;
    public IconRenderer(Context context) { this.context = context.getApplicationContext(); }
    public void clear() { cache.evictAll(); }
    public Bitmap icon(WidgetConfig.IconEntry entry) {
        return icon(entry, Math.round(48 * context.getResources().getDisplayMetrics().density));
    }
    public Bitmap image(String file) {
        if (!file.matches("[a-zA-Z0-9-]+\\.png")) return null;
        Bitmap bitmap = cache.get(file);
        if (bitmap == null) { bitmap = BitmapFactory.decodeFile(new File(context.getFilesDir(), file).getPath()); if (bitmap != null) cache.put(file, bitmap); }
        return bitmap;
    }
    public Bitmap icon(WidgetConfig.IconEntry entry, int requestedPixels) {
        int pixels = 64;
        while (pixels < requestedPixels && pixels < 512) pixels *= 2;
        String key = entry.component + ":" + entry.image + ":" + pixels;
        Bitmap bitmap = cache.get(key);
        if (bitmap != null) return bitmap;
        if (!entry.image.isEmpty() && entry.image.matches("[a-zA-Z0-9-]+\\.png")) {
            bitmap = image(entry.image);
        }
        if (bitmap == null) {
            Drawable icon;
            try { icon = context.getPackageManager().getActivityIcon(ComponentName.unflattenFromString(entry.component)); }
            catch (Exception e) { icon = context.getPackageManager().getDefaultActivityIcon(); }
            bitmap = rasterize(icon, pixels);
        }
        cache.put(key, bitmap);
        return bitmap;
    }
    static Bitmap rasterize(Drawable drawable, int pixels) {
        int sourceW = Math.max(1, drawable.getIntrinsicWidth()), sourceH = Math.max(1, drawable.getIntrinsicHeight());
        float scale = pixels / (float) Math.max(sourceW, sourceH);
        Bitmap bitmap = Bitmap.createBitmap(Math.max(1, Math.round(sourceW * scale)), Math.max(1, Math.round(sourceH * scale)), Bitmap.Config.ARGB_8888);
        drawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight()); drawable.draw(new Canvas(bitmap));
        return bitmap;
    }
    public static ColorFilter filter(WidgetConfig c) {
        if (c.effect == WidgetConfig.Effect.GRAYSCALE) {
            ColorMatrix m = new ColorMatrix(); m.setSaturation(0); return new ColorMatrixColorFilter(m);
        }
        if (c.effect == WidgetConfig.Effect.TINT) return new PorterDuffColorFilter(c.tint, PorterDuff.Mode.SRC_IN);
        return null;
    }
    public void draw(Canvas canvas, WidgetConfig c, LayoutEngine.Placement p) {
        if (lastEffect != c.effect || lastTint != c.tint) {
            lastEffect = c.effect; lastTint = c.tint; cachedFilter = filter(c);
        }
        paint.clearShadowLayer(); paint.setColorFilter(cachedFilter); paint.setAlpha(Math.round(255 * p.alpha() * c.iconOpacity / 100f));
        float s = p.size(), centerY = p.y() - (c.labelsAt(s) ? 8 : 0);
        float[] matrix = new float[9]; canvas.getMatrix().getValues(matrix);
        float scale = (float) Math.hypot(matrix[0], matrix[3]);
        Bitmap source = icon(c.icons.get(p.index()), (int) Math.ceil(s * scale));
        float fit = Math.max(.01f, s - 2 * Math.min(c.iconBorder, s / 3)) / Math.max(source.getWidth(), source.getHeight());
        float halfW = source.getWidth() * fit / 2, halfH = source.getHeight() * fit / 2;
        destination.set(p.x() - halfW, centerY - halfH, p.x() + halfW, centerY + halfH);
        canvas.drawBitmap(source, null, destination, paint);
        if (c.iconBorder > 0) {
            paint.setColorFilter(null); paint.setColor(c.iconBorderColor); paint.setAlpha(Math.round(255 * p.alpha() * c.iconOpacity / 100f));
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(Math.min(c.iconBorder, s / 3));
            float inset = paint.getStrokeWidth() / 2;
            destination.set(p.x() - s / 2 + inset, centerY - s / 2 + inset, p.x() + s / 2 - inset, centerY + s / 2 - inset);
            canvas.drawRoundRect(destination, Math.min(8, s / 4), Math.min(8, s / 4), paint);
            paint.setStyle(Paint.Style.FILL);
        }
        if (c.labelsAt(s)) {
            paint.setColorFilter(null); paint.setColor(c.foregroundColor());
            paint.setAlpha(Math.round(255 * p.alpha())); paint.setTextSize(10); paint.setTextAlign(Paint.Align.CENTER);
            if (c.labelShadow) paint.setShadowLayer(2, 0, 1, Color.BLACK);
            String label = c.icons.get(p.index()).label;
            float max = Math.max(s, 34);
            if (paint.measureText(label) > max) {
                int length = paint.breakText(label, true, Math.max(1, max - paint.measureText("…")), null);
                label = label.substring(0, length) + "…";
            }
            canvas.drawText(label, p.x(), centerY + s / 2 + 12, paint);
        }
    }
    /** Bitmap tiles are capped to protect RemoteViews' shared-memory budget. */
    public Bitmap tile(WidgetConfig c, LayoutEngine.Placement p, float scale) {
        float width = c.tileWidth(p.size()), height = c.tileHeight(p.size());
        Bitmap b = Bitmap.createBitmap(Math.max(1, (int) Math.ceil(width * scale)),
                Math.max(1, (int) Math.ceil(height * scale)), Bitmap.Config.ARGB_8888);
        b.setDensity(Bitmap.DENSITY_NONE);
        Canvas canvas = new Canvas(b); canvas.scale(scale, scale);
        draw(canvas, c, new LayoutEngine.Placement(p.index(), width / 2, height / 2, p.size(), p.depth(), p.alpha()));
        return b;
    }
    public static void launch(Context context, WidgetConfig.IconEntry entry) {
        try {
            ComponentName component = ComponentName.unflattenFromString(entry.component);
            if (component == null) throw new IllegalArgumentException();
            Intent launch = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            context.startActivity(launch);
        } catch (Exception e) { Toast.makeText(context, "This app is unavailable. Replace it in the editor.", Toast.LENGTH_LONG).show(); }
    }
}
