package dev.iconnavigator;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class LayoutEngineTest {
    static WidgetConfig fixture(WidgetConfig.Type type, int count) {
        WidgetConfig c = new WidgetConfig(); c.selectType(type, true);
        c.iconSize=40;c.thumbCustomReach=false;c.thumbCenterHeight=0;c.thumbLayerSpacing=100;
        java.util.Collections.fill(c.layerScales,100);
        for (int i = 0; i < count; i++) c.addIcon(new WidgetConfig.IconEntry("id-" + i, "app" + i + "/.Main", "App " + i));
        return c;
    }
    private void inside(WidgetConfig c, LayoutEngine.Frame f) {
        for (var p : f.placements()) {
            assertTrue(Float.isFinite(p.size()));
            assertTrue(p.x() - c.tileWidth(p.size()) / 2 >= -.02);
            assertTrue(p.x() + c.tileWidth(p.size()) / 2 <= f.contentWidth() + .02);
            assertTrue(p.y() - c.tileHeight(p.size()) / 2 >= -.02);
            assertTrue(p.y() + c.tileHeight(p.size()) / 2 <= f.contentHeight() + .02);
            assertEquals(p.index(), LayoutEngine.hitTest(c, f.placements(), p.x(), p.y()));
        }
    }
    @Test public void cleanGridRemainsThreeByFourWithoutNavigationOrMargins() {
        var c = fixture(WidgetConfig.Type.GRID_COMPACT, 12);
        var f = LayoutEngine.frame(c, 220, 280, 0);
        assertEquals(12, f.placements().size()); assertFalse(f.controls());
        assertEquals(0, c.backgroundArgb()); assertFalse(c.labels); assertEquals(0, c.outerPadding, 0);
        c.exaggeration = 200; assertEquals(f, LayoutEngine.frame(c, 220, 280, 99));
        for (var p : f.placements()) assertEquals(40, p.size(), .01);
    }
    @Test public void allProvidersAndAppCountsKeepAssignmentsAndNeverPaginate() {
        for (var type : WidgetConfig.Type.values())
        for (int[] dimensions : new int[][]{{120,160},{160,120},{220,280},{420,500},{500,180},{64,400},{400,64}})
        for (int count : new int[]{0,1,8,9,12,23,31,144,400})
        for (boolean labels : new boolean[]{false,true}) {
            var c = fixture(type, count); c.useDefaultLabels = false; c.labels = labels;
            float w = dimensions[0], h = dimensions[1];
            var f = LayoutEngine.frame(c, w, h, 1000);
            assertFalse(f.controls()); assertEquals(1, f.pages()); assertEquals(0, f.page());
            assertEquals(c.capacity(), f.capacity()); assertEquals(Math.min(count, c.capacity()), f.placements().size());
            for (int i = 0; i < f.placements().size(); i++) assertEquals(i, f.placements().get(i).index());
            assertEquals(count, c.icons.size());
            assertEquals(count > 0 && count <= c.capacity(), new SetupFlow().canSave(c));
            inside(c, f);
        }
    }
    @Test public void gridStripsAndDenseGridsUseFullHeightAndWidth() {
        for (int[] shape : new int[][]{{1,1},{8,1},{1,10},{3,4},{12,12}})
        for (int[] dimensions : new int[][]{{40,40},{64,400},{400,64},{120,160},{500,500}})
        for (int pad : new int[]{0,8,64}) for (boolean labels : new boolean[]{false,true}) {
            var c = fixture(WidgetConfig.Type.GRID_COMPACT, 400);
            c.columns = shape[0]; c.rows = shape[1]; c.outerPadding = pad; c.useDefaultLabels = false; c.labels = labels;
            var f = LayoutEngine.frame(c, dimensions[0], dimensions[1], 0);
            assertEquals(dimensions[0], f.contentWidth(), 0); assertEquals(dimensions[1], f.contentHeight(), 0);
            assertFalse(f.controls()); inside(c, f);
        }
        var c = fixture(WidgetConfig.Type.GRID_COMPACT, 9); c.columns = 8; c.rows = 1;
        c.paddingX = c.paddingY = 0; c.iconSize = 96;
        var f = LayoutEngine.frame(c, 320, 40, 0);
        assertEquals(8, f.placements().size()); assertEquals(20, f.placements().get(0).x(), .01);
        assertEquals(40, f.placements().get(0).size(), .01); assertFalse(new SetupFlow().canSave(c));
    }
    @Test public void defaultThumbAlwaysShowsAllTwentyThreeIconsAndShrinksToFit() {
        var c = fixture(WidgetConfig.Type.THUMB, 23);
        assertEquals(List.of(2,5,7,9), c.layers);
        for (int[] size : new int[][]{{40,40},{120,160},{160,240},{380,420},{700,800}}) {
            var f = LayoutEngine.frame(c, size[0], size[1], 0);
            assertEquals(23, f.capacity()); assertEquals(23, f.placements().size()); assertFalse(f.controls());
            inside(c, f);
        }
        var small = LayoutEngine.frame(c, 160, 240, 0);
        assertTrue(small.iconScale() < 1); assertTrue(small.placements().get(0).size() < 32);
    }
    @Test public void spacingTightensBeforeArtworkShrinks() {
        var c = fixture(WidgetConfig.Type.THUMB, 2);
        var roomy = LayoutEngine.frame(c, 88, 88, 0);
        assertEquals(1, roomy.spacingScale(), .0001); assertEquals(1, roomy.iconScale(), 0);
        var tight = LayoutEngine.frame(c, 80, 80, 0);
        assertEquals(1, tight.iconScale(), 0); assertTrue(tight.spacingScale() < .01);
        assertEquals(40, tight.placements().get(0).size(), .001);
        var smaller = LayoutEngine.frame(c, 60, 60, 0);
        assertEquals(0, smaller.spacingScale(), 0); assertTrue(smaller.iconScale() < 1);
        assertEquals(30, smaller.placements().get(0).size(), .001);
    }
    @Test public void unusedLayersReserveNoSpaceAndSparseSelectionsKeepLargerIcons() {
        var sparse = fixture(WidgetConfig.Type.THUMB, 2);
        var first = LayoutEngine.frame(sparse, 160, 240, 0);
        sparse.setLayerCount(6);
        assertEquals(first.placements(), LayoutEngine.frame(sparse, 160, 240, 0).placements());
        assertEquals(40, first.placements().get(0).size(), 0);
        var full = LayoutEngine.frame(fixture(WidgetConfig.Type.THUMB, 23), 160, 240, 0);
        assertTrue(full.placements().get(0).size() < first.placements().get(0).size());
    }
    @Test public void resizingPreservesArcPatternAssignmentOrderAndLayerSizeRatios() {
        var c = fixture(WidgetConfig.Type.THUMB, 23); c.layerScales.set(1, 120);
        var a = LayoutEngine.frame(c, 160, 240, 0); var b = LayoutEngine.frame(c, 760, 700, 9);
        float ax0 = Float.MAX_VALUE, ay0 = Float.MAX_VALUE, ax1 = -1, ay1 = -1;
        float bx0 = Float.MAX_VALUE, by0 = Float.MAX_VALUE, bx1 = -1, by1 = -1;
        for (int i = 0; i < 23; i++) {
            var p = a.placements().get(i); var q = b.placements().get(i);
            ax0 = Math.min(ax0,p.x()); ax1 = Math.max(ax1,p.x()); ay0 = Math.min(ay0,p.y()); ay1 = Math.max(ay1,p.y());
            bx0 = Math.min(bx0,q.x()); bx1 = Math.max(bx1,q.x()); by0 = Math.min(by0,q.y()); by1 = Math.max(by1,q.y());
        }
        for (int i = 0; i < 23; i++) {
            var p = a.placements().get(i); var q = b.placements().get(i);
            assertEquals((p.x()-ax0)/(ax1-ax0), (q.x()-bx0)/(bx1-bx0), .00001);
            assertEquals((p.y()-ay0)/(ay1-ay0), (q.y()-by0)/(by1-by0), .00001);
            assertEquals(p.index(), q.index()); assertEquals(p.size()/a.iconScale(), q.size()/b.iconScale(), .0001);
        }
    }
    @Test public void handednessIsAnExactMirrorForEveryLayer() {
        var c = fixture(WidgetConfig.Type.THUMB, 23);
        for (boolean labels : new boolean[]{false,true}) {
            c.useDefaultLabels = false; c.labels = labels;
            var right = LayoutEngine.frame(c, 520, 600, 0);
            c.layout = WidgetConfig.Layout.LEFT_HAND;
            var left = LayoutEngine.frame(c, 520, 600, 0);
            for (int i = 0; i < 23; i++) {
                var r = right.placements().get(i); var l = left.placements().get(i);
                assertEquals(520, r.x() + l.x(), .0001); assertEquals(r.y(), l.y(), 0);
                assertEquals(r.size(), l.size(), 0); assertEquals(r.index(), l.index());
            }
            c.layout = WidgetConfig.Layout.RIGHT_HAND;
        }
    }
    @Test public void configurableLayersFitActualDimensionsWithoutOverlapping() {
        for (int layers = 1; layers <= WidgetConfig.MAX_LAYERS; layers++)
        for (int count : new int[]{1,2,5,9,16})
        for (boolean labels : new boolean[]{false,true})
        for (int padding : new int[]{0,16,32})
        for (int outer : new int[]{0,64})
        for (int[] dimensions : new int[][]{{40,40},{160,240},{500,180},{64,400}}) {
            var c = fixture(WidgetConfig.Type.THUMB, 400); c.useDefaultLabels = false; c.labels = labels; c.setLayerCount(layers);
            Collections.fill(c.layers, count); c.paddingX = c.paddingY = padding; c.outerPadding = outer;
            c.layerScales.set(0, 150); c.icons.get(0).size = 128;
            var f = LayoutEngine.frame(c, dimensions[0], dimensions[1], 0);
            assertEquals(layers * count, f.capacity()); inside(c, f);
            for (var p : f.placements()) {
                float tw = c.tileWidth(p.size()), th = c.tileHeight(p.size());
                assertTrue(p.size() > 0 && p.size() <= 48);
                for (var q : f.placements()) if (p.index() != q.index())
                    assertTrue(Math.abs(p.x() - q.x()) >= (tw + c.tileWidth(q.size())) / 2 - .002
                            || Math.abs(p.y() - q.y()) >= (th + c.tileHeight(q.size())) / 2 - .002);
            }
        }
    }
    @Test public void occupiedArtworkUsesTopAndOuterEdgesWithoutHiddenInsets() {
        for (int count : new int[]{1,2,3,7,12,23}) for (boolean labels : new boolean[]{false,true})
        for (int[] dimensions : new int[][]{{80,120},{160,240},{400,500}}) {
            var c = fixture(WidgetConfig.Type.THUMB, count); c.useDefaultLabels = false; c.labels = labels;
            var f = LayoutEngine.frame(c, dimensions[0], dimensions[1], 0);
            float left = Float.MAX_VALUE, top = Float.MAX_VALUE, right = 0, bottom = 0;
            for (var p : f.placements()) {
                left = Math.min(left,p.x()-c.tileWidth(p.size())/2); right = Math.max(right,p.x()+c.tileWidth(p.size())/2);
                top = Math.min(top,p.y()-c.tileHeight(p.size())/2); bottom = Math.max(bottom,p.y()+c.tileHeight(p.size())/2);
            }
            assertEquals(0,left,.001); assertEquals(0,top,.001);
            if (count > 1) { assertEquals(dimensions[0],right,.001); assertEquals(dimensions[1],bottom,.001); }
        }
    }
    @Test public void layerScaleChangesOnlyThatLayersArtworkAndPreservesArcOrder() {
        var c = fixture(WidgetConfig.Type.THUMB, 23);
        var before = LayoutEngine.frame(c, 400, 500, 0);
        c.layerScales.set(1, 120);
        var after = LayoutEngine.frame(c, 400, 500, 0);
        for (int i = 0; i < 23; i++) {
            assertEquals(before.placements().get(i).index(), after.placements().get(i).index());
            assertEquals(i >= 2 && i < 7 ? 48 : 40, after.placements().get(i).size(), .001);
        }
        c.icons.get(2).size = 36;
        assertEquals(36, LayoutEngine.frame(c, 400, 500, 0).placements().get(2).size(), 0);
    }
    @Test public void defaultLayersShareSymmetricAngularAnchors() {
        var c = fixture(WidgetConfig.Type.THUMB, 23);
        var f = LayoutEngine.frame(c, 400, 400, 0);
        float cornerX = 380, cornerY = 380;
        // Innermost 22.5° and 67.5° coincide with rays in the 5- and 9-icon rings.
        for (int[] group : new int[][]{{0,3,16},{1,5,20}}) {
            double previous = -1;
            for (int index : group) {
                var p = f.placements().get(index);
                double angle = Math.atan2(cornerY - p.y(), cornerX - p.x());
                if (previous >= 0) assertEquals(previous, angle, .0001);
                previous = angle;
            }
        }
    }
    @Test public void gripHeightTurnsFanAroundLowerMiddleAndUpperSide() {
        var c = fixture(WidgetConfig.Type.THUMB, 23);
        var bottom = LayoutEngine.frame(c,400,500,0);
        c.thumbCenterHeight = 50;
        var middle = LayoutEngine.frame(c,400,500,0);
        assertTrue(middle.placements().get(18).x() < bottom.placements().get(18).x());
        assertEquals(250,middle.placements().get(18).y(),.001);
        assertEquals(middle.placements().get(14).x(),middle.placements().get(22).x(),.001);
        c.thumbCenterHeight = 100;
        var top = LayoutEngine.frame(c,400,500,0);
        assertTrue(top.placements().get(14).x() > bottom.placements().get(14).x());
        assertTrue(top.placements().get(18).y() > bottom.placements().get(18).y());
        assertEquals(bottom.capacity(),top.capacity());
        for (int i=0;i<23;i++) assertEquals(i,top.placements().get(i).index());
        var single=fixture(WidgetConfig.Type.THUMB,1);single.thumbCenterHeight=50;
        assertEquals(250,LayoutEngine.frame(single,400,500,0).placements().get(0).y(),.001);
        single.thumbCenterHeight=100;
        assertEquals(480,LayoutEngine.frame(single,400,500,0).placements().get(0).y(),.001);
    }
    @Test public void layerSpacingActuallyReducesDistanceBetweenSharedRays() {
        var c = fixture(WidgetConfig.Type.THUMB,23);
        var regular = LayoutEngine.frame(c,600,600,0);
        c.thumbLayerSpacing = 50;
        var dense = LayoutEngine.frame(c,600,600,0);
        for (int[] pair : new int[][]{{0,3},{1,5},{3,16},{5,20}}) {
            var a=regular.placements().get(pair[0]); var b=regular.placements().get(pair[1]);
            var x=dense.placements().get(pair[0]); var y=dense.placements().get(pair[1]);
            assertTrue(Math.hypot(x.x()-y.x(),x.y()-y.y()) < Math.hypot(a.x()-b.x(),a.y()-b.y()));
        }
        inside(c,dense);
    }
    @Test public void sideEdgeDistanceIsVisibleAndMirrorsWithTheHand() {
        var c=fixture(WidgetConfig.Type.THUMB,23);
        var before=LayoutEngine.frame(c,400,500,0);
        c.thumbEdgeInset=24;
        var right=LayoutEngine.frame(c,400,500,0);
        float last=0;
        for(var p:right.placements()) last=Math.max(last,p.x()+c.tileWidth(p.size())/2);
        assertEquals(376,last,.001);
        assertTrue(right.placements().get(0).x()<before.placements().get(0).x());
        c.layout=WidgetConfig.Layout.LEFT_HAND;
        var left=LayoutEngine.frame(c,400,500,0); float first=Float.MAX_VALUE;
        for(int i=0;i<23;i++) {
            var a=right.placements().get(i);var b=left.placements().get(i);
            assertEquals(400,a.x()+b.x(),.001);assertEquals(a.y(),b.y(),0);
            first=Math.min(first,b.x()-c.tileWidth(b.size())/2);
        }
        assertEquals(24,first,.001);
    }
    @Test public void innerAndOuterReachChangeTheirOwnRadiiRatherThanRefillingTheWidget() {
        var c=fixture(WidgetConfig.Type.THUMB,23);c.thumbCustomReach=true;
        c.thumbInnerReach=25;c.thumbOuterReach=75;c.iconSize=12;c.paddingX=c.paddingY=0;
        var first=LayoutEngine.frame(c,400,400,0);assertEquals(1,first.iconScale(),0);
        float origin=394, reach=388;
        for(int i:new int[]{0,1}) {
            var p=first.placements().get(i);assertEquals(reach*.25,Math.hypot(origin-p.x(),origin-p.y()),.001);
        }
        for(int i=14;i<23;i++) {
            var p=first.placements().get(i);assertEquals(reach*.75,Math.hypot(origin-p.x(),origin-p.y()),.001);
        }
        c.thumbInnerReach=40;var inner=LayoutEngine.frame(c,400,400,0);assertEquals(1,inner.iconScale(),0);
        assertEquals(reach*.4,Math.hypot(origin-inner.placements().get(0).x(),origin-inner.placements().get(0).y()),.001);
        for(int i=14;i<23;i++)assertEquals(first.placements().get(i),inner.placements().get(i));
        c.thumbOuterReach=90;var outer=LayoutEngine.frame(c,400,400,0);assertEquals(1,outer.iconScale(),0);
        for(int i=0;i<2;i++)assertEquals(inner.placements().get(i),outer.placements().get(i));
        var p=outer.placements().get(22);assertEquals(reach*.9,Math.hypot(origin-p.x(),origin-p.y()),.001);
        c.thumbLayerSpacing=25;assertEquals(outer,LayoutEngine.frame(c,400,400,0));
    }
    @Test public void customReachFitsActualSizesPreservesAssignmentsAndMirrors() {
        for(int[] dimensions:new int[][]{{40,40},{160,240},{500,180},{64,400}})
        for(int center:new int[]{0,50,100}) for(int[] range:new int[][]{{1,2},{25,75},{90,100}})
        for(int count:new int[]{0,1,2,23,400}) for(boolean labels:new boolean[]{false,true}) {
            var c=fixture(WidgetConfig.Type.THUMB,count);c.thumbCustomReach=true;
            c.thumbInnerReach=range[0];c.thumbOuterReach=range[1];c.thumbCenterHeight=center;
            c.useDefaultLabels=false;c.labels=labels;c.thumbEdgeInset=24;
            var f=LayoutEngine.frame(c,dimensions[0],dimensions[1],0);
            assertEquals(Math.min(count,23),f.placements().size());inside(c,f);
            for(var a:f.placements()) for(var b:f.placements()) if(a.index()!=b.index())
                assertTrue(Math.abs(a.x()-b.x()) >= (c.tileWidth(a.size())+c.tileWidth(b.size()))/2-.002
                        || Math.abs(a.y()-b.y()) >= (c.tileHeight(a.size())+c.tileHeight(b.size()))/2-.002);
            c.layout=WidgetConfig.Layout.LEFT_HAND;var left=LayoutEngine.frame(c,dimensions[0],dimensions[1],0);
            for(int i=0;i<f.placements().size();i++) {
                var a=f.placements().get(i);var b=left.placements().get(i);
                assertEquals(i,a.index());assertEquals(dimensions[0],a.x()+b.x(),.001);
                assertEquals(a.y(),b.y(),0);assertEquals(a.size(),b.size(),0);
            }
        }
    }
    @Test public void customReachSupportsOneToSixLayersAndIndividualSizes() {
        for(int layers=1;layers<=6;layers++)for(int[] size:new int[][]{{40,40},{160,240},{500,180}}) {
            var c=fixture(WidgetConfig.Type.THUMB,96);c.setLayerCount(layers);
            for(int i=0;i<layers;i++)c.layers.set(i,16);
            c.thumbCustomReach=true;c.thumbInnerReach=20;c.thumbOuterReach=95;
            c.thumbCenterHeight=35;c.icons.get(0).size=20;c.layerScales.set(layers-1,120);
            var f=LayoutEngine.frame(c,size[0],size[1],0);inside(c,f);
            assertEquals(layers*16,f.placements().size());assertEquals(96,c.icons.size());
            assertEquals(20*f.iconScale(),f.placements().get(0).size(),.0001);
            for(var a:f.placements())for(var b:f.placements())if(a.index()!=b.index())
                assertTrue(Math.abs(a.x()-b.x()) >= (c.tileWidth(a.size())+c.tileWidth(b.size()))/2-.002
                        || Math.abs(a.y()-b.y()) >= (c.tileHeight(a.size())+c.tileHeight(b.size()))/2-.002);
        }
    }
    @Test public void customReachRetainsAnglesAndSizeRatiosWhenResized() {
        var c=fixture(WidgetConfig.Type.THUMB,23);c.thumbCustomReach=true;c.thumbCenterHeight=35;
        c.thumbInnerReach=30;c.thumbOuterReach=90;c.layerScales.set(0,80);c.layerScales.set(2,120);
        LayoutEngine.Frame previous=null;
        for(int[] size:new int[][]{{500,500},{160,240},{80,120},{500,180}}) {
            var frame=LayoutEngine.frame(c,size[0],size[1],0);inside(c,frame);
            float max=0;for(var p:frame.placements())max=Math.max(max,p.size());
            float originX=size[0]-max/2,originY=max/2+(size[1]-max)*.65f;
            if(previous!=null)for(int i=0;i<23;i++)
                assertEquals(previous.placements().get(i).size()/previous.placements().get(0).size(),
                        frame.placements().get(i).size()/frame.placements().get(0).size(),.0001);
            var first=frame.placements().get(0);
            assertEquals(Math.toRadians(22.5)-Math.atan2(35,65),Math.atan2(originY-first.y(),originX-first.x()),.0001);
            previous=frame;
        }
    }
    @Test public void allGripSettingsFitWithoutDroppingAppsOrOverlapping() {
        for(int[] dimensions:new int[][]{{40,40},{160,240},{500,180},{64,400}})
        for(int center:new int[]{0,25,50,75,100}) for(int density:new int[]{25,100,150})
        for(int inset:new int[]{0,96}) for(int count:new int[]{0,1,2,23,400})
        for(boolean labels:new boolean[]{false,true}) {
            var c=fixture(WidgetConfig.Type.THUMB,count);c.useDefaultLabels=false;c.labels=labels;
            c.thumbCenterHeight=center;c.thumbLayerSpacing=density;c.thumbEdgeInset=inset;
            var f=LayoutEngine.frame(c,dimensions[0],dimensions[1],0);
            assertEquals(Math.min(count,23),f.placements().size());inside(c,f);
            for(var a:f.placements()) for(var b:f.placements()) if(a.index()!=b.index())
                assertTrue(Math.abs(a.x()-b.x()) >= (c.tileWidth(a.size())+c.tileWidth(b.size()))/2-.002
                        || Math.abs(a.y()-b.y()) >= (c.tileHeight(a.size())+c.tileHeight(b.size()))/2-.002);
            c.layout=WidgetConfig.Layout.LEFT_HAND;
            var mirrored=LayoutEngine.frame(c,dimensions[0],dimensions[1],0);
            for(int i=0;i<f.placements().size();i++) {
                assertEquals(dimensions[0],f.placements().get(i).x()+mirrored.placements().get(i).x(),.001);
                assertEquals(f.placements().get(i).y(),mirrored.placements().get(i).y(),0);
            }
        }
    }

}
