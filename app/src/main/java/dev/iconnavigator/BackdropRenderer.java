package dev.iconnavigator;

import android.graphics.*;

/** Decorative artwork has no click targets. The icon layer remains independently tappable. */
public final class BackdropRenderer {
    private BackdropRenderer() {}
    public static void draw(Canvas canvas, WidgetConfig c, IconRenderer icons, float width, float height) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        float radius = Math.min(c.cornerRadius, Math.min(width, height) / 2);
        RectF bounds = new RectF(0, 0, width, height);
        Path clip = new Path(); clip.addRoundRect(bounds, radius, radius, Path.Direction.CW);
        canvas.save(); canvas.clipPath(clip);
        canvas.drawColor(c.backgroundArgb());
        if (c.backgroundArt == WidgetConfig.BackgroundArt.GRADIENT) {
            double angle = Math.toRadians(c.gradientAngle);
            float dx = (float) Math.cos(angle) * width / 2, dy = (float) Math.sin(angle) * height / 2;
            int alpha = Math.round(c.artOpacity * 2.55f) << 24;
            paint.setShader(new LinearGradient(width / 2 - dx, height / 2 - dy, width / 2 + dx, height / 2 + dy,
                    alpha | (c.background & 0xffffff), alpha | (c.gradientEnd & 0xffffff), Shader.TileMode.CLAMP));
            canvas.drawRect(bounds, paint); paint.setShader(null);
        } else if (c.backgroundArt == WidgetConfig.BackgroundArt.IMAGE) {
            Bitmap image = icons.image(c.backgroundImage);
            if (image != null) {
                float scale = Math.max(width / image.getWidth(), height / image.getHeight());
                float w = image.getWidth() * scale, h = image.getHeight() * scale;
                paint.setAlpha(Math.round(c.artOpacity * 2.55f));
                canvas.drawBitmap(image, null, new RectF((width - w) / 2, (height - h) / 2, (width + w) / 2, (height + h) / 2), paint);
                paint.setAlpha(255);
            }
        }
        int count = Math.min(12, c.icons.size());
        if (c.backgroundArt == WidgetConfig.BackgroundArt.ICON_WASH) {
            for (int i = 0; i < Math.min(6, count); i++) {
                int color = colorOf(icons.icon(c.icons.get(i)), c);
                int alpha = Math.round(c.artOpacity * 2.55f);
                float x = width * ((i % 3 + .5f) / 3), y = height * ((i / 3 + .5f) / 2);
                paint.setShader(new RadialGradient(x, y, Math.max(width, height) * .7f,
                        (alpha << 24) | (color & 0xffffff), color & 0xffffff, Shader.TileMode.CLAMP));
                canvas.drawRect(bounds, paint);
            }
        } else if (c.backgroundArt == WidgetConfig.BackgroundArt.ICON_COLLAGE && count > 0) {
            paint.setColorFilter(IconRenderer.filter(c)); paint.setAlpha(Math.round(c.artOpacity * 2.55f));
            float cell = Math.max(32, Math.min(width, height) / 3);
            int index = 0;
            for (float y = -cell / 2; y < height; y += cell)
                for (float x = -cell / 2; x < width; x += cell) {
                    Bitmap bitmap = icons.icon(c.icons.get(index++ % count));
                    canvas.save(); canvas.rotate(-18, x + cell / 2, y + cell / 2);
                    float fit = (cell - 12) / Math.max(bitmap.getWidth(), bitmap.getHeight());
                    float halfW = bitmap.getWidth() * fit / 2, halfH = bitmap.getHeight() * fit / 2;
                    canvas.drawBitmap(bitmap, null, new RectF(x + cell / 2 - halfW, y + cell / 2 - halfH,
                            x + cell / 2 + halfW, y + cell / 2 + halfH), paint);
                    canvas.restore();
                }
        }
        canvas.restore();
        if (c.widgetBorder > 0) {
            paint.setShader(null); paint.setColorFilter(null); paint.setColor(c.widgetBorderColor);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(c.widgetBorder);
            float inset = c.widgetBorder / 2;
            bounds.inset(inset, inset); canvas.drawRoundRect(bounds, radius, radius, paint);
        }
    }
    private static int colorOf(Bitmap bitmap, WidgetConfig c) {
        if (c.effect == WidgetConfig.Effect.TINT) return c.tint;
        long r = 0, g = 0, b = 0, weight = 0;
        for (int y = 0; y < bitmap.getHeight(); y += 8) for (int x = 0; x < bitmap.getWidth(); x += 8) {
            int color = bitmap.getPixel(x, y), a = Color.alpha(color);
            r += Color.red(color) * a; g += Color.green(color) * a; b += Color.blue(color) * a; weight += a;
        }
        if (weight == 0) return 0xff808080;
        int red = (int) (r / weight), green = (int) (g / weight), blue = (int) (b / weight);
        if (c.effect == WidgetConfig.Effect.GRAYSCALE) red = green = blue = Math.round(.2126f * red + .7152f * green + .0722f * blue);
        return Color.rgb(red, green, blue);
    }
}
