package dev.iconnavigator;

import org.junit.Test;
import static org.junit.Assert.*;

public class WidgetConfigTest {
    @Test public void newThumbUsesTheStartingGripProfileWithoutChoosingPersonalApps() {
        var c=new WidgetConfig();assertEquals(40,c.iconSize,0);
        c.selectType(WidgetConfig.Type.THUMB,true);
        assertEquals(47,c.iconSize,0);assertTrue(c.thumbCustomReach);
        assertEquals(17,c.thumbCenterHeight);assertEquals(34,c.thumbInnerReach);assertEquals(95,c.thumbOuterReach);
        assertEquals(java.util.List.of(2,5,7,9),c.layers);
        assertEquals(java.util.List.of(150,121,105,100),c.layerScales);
        assertTrue(c.icons.isEmpty());assertEquals(0,c.outerPadding,0);assertFalse(c.showLabels());
    }
    @Test public void appsAndActivityAliasesCannotBeAddedTwice() {
        var c = new WidgetConfig();
        var first = new WidgetConfig.IconEntry("one", "app/.Main", "One"); first.image = "art.png";
        assertTrue(c.addIcon(first)); assertFalse(c.addIcon(new WidgetConfig.IconEntry("two", "app/.Other", "Alias")));
        assertTrue(c.containsApp("app/app.Main", null)); assertFalse(c.containsApp("app/.Main", first));
        c.icons.add(new WidgetConfig.IconEntry("old-dupe", "app/app.Main", "Old duplicate")); c.normalize();
        assertEquals(1, c.icons.size()); assertEquals("one", c.icons.get(0).id); assertEquals("art.png", c.icons.get(0).image);
    }
    @Test public void reorderPreservesIdentityArtworkAndTarget() {
        var c = LayoutEngineTest.fixture(WidgetConfig.Type.GRID_COMPACT, 4);
        c.icons.get(0).image = "custom.png"; c.move(0, 3);
        assertEquals("id-0", c.icons.get(3).id); assertEquals("app0/.Main", c.icons.get(3).component);
        assertEquals("custom.png", c.icons.get(3).image);
    }
    @Test public void presetsPreserveWidgetTypeLayersAndApps() {
        var c = LayoutEngineTest.fixture(WidgetConfig.Type.LEFT_THUMB, 6); c.layers.set(0, 5); c.exaggeration = 150; c.layerScales.set(0, 120);
        for (int i = 1; i < Themes.NAMES.length; i++) {
            Themes.apply(c, i); assertEquals(WidgetConfig.Type.LEFT_THUMB, c.type); assertEquals(5, (int) c.layers.get(0));
            assertEquals(6, c.icons.size()); assertEquals(150, c.exaggeration); assertEquals(120, (int) c.layerScales.get(0));
        }
        Themes.apply(c, 2); assertEquals(0, c.opacity); assertFalse(c.labels); assertEquals(WidgetConfig.Effect.GRAYSCALE, c.effect);
    }
    @Test public void invalidConfigurationIsBounded() {
        var c = LayoutEngineTest.fixture(WidgetConfig.Type.RIGHT_THUMB, 1);
        c.columns = 0; c.rows = 200; c.layers.clear(); c.layers.add(0); c.layers.add(100);
        c.iconSize = Float.NaN; c.exaggeration = 300; c.gradientAngle = -5; c.normalize();
        assertEquals(1, c.columns); assertEquals(12, c.rows); assertEquals(1, (int) c.layers.get(0)); assertEquals(16, (int) c.layers.get(1));
        assertTrue(Float.isFinite(c.iconSize)); assertEquals(200, c.exaggeration); assertEquals(0, c.gradientAngle);
    }
    @Test public void labelDefaultCanBeOverriddenAndSmallLabelsRemainHidden() {
        var c=new WidgetConfig();assertFalse(c.showLabels());
        c.defaultLabels=true;assertTrue(c.showLabels());assertTrue(c.labelsAt(40));assertFalse(c.labelsAt(20));
        c.useDefaultLabels=false;c.labels=false;assertFalse(c.showLabels());
        c.defaultLabels=false;c.labels=true;assertTrue(c.showLabels());
        c.useDefaultLabels=true;assertFalse(c.showLabels());
    }
    @Test public void presetsPreserveGripAndLabelOverrides() {
        var c=LayoutEngineTest.fixture(WidgetConfig.Type.THUMB,23);
        c.layout=WidgetConfig.Layout.LEFT_HAND;c.thumbCenterHeight=50;c.thumbLayerSpacing=60;c.thumbEdgeInset=24;
        c.thumbCustomReach=true;c.thumbInnerReach=30;c.thumbOuterReach=85;
        c.useDefaultLabels=false;c.labels=true;c.defaultLabels=true;
        for(int preset=1;preset<Themes.NAMES.length;preset++) {
            Themes.apply(c,preset);
            assertEquals(WidgetConfig.Layout.LEFT_HAND,c.layout);assertEquals(50,c.thumbCenterHeight);
            assertEquals(60,c.thumbLayerSpacing);assertEquals(24,c.thumbEdgeInset,0);
            assertTrue(c.thumbCustomReach);assertEquals(30,c.thumbInnerReach);assertEquals(85,c.thumbOuterReach);
            assertFalse(c.useDefaultLabels);assertTrue(c.labels);assertTrue(c.defaultLabels);
        }
    }
    @Test public void gripSettingsNormalizeToTheirEditableRanges() {
        var c=new WidgetConfig();c.thumbCenterHeight=-10;c.thumbLayerSpacing=400;c.thumbEdgeInset=Float.NaN;c.normalize();
        assertEquals(0,c.thumbCenterHeight);assertEquals(150,c.thumbLayerSpacing);assertEquals(0,c.thumbEdgeInset,0);
        c.thumbCenterHeight=200;c.thumbLayerSpacing=-1;c.thumbEdgeInset=200;c.normalize();
        assertEquals(100,c.thumbCenterHeight);assertEquals(25,c.thumbLayerSpacing);assertEquals(96,c.thumbEdgeInset,0);
        c.thumbInnerReach=-10;c.thumbOuterReach=200;c.normalize();
        assertEquals(1,c.thumbInnerReach);assertEquals(100,c.thumbOuterReach);
        c.thumbInnerReach=200;c.thumbOuterReach=0;c.normalize();
        assertEquals(99,c.thumbInnerReach);assertEquals(100,c.thumbOuterReach);
    }

}
