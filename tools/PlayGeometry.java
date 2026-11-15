import dev.iconnavigator.*;
import java.util.Locale;

/** Export the starting thumb profile for vector artwork, without personal apps. */
public final class PlayGeometry {
    public static void main(String[] args) {
        WidgetConfig c=new WidgetConfig();c.selectType(WidgetConfig.Type.THUMB,true);
        boolean picker=args.length>0 && args[0].equals("--picker");
        for(int i=0;i<(picker?23:13);i++)c.addIcon(new WidgetConfig.IconEntry("example-"+i,"example"+i+"/.Main","App"));
        var frame=LayoutEngine.frame(c,picker?160:250,picker?240:300,0);
        System.out.print("[");
        for(var p:frame.placements()) {
            if(p.index()>0)System.out.print(",");
            System.out.printf(Locale.ROOT,"{\"x\":%.4f,\"y\":%.4f,\"size\":%.4f}",p.x(),p.y(),p.size());
        }
        System.out.println("]");
    }
}
