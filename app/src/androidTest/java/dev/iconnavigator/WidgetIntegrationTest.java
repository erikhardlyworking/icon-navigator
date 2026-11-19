package dev.iconnavigator;

import android.appwidget.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class WidgetIntegrationTest {
    @Test public void releasePrivacyDeclarationsMatchThePackagedApp() throws Exception {
        var info=context().getPackageManager().getPackageInfo(context().getPackageName(),android.content.pm.PackageManager.GET_PERMISSIONS);
        var permissions=info.requestedPermissions==null?java.util.List.<String>of():java.util.Arrays.asList(info.requestedPermissions);
        for(String permission:new String[]{"android.permission.INTERNET","android.permission.QUERY_ALL_PACKAGES","android.permission.READ_CONTACTS","android.permission.ACCESS_FINE_LOCATION"})
            assertFalse(permissions.contains(permission));
        var privacy=context().getPackageManager().getActivityInfo(new ComponentName(context(),PrivacyActivity.class),0);
        assertFalse(privacy.exported);
        assertEquals(0,info.applicationInfo.flags&android.content.pm.ApplicationInfo.FLAG_ALLOW_BACKUP);
    }
    private android.app.Instrumentation instrumentation() { return InstrumentationRegistry.getInstrumentation(); }
    private Context context() { return instrumentation().getTargetContext(); }
    private void onMain(Runnable action) throws Throwable {
        var failure = new java.util.concurrent.atomic.AtomicReference<Throwable>();
        instrumentation().runOnMainSync(() -> { try { action.run(); } catch (Throwable e) { failure.set(e); } });
        if (failure.get() != null) throw failure.get();
    }
    private WidgetConfig fixture(WidgetConfig.Type type, int count) {
        WidgetConfig c = new WidgetConfig(); c.selectType(type, true);
        c.iconSize=40;c.thumbCustomReach=false;c.thumbCenterHeight=0;c.thumbLayerSpacing=100;
        java.util.Collections.fill(c.layerScales,100);
        for (int i = 0; i < count; i++) c.addIcon(new WidgetConfig.IconEntry("id-" + i, "test.app" + i + "/.Main", "App " + i));
        return c;
    }
    @Test public void allProvidersBindAndRenderUsingTheirFixedType() throws Throwable {
        var manager = AppWidgetManager.getInstance(context()); var host = new AppWidgetHost(context(), 84001);
        ConfigStore store = new ConfigStore(context());
        for (var type : WidgetConfig.Type.values()) {
            int id = host.allocateAppWidgetId();
            try {
                instrumentation().getUiAutomation().adoptShellPermissionIdentity("android.permission.BIND_APPWIDGET");
                assertTrue(manager.bindAppWidgetIdIfAllowed(id, WidgetProviders.component(context(), type)));
                instrumentation().getUiAutomation().dropShellPermissionIdentity();
                assertTrue(NavigatorWidget.owns(context(), id)); assertEquals(type, WidgetProviders.type(context(), id));
                var provider = manager.getAppWidgetInfo(id);
                assertNotNull(provider.configure);
                assertEquals(new ComponentName(context(), EditorActivity.class), provider.configure);
                assertTrue((provider.widgetFeatures & AppWidgetProviderInfo.WIDGET_FEATURE_RECONFIGURABLE) != 0);
                store.save(id, fixture(WidgetConfig.Type.GRID_COMPACT, 18));
                assertEquals(type, WidgetProviders.load(context(), id).type);
                Bundle options = new Bundle();
                options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, type.width);
                options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, type.height);
                ArrayList<android.util.SizeF> sizes = new ArrayList<>();
                for (int i = 0; i < 4; i++) sizes.add(new android.util.SizeF(type.width + i * 20, type.height + i * 20));
                options.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, sizes);
                manager.updateAppWidgetOptions(id, options); NavigatorWidget.update(context(), id);
                assertEquals(type.title, manager.getAppWidgetInfo(id).loadLabel(context().getPackageManager()));
            } finally { instrumentation().getUiAutomation().dropShellPermissionIdentity(); host.deleteAppWidgetId(id); store.delete(id); }
        }
    }
    @Test public void allStylesAndOverlaysKeepEveryForegroundTargetClickable() throws Throwable {
        onMain(() -> {
            for (var type : WidgetConfig.Type.values()) for (var overlay : WidgetConfig.BackgroundArt.values()) {
                var c = fixture(type, 21); c.backgroundArt = overlay; c.useDefaultLabels = false; c.labels = true;
                Set<String> reached = new HashSet<>();
                float width = type.width, height = type.height;
                var frame = LayoutEngine.frame(c, width, height, 0);
                for (int page = 0; page < frame.pages(); page++) {
                    View view = NavigatorWidget.render(context(), 123, c, page, width, height).apply(context(), new FrameLayout(context()));
                    assertFalse(view.findViewById(R.id.backdrop).hasOnClickListeners());
                    FrameLayout icons = view.findViewById(R.id.widget_icons);
                    for (int i = 0; i < icons.getChildCount(); i++) {
                        var icon = icons.getChildAt(i); assertTrue(icon.hasOnClickListeners()); assertTrue(reached.add(icon.getContentDescription().toString()));
                        if (c.thumb()) {
                            float density = context().getResources().getDisplayMetrics().density;
                            var placement = frame.placements().get(i);
                            assertEquals(c.tileWidth(placement.size()) * density, icon.getLayoutParams().width, 1);
                            assertEquals(c.tileHeight(placement.size()) * density, icon.getLayoutParams().height, 1);
                        }
                    }
                }
                assertEquals(Math.min(21, c.capacity()), reached.size());
            }
        });
    }
    @Test public void plainGridUsesDeviceDensityInsteadOfLowResolutionTiles() throws Throwable {
        onMain(() -> {
            var c = fixture(WidgetConfig.Type.GRID_COMPACT, 12);
            View view = NavigatorWidget.render(context(), 123, c, 0, 220, 280).apply(context(), new FrameLayout(context()));
            assertEquals(3, ((android.view.ViewGroup) view).getChildCount());
            FrameLayout icons = view.findViewById(R.id.widget_icons); assertEquals(12, icons.getChildCount());
            Bitmap bitmap = ((BitmapDrawable) ((ImageView) icons.getChildAt(0)).getDrawable()).getBitmap();
            assertTrue(bitmap.getWidth() >= Math.ceil(40 * context().getResources().getDisplayMetrics().density));
            assertFalse(bitmap.isMutable());
            c.icons.clear(); NavigatorWidget.render(context(), 123, c, 0, 220, 280).reapply(context(), view);
            assertEquals(0, icons.getChildCount()); assertEquals(View.VISIBLE, view.findViewById(R.id.empty).getVisibility());
        });
    }
    @Test public void blankWidgetAreasNeverOpenSettingsAfterFreshInflationOrReapply() throws Throwable {
        onMain(() -> {
            int[] backgroundTargets = {R.id.widget_root, R.id.backdrop, R.id.widget_icons, R.id.empty};
            var edit = android.app.PendingIntent.getActivity(context(), 84006,
                    new Intent(context(), EditorActivity.class), android.app.PendingIntent.FLAG_IMMUTABLE);
            for (var type : WidgetConfig.Type.values())
            for (int[] size : new int[][]{{80,120},{400,64},{64,400},{440,500}})
            for (int count : new int[]{0,1,23,400}) {
                var config = fixture(type, count);
                var remote = NavigatorWidget.render(context(), 123, config, 0, size[0], size[1]);
                var view = remote.apply(context(), new FrameLayout(context()));
                for (int target : backgroundTargets) {
                    assertFalse(view.findViewById(target).hasOnClickListeners());
                    assertTrue(view.findViewById(target).isClickable());
                    assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO, view.findViewById(target).getImportantForAccessibility());
                    assertFalse(view.findViewById(target).performClick());
                }
                // Reproduce a launcher retaining the old configuration click actions.
                var legacy = new RemoteViews(context().getPackageName(), remote.getLayoutId());
                for (int target : backgroundTargets) legacy.setOnClickPendingIntent(target, edit);
                legacy.reapply(context(), view);
                for (int target : backgroundTargets) assertTrue(view.findViewById(target).hasOnClickListeners());
                remote.reapply(context(), view);
                for (int target : backgroundTargets) {
                    assertFalse(view.findViewById(target).hasOnClickListeners());
                    assertTrue(view.findViewById(target).isClickable());
                    assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO, view.findViewById(target).getImportantForAccessibility());
                    assertFalse(view.findViewById(target).performClick());
                }
                FrameLayout icons = view.findViewById(R.id.widget_icons);
                assertEquals(Math.min(count, config.capacity()), icons.getChildCount());
                for (int i = 0; i < icons.getChildCount(); i++) {
                    assertTrue(icons.getChildAt(i).hasOnClickListeners());
                    assertTrue(icons.getChildAt(i).isImportantForAccessibility());
                }
                if (count == 0) assertEquals("Long-press for settings", ((TextView) view.findViewById(R.id.empty)).getText());
                if (count <= 1) {
                    // Bottom-right is empty for these sparse fixtures. It must
                    // consume the gesture without bubbling into host configuration.
                    float density = context().getResources().getDisplayMetrics().density;
                    int width = Math.round(size[0] * density), height = Math.round(size[1] * density);
                    view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
                    view.layout(0, 0, width, height);
                    long time = android.os.SystemClock.uptimeMillis();
                    var down = android.view.MotionEvent.obtain(time, time, android.view.MotionEvent.ACTION_DOWN, width-1, height-1, 0);
                    var up = android.view.MotionEvent.obtain(time, time+20, android.view.MotionEvent.ACTION_UP, width-1, height-1, 0);
                    try { assertTrue(view.dispatchTouchEvent(down)); assertTrue(view.dispatchTouchEvent(up)); }
                    finally { down.recycle(); up.recycle(); }
                }
            }
            edit.cancel();
        });
    }
    @Test public void resizedWidgetsKeepEveryTargetInsideTheirActualBounds() throws Throwable {
        // Both offered families and every retained provider, across shapes and overflow counts.
        onMain(() -> {
            float density = context().getResources().getDisplayMetrics().density;
            for (var type : WidgetConfig.Type.values())
            for (int[] size : new int[][]{{120,160},{440,500},{400,64},{64,400}})
            for (int count : new int[]{0,1,8,13,35}) {
                var c = fixture(type, count);
                int width = size[0], height = size[1];
                var first = LayoutEngine.frame(c, width, height, 0); int reached = 0;
                for (int page = 0; page < first.pages(); page++) {
                    var frame = LayoutEngine.frame(c, width, height, page);
                    View view = NavigatorWidget.render(context(), 123, c, page, width, height).apply(context(), new FrameLayout(context()));
                    view.measure(View.MeasureSpec.makeMeasureSpec(Math.round(width * density), View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(Math.round(height * density), View.MeasureSpec.EXACTLY));
                    view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
                    FrameLayout icons = view.findViewById(R.id.widget_icons);
                    assertEquals(0, icons.getPaddingLeft()); assertEquals(0, icons.getPaddingTop());
                    assertEquals(frame.placements().size(), icons.getChildCount());
                    for (int i = 0; i < icons.getChildCount(); i++) {
                        View icon = icons.getChildAt(i); assertTrue(icon.hasOnClickListeners());
                        assertTrue(icon.getWidth() > 0 && icon.getHeight() > 0);
                        assertTrue(icon.getLeft() >= -1 && icon.getTop() >= -1);
                        assertTrue(icon.getRight() <= frame.contentWidth() * density + 2);
                        assertTrue(icon.getBottom() <= frame.contentHeight() * density + 2);
                        assertEquals("App " + reached++, icon.getContentDescription());
                    }
                    assertEquals(3, ((android.view.ViewGroup) view).getChildCount());
                }
                assertEquals(Math.min(count, c.capacity()), reached);
            }
        });
    }
    @Test public void resizeRerasterizesArtworkAtNativeDensityWithoutChangingAspect() throws Throwable {
        Bitmap source = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888); source.eraseColor(Color.GREEN);
        Bitmap raster = IconRenderer.rasterize(new BitmapDrawable(context().getResources(), source), 256);
        assertEquals(256, raster.getWidth()); assertEquals(128, raster.getHeight());
        java.io.File file = new java.io.File(context().getFilesDir(), "resize-aspect-test.png");
        try {
            try (var out = new java.io.FileOutputStream(file)) { source.compress(Bitmap.CompressFormat.PNG, 100, out); }
            onMain(() -> {
                float density = context().getResources().getDisplayMetrics().density;
                for (var type : WidgetConfig.Type.values()) {
                    var c = fixture(type, 1); c.icons.get(0).image = file.getName(); c.icons.get(0).size = 96;
                    int previousWidth = 0;
                    for (int multiplier : new int[]{1,2}) {
                        View view = NavigatorWidget.render(context(), 123, c, 0, type.width * multiplier, type.height * multiplier)
                                .apply(context(), new FrameLayout(context()));
                        ImageView icon = (ImageView) ((FrameLayout) view.findViewById(R.id.widget_icons)).getChildAt(0);
                        Bitmap bitmap = ((BitmapDrawable) icon.getDrawable()).getBitmap();
                        var placement = LayoutEngine.frame(c, type.width * multiplier, type.height * multiplier, 0).placements().get(0);
                        assertTrue(bitmap.getWidth() >= Math.floor(c.tileWidth(placement.size()) * density));
                        assertTrue(bitmap.getWidth() >= previousWidth); previousWidth = bitmap.getWidth();
                        assertEquals(ImageView.ScaleType.FIT_CENTER, icon.getScaleType());
                        int left = bitmap.getWidth(), right = -1, top = bitmap.getHeight(), bottom = -1;
                        for (int y = 0; y < bitmap.getHeight(); y++) for (int x = 0; x < bitmap.getWidth(); x++)
                            if (Color.alpha(bitmap.getPixel(x, y)) > 128) {
                                left = Math.min(left,x); right = Math.max(right,x); top = Math.min(top,y); bottom = Math.max(bottom,y);
                            }
                        assertEquals(2, (right - left + 1f) / (bottom - top + 1f), .04);
                    }
                }
            });
        } finally { file.delete(); }
    }
    @Test public void resizeCallbackRefreshesEveryBoundProvider() throws Throwable {
        var manager = AppWidgetManager.getInstance(context()); var host = new AppWidgetHost(context(), 84003);
        var store = new ConfigStore(context());
        for (var type : WidgetConfig.Type.values()) {
            int id = host.allocateAppWidgetId();
            try {
                instrumentation().getUiAutomation().adoptShellPermissionIdentity("android.permission.BIND_APPWIDGET");
                assertTrue(manager.bindAppWidgetIdIfAllowed(id, WidgetProviders.component(context(), type)));
                instrumentation().getUiAutomation().dropShellPermissionIdentity();
                var c = fixture(type, 13); store.save(id, c);
                for (int[] size : new int[][]{{120,160},{460,500},{400,160}}) {
                    Bundle options = new Bundle();
                    options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, size[0]);
                    options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, size[1]);
                    manager.updateAppWidgetOptions(id, options);
                    new NavigatorWidget().onAppWidgetOptionsChanged(context(), manager, id, options);
                    onMain(() -> {
                        var view = host.createView(context(), id, manager.getAppWidgetInfo(id));
                        FrameLayout icons = view.findViewById(R.id.widget_icons); assertNotNull(icons);
                        var expected = LayoutEngine.frame(c, size[0], size[1], 0);
                        assertEquals(expected.placements().size(), icons.getChildCount());
                        var params = (FrameLayout.LayoutParams) icons.getChildAt(0).getLayoutParams();
                        var p = expected.placements().get(0); float density = context().getResources().getDisplayMetrics().density;
                        assertEquals((p.x() - c.tileWidth(p.size()) / 2) * density, params.leftMargin, 1);
                    });
                }
            } finally { instrumentation().getUiAutomation().dropShellPermissionIdentity(); host.deleteAppWidgetId(id); store.delete(id); }
        }
    }
    @Test public void denseResponsiveSnapshotsStayWithinMemoryAndBinderBudgets() throws Throwable {
        onMain(() -> {
            var c = fixture(WidgetConfig.Type.GRID_COMPACT, 400); c.rows = c.columns = 12; c.useDefaultLabels = false; c.labels = true;
            var variants = new LinkedHashMap<android.util.SizeF, RemoteViews>(); long bitmapBytes = 0;
            for (int i = 0; i < 4; i++) {
                int width = 200 + i * 60, height = 300 + i * 60;
                var remote = NavigatorWidget.render(context(), 123, c, 0, width, height);
                variants.put(new android.util.SizeF(width, height), remote);
                View view = remote.apply(context(), new FrameLayout(context()));
                bitmapBytes += ((BitmapDrawable) ((ImageView) view.findViewById(R.id.backdrop)).getDrawable()).getBitmap().getAllocationByteCount();
                FrameLayout icons = view.findViewById(R.id.widget_icons);
                for (int j = 0; j < icons.getChildCount(); j++) bitmapBytes += ((BitmapDrawable) ((ImageView) icons.getChildAt(j)).getDrawable()).getBitmap().getAllocationByteCount();
            }
            var metrics = context().getResources().getDisplayMetrics();
            assertTrue(bitmapBytes < metrics.widthPixels * (long) metrics.heightPixels * 4);
            var parcel = android.os.Parcel.obtain();
            try { new RemoteViews(variants).writeToParcel(parcel, 0); assertTrue(parcel.dataSize() < 900000); }
            finally { parcel.recycle(); }
        });
    }
    @Test public void paginationIsAbsentEvenForLegacyOverflowAndPageRequests() throws Throwable {
        onMain(() -> {
            for (var type : WidgetConfig.Type.values()) for (int count : new int[]{0,1,23,400}) {
                var c = fixture(type, count);
                var view = NavigatorWidget.render(context(), 123, c, 99, 160, 240)
                        .apply(context(), new FrameLayout(context()));
                assertEquals(3, ((android.view.ViewGroup) view).getChildCount());
                assertEquals(Math.min(count, c.capacity()), ((FrameLayout) view.findViewById(R.id.widget_icons)).getChildCount());
                assertEquals(count, c.icons.size());
            }
        });
    }
    @Test public void thumbHandednessSurvivesProviderEnforcementAndOnlyTwoEntriesAreOffered() throws Throwable {
        var manager = AppWidgetManager.getInstance(context()); var host = new AppWidgetHost(context(), 84004);
        assertArrayEquals(new WidgetConfig.Type[]{WidgetConfig.Type.GRID_COMPACT, WidgetConfig.Type.THUMB}, WidgetConfig.offeredTypes());
        int offered = 0;
        for (var type : WidgetConfig.Type.values()) {
            int id = host.allocateAppWidgetId();
            try {
                instrumentation().getUiAutomation().adoptShellPermissionIdentity("android.permission.BIND_APPWIDGET");
                assertTrue(manager.bindAppWidgetIdIfAllowed(id, WidgetProviders.component(context(), type)));
                instrumentation().getUiAutomation().dropShellPermissionIdentity();
                var info = manager.getAppWidgetInfo(id);
                if ((info.widgetFeatures & AppWidgetProviderInfo.WIDGET_FEATURE_HIDE_FROM_PICKER) == 0) offered++;
                if (type == WidgetConfig.Type.THUMB) {
                    float density = context().getResources().getDisplayMetrics().density;
                    assertEquals(40*density,info.minResizeWidth,1); assertEquals(40*density,info.minResizeHeight,1);
                    assertEquals(2,info.targetCellWidth); assertEquals(3,info.targetCellHeight);
                }
                if (type.layout != WidgetConfig.Layout.GRID) {
                    var c = fixture(type, 23); c.layout = WidgetConfig.Layout.LEFT_HAND;
                    new ConfigStore(context()).save(id, c);
                    assertEquals(WidgetConfig.Layout.LEFT_HAND, WidgetProviders.load(context(), id).layout);
                    c.layout = WidgetConfig.Layout.RIGHT_HAND; new ConfigStore(context()).save(id, c);
                    assertEquals(WidgetConfig.Layout.RIGHT_HAND, WidgetProviders.load(context(), id).layout);
                }
            } finally { instrumentation().getUiAutomation().dropShellPermissionIdentity(); host.deleteAppWidgetId(id); new ConfigStore(context()).delete(id); }
        }
        assertEquals(2, offered);
    }
    @Test public void smallThumbKeepsAllAppsClickableAndFillsItsOccupiedBounds() throws Throwable {
        onMain(() -> {
            float density = context().getResources().getDisplayMetrics().density;
            for (int count : new int[]{1,2,7,23}) for (int[] size : new int[][]{{80,120},{160,240},{400,500}}) {
                var c = fixture(WidgetConfig.Type.THUMB, count);
                var view = NavigatorWidget.render(context(), 123, c, 0, size[0], size[1]).apply(context(), new FrameLayout(context()));
                view.measure(View.MeasureSpec.makeMeasureSpec(Math.round(size[0] * density), View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(Math.round(size[1] * density), View.MeasureSpec.EXACTLY));
                view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
                FrameLayout icons = view.findViewById(R.id.widget_icons);
                assertEquals(count, icons.getChildCount());
                assertEquals(View.GONE, view.findViewById(R.id.empty).getVisibility());
                int left = Integer.MAX_VALUE, top = Integer.MAX_VALUE;
                for (int i = 0; i < icons.getChildCount(); i++) {
                    View icon = icons.getChildAt(i); assertTrue(icon.hasOnClickListeners());
                    left = Math.min(left,icon.getLeft()); top = Math.min(top,icon.getTop());
                    for (int j = 0; j < i; j++) {
                        View other = icons.getChildAt(j);
                        int overlapW = Math.min(icon.getRight(),other.getRight()) - Math.max(icon.getLeft(),other.getLeft());
                        int overlapH = Math.min(icon.getBottom(),other.getBottom()) - Math.max(icon.getTop(),other.getTop());
                        assertTrue("Targets may only touch after pixel rounding", overlapW <= 1 || overlapH <= 1);
                    }
                }
                assertEquals(0,left); assertEquals(0,top);
            }
        });
    }
    @Test public void gripSettingsRenderAtTheSamePositionsAsTheSharedPreviewGeometry() throws Throwable {
        onMain(() -> {
            float density=context().getResources().getDisplayMetrics().density;
            for (var type:new WidgetConfig.Type[]{WidgetConfig.Type.THUMB,WidgetConfig.Type.LEFT_THUMB,WidgetConfig.Type.RIGHT_THUMB})
            for (int center:new int[]{25,50,100}) for(boolean custom:new boolean[]{false,true})
            for(int[] size:new int[][]{{160,240},{400,500}}) {
                var c=fixture(type,23);c.thumbCenterHeight=center;c.thumbLayerSpacing=50;c.thumbEdgeInset=24;
                c.thumbCustomReach=custom;c.thumbInnerReach=30;c.thumbOuterReach=85;
                var frame=LayoutEngine.frame(c,size[0],size[1],0);
                var view=NavigatorWidget.render(context(),123,c,0,size[0],size[1]).apply(context(),new FrameLayout(context()));
                FrameLayout icons=view.findViewById(R.id.widget_icons);assertEquals(23,icons.getChildCount());
                for(int i=0;i<23;i++) {
                    var p=frame.placements().get(i);var icon=icons.getChildAt(i);
                    var params=(FrameLayout.LayoutParams) icon.getLayoutParams();
                    // Allow complex-dimension quantization plus integer pixel-offset truncation.
                    assertEquals((p.x()-c.tileWidth(p.size())/2)*density,params.leftMargin,2);
                    assertEquals((p.y()-c.tileHeight(p.size())/2)*density,params.topMargin,2);
                    assertTrue(icon.hasOnClickListeners());
                }
            }
        });
    }
    @Test public void appLabelDefaultAffectsFollowingWidgetsButRespectsOverridesInRendering() throws Throwable {
        var store=new ConfigStore(context());boolean previous=store.defaultLabels();
        var following=fixture(WidgetConfig.Type.GRID_COMPACT,1);
        var hidden=fixture(WidgetConfig.Type.GRID_COMPACT,1);hidden.useDefaultLabels=false;hidden.labels=false;
        var shown=fixture(WidgetConfig.Type.GRID_COMPACT,1);shown.useDefaultLabels=false;shown.labels=true;
        try {
            store.defaultLabels(false);store.save(-960,following);store.save(-961,hidden);store.save(-962,shown);
            store.defaultLabels(true);
            assertTrue(store.load(-960).showLabels());assertFalse(store.load(-961).showLabels());assertTrue(store.load(-962).showLabels());
            onMain(() -> {
                float density=context().getResources().getDisplayMetrics().density;
                for(int id:new int[]{-960,-961,-962}) {
                    var c=store.load(id);
                    var view=NavigatorWidget.render(context(),id,c,0,220,280).apply(context(),new FrameLayout(context()));
                    var icon=((FrameLayout) view.findViewById(R.id.widget_icons)).getChildAt(0);
                    assertEquals((id==-961?40:56)*density,icon.getLayoutParams().height,1);
                }
            });
            store.defaultLabels(false);assertFalse(store.load(-960).showLabels());assertTrue(store.load(-962).showLabels());
        } finally {store.defaultLabels(previous);store.delete(-960);store.delete(-961);store.delete(-962);}
    }
    @Test public void iconOpacityChangesPixelsWithoutChangingTargetsOrTextColor() throws Throwable {
        var c = fixture(WidgetConfig.Type.GRID_COMPACT, 1); c.iconOpacity = 50; c.iconBorder = 4; c.iconBorderColor = Color.GREEN;
        var p = new LayoutEngine.Placement(0, 20, 20, 40, 1, 1);
        Bitmap tile = new IconRenderer(context()).tile(c, p, 2);
        assertEquals(128, Color.alpha(tile.getPixel(40, 3)), 1);
        c.automaticTextColor = false; c.textColor = Color.MAGENTA;
        assertEquals(Color.MAGENTA, c.foregroundColor());
        onMain(() -> {
            View view = NavigatorWidget.render(context(), 123, c, 0, 240, 280).apply(context(), new FrameLayout(context()));
            assertTrue(((FrameLayout) view.findViewById(R.id.widget_icons)).getChildAt(0).hasOnClickListeners());
        });
    }
    @Test public void legacyDataMigratesAndDuplicateAppsKeepFirstArtwork() {
        for (String old : new String[]{"ORBIT", "SPHERE", "HALF_SPHERE", "STARFIELD", "CRAWL", "SPIRAL", "HONEYCOMB", "DEPTH", "THOUGHT_MAP"}) {
            var c = ConfigStore.decode("{\"version\":3,\"layout\":\"" + old + "\",\"icons\":[{\"id\":\"first\",\"component\":\"app/.Main\",\"image\":\"art.png\"},{\"component\":\"app/.Alias\"}]}");
            assertEquals(WidgetConfig.Layout.GRID, c.layout); assertEquals(1, c.icons.size()); assertEquals("art.png", c.icons.get(0).image);
        }
        assertEquals(WidgetConfig.Type.LEFT_THUMB, ConfigStore.decode("{\"layout\":\"LEFT_HAND\"}").type);
        assertEquals(WidgetConfig.Type.GRID_COMPACT, ConfigStore.decode("{bad").type);
        var oldThumb = ConfigStore.decode("{\"version\":4,\"type\":\"LEFT_THUMB\",\"layers\":[2,3,4],\"pageDots\":true}");
        assertEquals(WidgetConfig.Layout.LEFT_HAND, oldThumb.layout); assertEquals(List.of(2,3,4), oldThumb.layers);
        assertEquals(List.of(100,100,100), oldThumb.layerScales);
        var oldLabels=ConfigStore.decode("{\"version\":5,\"labels\":true}");
        assertFalse(oldLabels.useDefaultLabels);assertTrue(oldLabels.showLabels());
        var oldHidden=ConfigStore.decode("{\"version\":5,\"labels\":false}");
        oldHidden.defaultLabels=true;assertFalse(oldHidden.showLabels());
        assertEquals(0,oldLabels.thumbCenterHeight);assertEquals(100,oldLabels.thumbLayerSpacing);assertEquals(0,oldLabels.thumbEdgeInset,0);
        var version6=ConfigStore.decode("{\"version\":6,\"type\":\"THUMB\",\"thumbLayerSpacing\":55}");
        assertFalse(version6.thumbCustomReach);assertEquals(55,version6.thumbLayerSpacing);
        assertEquals(25,version6.thumbInnerReach);assertEquals(100,version6.thumbOuterReach);
        assertTrue(ConfigStore.decode("{}").useDefaultLabels);
        assertEquals(0, ConfigStore.decode("{}").outerPadding, 0);
        assertEquals(12, ConfigStore.decode("{\"outer\":12}").outerPadding, 0);
    }
    @Test public void newSettingsPersistIndependentlyForEachWidget() {
        var store = new ConfigStore(context()); var c = fixture(WidgetConfig.Type.LEFT_THUMB, 15);
        c.layers.set(0, 5); c.layerScales.set(1, 125); c.thumbCenterHeight=35; c.thumbLayerSpacing=55; c.thumbEdgeInset=28; c.useDefaultLabels=false; c.labels=true; c.layout = WidgetConfig.Layout.RIGHT_HAND; c.gradientEnd = Color.CYAN; c.gradientAngle = 123; c.backgroundImage = "example.png";
        c.backgroundArt = WidgetConfig.BackgroundArt.IMAGE; c.iconOpacity = 65; c.automaticTextColor = false; c.textColor = Color.MAGENTA; c.labelShadow = false; c.exaggeration = 150; c.icons.get(0).image = "custom.png";
        c.thumbCustomReach=true;c.thumbInnerReach=35;c.thumbOuterReach=90;
        try {
            store.save(-901, c); store.save(-902, new WidgetConfig());
            assertEquals(ConfigStore.encode(c), ConfigStore.encode(store.load(-901)));
            assertEquals(0, store.load(-902).icons.size());
        } finally { store.delete(-901); store.delete(-902); }
    }
    @Test public void gradientAndImageOverlayHaveCorrectColorsAndOpacity() throws Exception {
        var c = new WidgetConfig(); c.backgroundArt = WidgetConfig.BackgroundArt.GRADIENT;
        c.background = Color.RED; c.gradientEnd = Color.BLUE; c.gradientAngle = 0; c.artOpacity = 100; c.cornerRadius = 0;
        Bitmap output = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        BackdropRenderer.draw(new Canvas(output), c, new IconRenderer(context()), 100, 100);
        assertTrue(Color.red(output.getPixel(5, 50)) > 220); assertTrue(Color.blue(output.getPixel(95, 50)) > 220);
        java.io.File file = new java.io.File(context().getFilesDir(), "overlay-test.png");
        Bitmap source = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888); source.eraseColor(Color.GREEN);
        try {
            try (var out = new java.io.FileOutputStream(file)) { source.compress(Bitmap.CompressFormat.PNG, 100, out); }
            c.backgroundArt = WidgetConfig.BackgroundArt.IMAGE; c.backgroundImage = file.getName(); c.artOpacity = 50;
            output.eraseColor(Color.TRANSPARENT); BackdropRenderer.draw(new Canvas(output), c, new IconRenderer(context()), 100, 100);
            assertEquals(128, Color.alpha(output.getPixel(50, 50)), 1); assertEquals(255, Color.green(output.getPixel(50, 50)));
        } finally { file.delete(); }
    }
    @Test public void grayscaleAndTintPreserveTransparentPixels() {
        Bitmap source = Bitmap.createBitmap(2, 1, Bitmap.Config.ARGB_8888); source.setPixel(0, 0, Color.RED);
        for (var effect : new WidgetConfig.Effect[]{WidgetConfig.Effect.GRAYSCALE, WidgetConfig.Effect.TINT}) {
            var c = new WidgetConfig(); c.effect = effect; c.tint = Color.CYAN;
            Bitmap output = Bitmap.createBitmap(2, 1, Bitmap.Config.ARGB_8888); Paint paint = new Paint(); paint.setColorFilter(IconRenderer.filter(c));
            new Canvas(output).drawBitmap(source, 0, 0, paint); int color = output.getPixel(0, 0);
            assertEquals(0, Color.alpha(output.getPixel(1, 0)));
            if (effect == WidgetConfig.Effect.GRAYSCALE) { assertEquals(Color.red(color), Color.green(color)); assertEquals(Color.red(color), Color.blue(color)); }
            else assertEquals(Color.CYAN, color);
        }
    }
}
