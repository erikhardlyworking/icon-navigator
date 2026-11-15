import dev.iconnavigator.*;
import java.util.Locale;

/** Export production geometry, including count-dependent fitting and actual target bounds. */
public final class ThumbSimulation {
    public static void main(String[] args) {
        boolean grips=args.length>0 && args[0].equals("grips");
        System.out.println("scenario,width,height,labels,capacity,index,x,y,size,target_width,target_height,icon_scale,spacing_scale,apps,center_height,layer_spacing,edge_inset");
        for (int shape = 0; shape < 4; shape++) for (boolean left : new boolean[]{true, false}) {
            int width = grips?400:new int[]{80,120,160,400}[shape], height = grips?500:new int[]{120,160,240,500}[shape];
            int apps = grips?23:new int[]{2,7,23,23}[shape];
            WidgetConfig c = new WidgetConfig(); c.selectType(WidgetConfig.Type.THUMB, true);
            // These historical comparisons explicitly exercise Fit widget mode.
            c.thumbCustomReach=false;c.thumbCenterHeight=0;c.thumbLayerSpacing=100;c.iconSize=40;
            java.util.Collections.fill(c.layerScales,100);
            c.layout = left ? WidgetConfig.Layout.LEFT_HAND : WidgetConfig.Layout.RIGHT_HAND;
            c.useDefaultLabels = false; c.labels = !grips && shape == 3;
            if(grips) {
                c.thumbCenterHeight=shape==0?0:50;
                c.thumbLayerSpacing=shape>=2?50:100;
                c.thumbEdgeInset=shape==3?32:0;
            }
            for (int i = 0; i < apps; i++) c.addIcon(new WidgetConfig.IconEntry("i" + i,"app" + i + "/.Main","App " + i));
            var frame = LayoutEngine.frame(c,width,height,0);
            for (var p : frame.placements()) System.out.printf(Locale.ROOT,
                    "%s_%d,%d,%d,%s,%d,%d,%.3f,%.3f,%.3f,%.3f,%.3f,%.5f,%.5f,%d,%d,%d,%.1f%n",
                    left ? "left" : "right",shape,width,height,c.labels,frame.capacity(),p.index(),p.x(),p.y(),p.size(),c.tileWidth(p.size()),c.tileHeight(p.size()),frame.iconScale(),frame.spacingScale(),apps,c.thumbCenterHeight,c.thumbLayerSpacing,c.thumbEdgeInset);
        }
    }
}
