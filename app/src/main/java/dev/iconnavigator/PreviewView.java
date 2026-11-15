package dev.iconnavigator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

/** Actual widget geometry remains visible while apps are edited or appearance changes. */
public final class PreviewView extends View {
    public interface Listener { void changed(); void edit(int index); }
    public final WidgetConfig config;
    public boolean editing;
    public float page;
    public Listener listener;
    private final IconRenderer renderer;
    private final float density;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float downX, downY, lastX, lastY;
    private boolean moved, dragging;
    private int pressed = -1, drop = -1;
    private final Runnable longPress = () -> {
        if (editing && pressed >= 0 && !moved) {
            dragging = true; drop = pressed;
            performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS); invalidate();
        }
    };
    public PreviewView(Context context, WidgetConfig config) {
        super(context); this.config = config; renderer = new IconRenderer(context);
        density = getResources().getDisplayMetrics().density;
        setContentDescription("Widget preview"); setFocusable(true);
    }
    public void refresh() { invalidate(); }
    public LayoutEngine.Frame frame() { return LayoutEngine.frame(config, getWidth() / density, getHeight() / density, Math.round(page)); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas); canvas.save(); canvas.scale(density, density);
        float w = getWidth() / density, h = getHeight() / density;
        for (int y = 0; y < h; y += 16) for (int x = 0; x < w; x += 16) {
            paint.setColor(((x / 16 + y / 16) % 2 == 0) ? 0xff20251f : 0xff242a23);
            canvas.drawRect(x, y, x + 16, y + 16, paint);
        }
        BackdropRenderer.draw(canvas, config, renderer, w, h);
        var frame = frame(); page = frame.page();
        for (var p : frame.placements()) {
            if (dragging && p.index() == pressed) continue;
            renderer.draw(canvas, config, p);
            if (dragging && p.index() == drop) {
                paint.setColor(0xffb9f97b); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2);
                canvas.drawRect(p.x() - config.tileWidth(p.size()) / 2, p.y() - config.tileHeight(p.size()) / 2,
                        p.x() + config.tileWidth(p.size()) / 2, p.y() + config.tileHeight(p.size()) / 2, paint);
                paint.setStyle(Paint.Style.FILL);
            }
        }
        if (dragging && pressed >= 0) {
            float size = frame.placements().stream().filter(p -> p.index() == pressed).map(LayoutEngine.Placement::size).findFirst().orElse(40f);
            renderer.draw(canvas, config, new LayoutEngine.Placement(pressed, lastX, lastY, size, 1, .8f));
        }
        paint.setColor(config.foregroundColor()); paint.setTextSize(12); paint.setTextAlign(Paint.Align.CENTER);
        if (config.icons.isEmpty()) canvas.drawText("Add apps to see your widget", w / 2, h / 2, paint);
        canvas.restore();
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX() / density, y = event.getY() / density;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = lastX = x; downY = lastY = y; moved = dragging = false;
                pressed = LayoutEngine.hitTest(config, frame().placements(), x, y);
                postDelayed(longPress, ViewConfiguration.getLongPressTimeout());
                getParent().requestDisallowInterceptTouchEvent(true); return true;
            case MotionEvent.ACTION_MOVE:
                if (Math.hypot(x - downX, y - downY) > 8) { moved = true; removeCallbacks(longPress); }
                if (dragging) {
                    float distance = Float.MAX_VALUE;
                    for (var p : frame().placements()) {
                        float d = (float) Math.hypot(x - p.x(), y - p.y());
                        if (d < distance) { drop = p.index(); distance = d; }
                    }
                }
                lastX = x; lastY = y; invalidate(); return true;
            case MotionEvent.ACTION_UP:
                removeCallbacks(longPress);
                var frame = frame(); float w = getWidth() / density;
                if (dragging) { config.move(pressed, drop); if (listener != null) listener.changed(); }
                else if (!moved && editing && pressed >= 0 && listener != null) { performClick(); listener.edit(pressed); }
                dragging = false; pressed = -1; invalidate(); getParent().requestDisallowInterceptTouchEvent(false); return true;
            case MotionEvent.ACTION_CANCEL:
                removeCallbacks(longPress); dragging = false; pressed = -1; invalidate();
                getParent().requestDisallowInterceptTouchEvent(false); return true;
            default: return true;
        }
    }
    @Override public boolean performClick() { super.performClick(); return true; }
    @Override protected void onDetachedFromWindow() { removeCallbacks(longPress); super.onDetachedFromWindow(); }
}
