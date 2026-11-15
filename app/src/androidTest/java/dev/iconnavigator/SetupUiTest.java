package dev.iconnavigator;

import android.view.*;
import android.view.inspector.WindowInspector;
import android.widget.*;
import android.os.SystemClock;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import static org.junit.Assert.*;

public class SetupUiTest {
    private View text(View root, String label) {
        if (root instanceof TextView t && label.equals(t.getText().toString())) return root;
        if (root instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) { View found = text(group.getChildAt(i), label); if (found != null) return found; }
        return null;
    }
    private View description(View root, String label) {
        if (label.contentEquals(root.getContentDescription() == null ? "" : root.getContentDescription())) return root;
        if (root instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) { View found = description(group.getChildAt(i), label); if (found != null) return found; }
        return null;
    }
    private <T extends View> T kind(View root, Class<T> type) {
        if (type.isInstance(root)) return type.cast(root);
        if (root instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) { T found = kind(group.getChildAt(i), type); if (found != null) return found; }
        return null;
    }
    private View windowWith(String label) {
        for (View root : WindowInspector.getGlobalWindowViews()) if (text(root, label) != null) return root;
        return null;
    }
    private WidgetConfig fixture(int count) {
        WidgetConfig c = new WidgetConfig();
        c.thumbCustomReach=false;c.thumbCenterHeight=0;c.thumbLayerSpacing=100;c.thumbInnerReach=25;c.thumbOuterReach=100;
        java.util.Collections.fill(c.layerScales,100);
        for (int i = 0; i < count; i++) c.addIcon(new WidgetConfig.IconEntry("one-" + i, "sample" + i + "/.Main", "Example " + i));
        return c;
    }
    private void draft(int count, java.util.function.Consumer<ActivityScenario<EditorActivity>> action) {
        draft(count,false,action);
    }
    private void draft(int count, boolean defaultLabels, java.util.function.Consumer<ActivityScenario<EditorActivity>> action) {
        var context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        var store = new ConfigStore(context); boolean exists = store.exists(0); var previous = store.load(0); boolean previousDefault = store.defaultLabels(); store.defaultLabels(defaultLabels); store.save(0, fixture(count));
        try (var scenario = ActivityScenario.launch(EditorActivity.class)) { action.accept(scenario); }
        finally { store.defaultLabels(previousDefault); if (exists) store.save(0, previous); else store.delete(0); }
    }
    @Test public void viewsStayOpenAfterRemovalAndStateSurvivesRecreation() {
        draft(2, scenario -> {
            scenario.onActivity(a -> {
                View root = a.getWindow().getDecorView(); assertNotNull(description(root, "Widget preview"));
                assertTrue(text(root, "Save widget").isEnabled());
                ((Spinner) description(root,"Show icon labels")).setSelection(1);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(a -> {
                View root=a.getWindow().getDecorView();
                text(root, "Visual options").performClick(); assertNotNull(text(root, "Icon colors"));
                assertNull(text(root, "+ Add apps")); assertNull(description(root, "Columns"));
                assertTrue(((PreviewView) description(root,"Widget preview")).config.showLabels());
                text(root, "Select apps").performClick(); description(root, "Remove Example 0").performClick();
                assertEquals(1, ((PreviewView) description(root, "Widget preview")).config.icons.size());
                text(root, "Visual options").performClick(); assertTrue(((PreviewView) description(root,"Widget preview")).config.showLabels());
            });
            scenario.recreate();
            scenario.onActivity(a -> {
                View root = a.getWindow().getDecorView(); assertNotNull(text(root, "Icon colors"));
                assertEquals(1, ((PreviewView) description(root, "Widget preview")).config.icons.size());
                text(root, "Select apps").performClick(); description(root, "Remove Example 1").performClick();
                assertFalse(text(root, "Save widget").isEnabled());
                text(root, "Visual options").performClick(); assertNotNull(text(root, "Icon colors"));
                text(root, "Basic settings").performClick(); assertNotNull(description(root, "Columns"));
                assertNotNull(description(root, "Widget preview"));
            });
        });
    }
    @Test public void widgetTypeChoosesBasicControlsAndCanSaveFromAnyView() {
        draft(1, scenario -> {
            scenario.onActivity(a -> ((Spinner) description(a.getWindow().getDecorView(), "Widget type")).setSelection(1));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(a -> {
                View root = a.getWindow().getDecorView(); assertNotNull(description(root, "Number of layers")); assertNull(description(root, "Columns"));
                assertNotNull(description(root, "Icons in layer 1 (nearest thumb)"));
                assertNotNull(description(root,"Layer spacing (%)"));assertNull(text(root,"Default app names"));
                text(root, "Visual options").performClick();assertNull(description(root,"Layer spacing (%)"));
                text(root, "Select apps").performClick();
                assertNotNull(description(root, "Widget preview")); text(root, "Save widget").performClick();
                assertEquals(WidgetConfig.Type.THUMB, new ConfigStore(a).load(0).type);
            });
        });
    }
    @Test public void innerAndOuterReachPreviewPersistAndPresetsKeepThem() {
        draft(12,scenario -> {
            scenario.onActivity(a -> ((Spinner)description(a.getWindow().getDecorView(),"Widget type")).setSelection(1));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(a -> ((Spinner)description(a.getWindow().getDecorView(),"Thumb reach")).setSelection(1));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            SystemClock.sleep(150); // Allow the posted layout/scroll to draw a frame.
            int[] chosen=new int[2];
            scenario.onActivity(a -> {
                View root=a.getWindow().getDecorView();var preview=(PreviewView)description(root,"Widget preview");
                assertTrue(preview.config.thumbCustomReach);assertNull(description(root,"Layer spacing (%)"));
                var before=preview.frame().placements();
                SeekBar inner=(SeekBar)description(root,"Inner reach (%)"),outer=(SeekBar)description(root,"Outer reach (%)");
                assertTrue(inner.getGlobalVisibleRect(new android.graphics.Rect()));
                assertTrue(outer.getGlobalVisibleRect(new android.graphics.Rect()));
                inner.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT,new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_RIGHT));
                outer.onKeyDown(KeyEvent.KEYCODE_DPAD_LEFT,new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_LEFT));
                assertTrue(preview.config.thumbInnerReach>25);assertTrue(preview.config.thumbOuterReach<100);
                assertEquals(preview.config.thumbInnerReach+1,outer.getMin());
                assertEquals(preview.config.thumbOuterReach-1,inner.getMax());
                assertNotEquals(before,preview.frame().placements());
                chosen[0]=preview.config.thumbInnerReach;chosen[1]=preview.config.thumbOuterReach;
                text(root,"Select apps").performClick();text(root,"Visual options").performClick();
                ((Spinner)description(root,"Appearance preset")).setSelection(3);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();scenario.recreate();
            scenario.onActivity(a -> {
                View root=a.getWindow().getDecorView();var c=((PreviewView)description(root,"Widget preview")).config;
                assertTrue(c.thumbCustomReach);assertEquals(chosen[0],c.thumbInnerReach);assertEquals(chosen[1],c.thumbOuterReach);
                text(root,"Basic settings").performClick();
                assertEquals(chosen[0],((SeekBar)description(root,"Inner reach (%)")).getProgress());
                text(root,"Save widget").performClick();var saved=new ConfigStore(a).load(0);
                assertTrue(saved.thumbCustomReach);assertEquals(chosen[0],saved.thumbInnerReach);assertEquals(chosen[1],saved.thumbOuterReach);
            });
        });
    }
    @Test public void overflowWarnsInEveryViewAndPreventsSaving() {
        draft(13, scenario -> scenario.onActivity(a -> {
            View root = a.getWindow().getDecorView();
            for (String tab : new String[]{"Basic settings", "Select apps", "Visual options"}) {
                text(root, tab).performClick();
                assertFalse(text(root, "Save widget").isEnabled());
                assertTrue(((TextView) description(root, "Layout warning")).getText().toString().contains("13 apps selected, but only 12 positions"));
            }
            text(root, "Select apps").performClick(); description(root, "Remove Example 12").performClick();
            assertTrue(text(root, "Save widget").isEnabled());
            assertEquals(View.GONE, description(root, "Layout warning").getVisibility());
        }));
    }
    @Test public void thumbSideLayersAndIndependentScalesSurviveRecreation() {
        draft(23, scenario -> {
            var instrumentation = InstrumentationRegistry.getInstrumentation();
            scenario.onActivity(a -> ((Spinner) description(a.getWindow().getDecorView(), "Widget type")).setSelection(1));
            instrumentation.waitForIdleSync();
            scenario.onActivity(a -> {
                View root = a.getWindow().getDecorView();
                assertTrue(text(root, "Save widget").isEnabled());
                assertEquals(4, ((PreviewView) description(root, "Widget preview")).config.layers.size());
                ((Spinner) description(root, "Thumb side")).setSelection(1);
            });
            instrumentation.waitForIdleSync();
            scenario.onActivity(a -> {
                View root = a.getWindow().getDecorView();
                var preview=(PreviewView) description(root,"Widget preview");
                assertEquals(WidgetConfig.Layout.LEFT_HAND,preview.config.layout);
                var before=preview.frame().placements();
                SeekBar center=(SeekBar) description(root,"Thumb center height (%)");
                center.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT,new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_RIGHT));
                assertTrue(preview.config.thumbCenterHeight>0);assertNotEquals(before,preview.frame().placements());
                SeekBar spacing=(SeekBar) description(root,"Layer spacing (%)");
                before=preview.frame().placements();
                spacing.onKeyDown(KeyEvent.KEYCODE_DPAD_LEFT,new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_LEFT));
                assertTrue(preview.config.thumbLayerSpacing<100);assertNotEquals(before,preview.frame().placements());
                ((Spinner) description(root, "Number of layers")).setSelection(4);
            });
            instrumentation.waitForIdleSync();
            scenario.onActivity(a -> {
                View root = a.getWindow().getDecorView(); text(root, "Visual options").performClick();
                var preview=(PreviewView) description(root,"Widget preview");
                assertNull(description(root,"Layer spacing (%)"));
                assertNull(text(root,"Thumb spacing"));
                assertNull(description(root,"Distance from side edge (dp)"));
                assertTrue(preview.config.thumbLayerSpacing<100);
                SeekBar scale = (SeekBar) description(root, "Layer 2 icon scale (%)");
                scale.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
            });
            scenario.recreate();
            scenario.onActivity(a -> {
                var c = ((PreviewView) description(a.getWindow().getDecorView(), "Widget preview")).config;
                assertEquals(WidgetConfig.Layout.LEFT_HAND, c.layout); assertEquals(5, c.layers.size());
                assertTrue(c.layerScales.get(1) > 100); assertEquals(100, (int) c.layerScales.get(0));
                assertTrue(c.thumbCenterHeight>0);assertTrue(c.thumbLayerSpacing<100);
            });
        });
    }
    @Test public void smallBoundThumbUsesActualPreviewSizeWarnsAndCanSave() {
        var instrumentation = InstrumentationRegistry.getInstrumentation();
        var context = instrumentation.getTargetContext();
        var manager = android.appwidget.AppWidgetManager.getInstance(context);
        var host = new android.appwidget.AppWidgetHost(context, 84009);
        var store = new ConfigStore(context); int id = host.allocateAppWidgetId();
        try {
            instrumentation.getUiAutomation().adoptShellPermissionIdentity("android.permission.BIND_APPWIDGET");
            assertTrue(manager.bindAppWidgetIdIfAllowed(id, WidgetProviders.component(context, WidgetConfig.Type.THUMB)));
            instrumentation.getUiAutomation().dropShellPermissionIdentity();
            var c = fixture(23); c.selectType(WidgetConfig.Type.THUMB, true); c.useDefaultLabels = false; c.labels = true; store.save(id,c);
            var options = new android.os.Bundle();
            options.putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,80);
            options.putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,120);
            manager.updateAppWidgetOptions(id,options);
            var intent = new android.content.Intent(context,EditorActivity.class)
                    .putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID,id);
            try (var scenario = ActivityScenario.<EditorActivity>launch(intent)) {
                instrumentation.waitForIdleSync();
                scenario.onActivity(a -> {
                    View root = a.getWindow().getDecorView();
                    var preview = (PreviewView) description(root,"Widget preview");
                    float density = a.getResources().getDisplayMetrics().density;
                    assertEquals(80,preview.getLayoutParams().width/density,1);
                    assertEquals(120,preview.getLayoutParams().height/density,1);
                    assertEquals(23,preview.frame().placements().size());
                    assertTrue(((TextView) description(root,"Layout warning")).getText().toString().contains("Icons shrink to fit"));
                    for (String tab : new String[]{"Basic settings","Select apps","Visual options"}) {
                        text(root,tab).performClick(); assertTrue(text(root,"Save widget").isEnabled());
                    }
                    text(root,"Save widget").performClick(); assertEquals(23,store.load(id).icons.size());
                });
            }
        } finally {
            instrumentation.getUiAutomation().dropShellPermissionIdentity(); host.deleteAppWidgetId(id); store.delete(id);
        }
    }
    @Test public void iconLabelDropdownPreviewsPersistsAndLeavesDefaultsUnchanged() {
        draft(2,true,scenario -> {
            var instrumentation=InstrumentationRegistry.getInstrumentation();
            var store=new ConfigStore(instrumentation.getTargetContext());
            scenario.onActivity(a -> {
                View root=a.getWindow().getDecorView();
                var preview=(PreviewView) description(root,"Widget preview");
                Spinner labels=(Spinner) description(root,"Show icon labels");
                assertEquals(3,labels.getCount());
                assertEquals("Follow defaults",labels.getItemAtPosition(0));
                assertEquals("Yes",labels.getItemAtPosition(1));assertEquals("No",labels.getItemAtPosition(2));
                assertTrue(preview.config.useDefaultLabels);assertTrue(preview.config.showLabels());
                assertNull(text(root,"Default app names"));
                labels.setSelection(2);
            });
            instrumentation.waitForIdleSync();
            scenario.onActivity(a -> {
                View root=a.getWindow().getDecorView();
                assertFalse(((PreviewView) description(root,"Widget preview")).config.showLabels());
                text(root,"Save widget").performClick();
            });
            instrumentation.waitForIdleSync();
            assertTrue(store.defaultLabels());assertFalse(store.load(0).useDefaultLabels);assertFalse(store.load(0).showLabels());
            scenario.recreate();
            scenario.onActivity(a -> {
                View root=a.getWindow().getDecorView();
                assertEquals(2,((Spinner) description(root,"Show icon labels")).getSelectedItemPosition());
                ((Spinner) description(root,"Show icon labels")).setSelection(1);
            });
            instrumentation.waitForIdleSync();
            store.defaultLabels(false);
            scenario.onActivity(a -> {
                View root=a.getWindow().getDecorView();
                assertTrue(((PreviewView) description(root,"Widget preview")).config.showLabels());
                text(root,"Save widget").performClick();
            });
            instrumentation.waitForIdleSync();
            assertFalse(store.defaultLabels());assertTrue(store.load(0).showLabels());
            scenario.recreate();
            scenario.onActivity(a -> ((Spinner) description(a.getWindow().getDecorView(),"Show icon labels")).setSelection(0));
            instrumentation.waitForIdleSync();
            scenario.onActivity(a -> {
                var c=((PreviewView) description(a.getWindow().getDecorView(),"Widget preview")).config;
                assertTrue(c.useDefaultLabels);assertFalse(c.showLabels());
            });
        });
    }
    @Test public void advancedExpansionRevealsSettingsAndPresetsRemainEditable() {
        draft(2, scenario -> {
            var instrumentation = InstrumentationRegistry.getInstrumentation();
            for (int preset = 1; preset < Themes.NAMES.length; preset++) {
                final int choice = preset;
                scenario.onActivity(a -> {
                    View root = a.getWindow().getDecorView(); text(root, "Visual options").performClick();
                    ((Spinner) description(root, "Appearance preset")).setSelection(choice);
                });
                instrumentation.waitForIdleSync();
                scenario.onActivity(a -> {
                    View root = a.getWindow().getDecorView(); var c = ((PreviewView) description(root, "Widget preview")).config;
                    assertEquals(2, c.icons.size()); assertEquals(WidgetConfig.Type.GRID_COMPACT, c.type);
                    text(root, "Background & overlays ▸").performClick();
                    assertNotNull(text(root, "Background / gradient start color"));
                    SeekBar opacity = (SeekBar) description(root, "Background opacity (%)");
                    int old = c.opacity; opacity.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
                    assertTrue(c.opacity > old);
                    if (choice == 6) {
                        assertNotNull(text(root, "Gradient end color"));
                        assertNotNull(description(root, "Gradient direction (degrees)"));
                        assertNotNull(description(root, "Overlay opacity (%)"));
                    }
                    text(root, "Background & overlays ▾").performClick();
                    kind(root, ScrollView.class).fullScroll(View.FOCUS_DOWN);
                });
                instrumentation.waitForIdleSync();
                final int[] before = new int[1];
                scenario.onActivity(a -> {
                    View root = a.getWindow().getDecorView(); before[0] = kind(root, ScrollView.class).getScrollY();
                    text(root, "Advanced settings ▸").performClick();
                });
                // ScrollView animates the reveal; wait for that animation, not just an idle queue.
                try { Thread.sleep(500); } catch (InterruptedException e) { throw new AssertionError(e); }
                scenario.onActivity(a -> {
                    View root = a.getWindow().getDecorView();
                    assertTrue("Expansion must reveal the new controls", kind(root, ScrollView.class).getScrollY() > before[0]);
                    var visible = new android.graphics.Rect();
                    assertTrue(text(root, "Icon sizing and spacing").getGlobalVisibleRect(visible));
                    var c = ((PreviewView) description(root, "Widget preview")).config;
                    SeekBar padding = (SeekBar) description(root, "Padding around the widget (dp)");
                    float old = c.outerPadding;
                    padding.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
                    assertTrue(c.outerPadding > old);
                    assertNotNull(description(root, "Padding beside each icon (dp)"));
                    assertNotNull(description(root, "Padding above / below (dp)"));
                    assertNotNull(description(root, "Widget border width (dp)"));
                    assertNotNull(description(root, "Icon border width (dp)"));
                    SeekBar opacity = (SeekBar) description(root, "Icon opacity (%)");
                    opacity.onKeyDown(KeyEvent.KEYCODE_DPAD_LEFT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT));
                    assertTrue(c.iconOpacity < 100);
                    ((Switch) text(root, "Automatic text color")).setChecked(false);
                    ((Switch) text(root, "Shadow behind app names")).setChecked(false);
                    assertFalse(c.automaticTextColor); assertFalse(c.labelShadow);
                    assertNotNull(description(root, "Widget preview"));
                    text(root, "Advanced settings ▾").performClick();
                });
            }
        });
    }
    @Test public void actualPickerAddsOnlyOnceAndRemovalMakesAppAvailableAgain() {
        draft(0, scenario -> {
            try { Thread.sleep(500); } catch (InterruptedException e) { throw new AssertionError(e); }
            var instrumentation = InstrumentationRegistry.getInstrumentation();
            final String[] label = new String[1]; final int[] available = new int[1];
            scenario.onActivity(a -> {
                View root = a.getWindow().getDecorView(); text(root, "Select apps").performClick(); text(root, "+ Add apps").performClick();
            });
            instrumentation.waitForIdleSync();
            scenario.onActivity(a -> {
                View dialog = windowWith("Add selected"); assertNotNull(dialog);
                ListView list = kind(dialog, ListView.class); assertTrue(list.getCount() > 1); available[0] = list.getCount();
                label[0] = list.getItemAtPosition(0).toString();
                list.performItemClick(list.getChildAt(0), 0, list.getAdapter().getItemId(0));
                assertTrue(list.isItemChecked(0)); text(dialog, "Add selected").performClick();
            });
            // AlertDialog posts its button callback; allow it to run before checking the draft.
            instrumentation.waitForIdleSync();
            scenario.onActivity(a -> {
                View root = a.getWindow().getDecorView();
                assertEquals(1, ((PreviewView) description(root, "Widget preview")).config.icons.size());
                text(root, "Visual options").performClick(); text(root, "Select apps").performClick(); text(root, "+ Add apps").performClick();
            });
            instrumentation.waitForIdleSync();
            scenario.onActivity(a -> {
                View dialog = windowWith("Add selected"); assertEquals(available[0] - 1, kind(dialog, ListView.class).getCount()); text(dialog, "Cancel").performClick();
            });
            instrumentation.waitForIdleSync();
            scenario.onActivity(a -> {
                View root = a.getWindow().getDecorView(); description(root, "Remove " + label[0]).performClick(); text(root, "+ Add apps").performClick();
            });
            instrumentation.waitForIdleSync();
            scenario.onActivity(a -> {
                View dialog = windowWith("Add selected"); assertEquals(available[0], kind(dialog, ListView.class).getCount()); text(dialog, "Cancel").performClick();
            });
        });
    }
    @Test public void longPressDragReordersActualPreviewWithoutLosingApps() {
        draft(6, scenario -> {
            final PreviewView[] preview = new PreviewView[1]; final float[] target = new float[2];
            final long downTime = SystemClock.uptimeMillis();
            scenario.onActivity(a -> text(a.getWindow().getDecorView(), "Select apps").performClick());
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(a -> {
                preview[0] = (PreviewView) description(a.getWindow().getDecorView(), "Widget preview");
                var items = preview[0].frame().placements(); float density = a.getResources().getDisplayMetrics().density;
                target[0] = items.get(5).x() * density; target[1] = items.get(5).y() * density;
                MotionEvent event = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, items.get(0).x() * density, items.get(0).y() * density, 0);
                preview[0].onTouchEvent(event); event.recycle();
            });
            try { Thread.sleep(ViewConfiguration.getLongPressTimeout() + 100); } catch (InterruptedException e) { throw new AssertionError(e); }
            scenario.onActivity(a -> {
                for (int action : new int[]{MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP}) {
                    MotionEvent event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, target[0], target[1], 0);
                    preview[0].onTouchEvent(event); event.recycle();
                }
                assertEquals("one-0", preview[0].config.icons.get(5).id); assertEquals(6, preview[0].config.icons.size());
            });
        });
    }
}
