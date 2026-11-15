package dev.iconnavigator;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowInsets;
import android.widget.FrameLayout;

/** Debug-only, actual Android widget host for a dock-free screenshot. */
public final class PlayScreenshotHomeActivity extends Activity {
    private AppWidgetHost host;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setDecorFitsSystemWindows(false);
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);
        root.setOnApplyWindowInsetsListener((view,insets)->{
            var bars=insets.getInsets(WindowInsets.Type.systemBars());
            view.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;
        });
        setContentView(root);
        int id=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,0);
        host=new AppWidgetHost(this,84020);
        var info=AppWidgetManager.getInstance(this).getAppWidgetInfo(id);
        if(info==null){finish();return;}
        var widget=host.createView(this,id,info);widget.setPadding(0,0,0,0);
        widget.setContentDescription("Thumb icon selector");
        float density=getResources().getDisplayMetrics().density;
        root.addView(widget,new FrameLayout.LayoutParams(Math.round(250*density),Math.round(300*density),Gravity.RIGHT|Gravity.BOTTOM));
        host.startListening();
    }
    @Override public void onDestroy(){if(host!=null)host.stopListening();super.onDestroy();}
}
