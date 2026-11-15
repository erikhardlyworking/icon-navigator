package dev.iconnavigator;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.PendingIntent;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/** Opt-in capture runner: not a JUnit test, never included in the ordinary test suite. */
public final class PlayScreenshotInstrumentation extends Instrumentation {
    private Bundle args;
    private Context context;
    private File output;
    private static final String[] APPS = {
        "com.android.dialer/.main.impl.MainActivity", "com.android.messaging/.ui.conversationlist.ConversationListActivity",
        "com.android.camera2/com.android.camera.CameraLauncher", "com.android.gallery3d/.app.GalleryActivity",
        "com.android.deskclock/.DeskClock", "com.android.calendar/.AllInOneActivity",
        "com.android.contacts/.activities.PeopleActivity", "com.android.documentsui/.LauncherActivity",
        "org.fossify.math/.activities.SplashActivity.Green", "org.fossify.notes/.activities.SplashActivity.Green",
        "org.fossify.musicplayer/.activities.SplashActivity.Green", "org.fossify.voicerecorder/.activities.SplashActivity.Green",
        "com.android.settings/.Settings", "com.android.quicksearchbox/.SearchActivity",
        "org.chromium.webview_shell/.WebViewBrowserActivity", "dev.iconnavigator/.EditorActivity"
    };
    private static final String[] LABELS = {
        "Phone", "Messages", "Camera", "Gallery", "Clock", "Calendar", "Contacts", "Files",
        "Calculator", "Notes", "Music", "Recorder", "Settings", "Search", "Browser", "Icon Navigator"
    };
    @Override public void onCreate(Bundle arguments) { args=arguments; super.onCreate(arguments); start(); }
    @Override public void onStart() {
        Bundle result=new Bundle();
        try {
            if (!"true".equals(args.getString("capturePlayScreenshots"))
                    || !(Build.PRODUCT.startsWith("sdk_") || Build.PRODUCT.startsWith("emu"))
                    || !"1".equals(shell("getprop ro.kernel.qemu").trim()))
                throw new IllegalStateException("Screenshot capture requires explicit opt-in and an Android emulator.");
            context=getTargetContext();output=new File(context.getExternalFilesDir(null),"play-screenshots");
            if (!output.exists() && !output.mkdirs()) throw new IllegalStateException("Cannot create capture output");
            var accessibility=getUiAutomation().getServiceInfo();
            accessibility.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
            getUiAutomation().setServiceInfo(accessibility);
            String stage=args.getString("stage","prepare");
            switch(stage) {
                case "prepare": prepare(); break;
                case "pin-grid": pin("grid12"); break;
                case "pin-thumb": pin("right"); break;
                case "grid9": home("grid","grid9", "01-grid-9.png"); break;
                case "grid12": home("grid","grid12", "02-grid-12.png"); break;
                case "grid16": home("grid","grid16", "03-grid-16.png"); break;
                case "home-right": minimalHome(); break;
                case "home-left": home("thumb","left", "05-thumb-left.png"); break;
                case "strip": home("grid","strip","06-grid-strip.png"); break;
                case "visual": editor("thumb","styled","colors","07-visual-options.png"); break;
                case "layers": editor("thumb","styled","layers","08-layer-sizes.png"); break;
                default: throw new IllegalArgumentException(stage);
            }
            result.putString("output",output.toString());result.putString("stage",stage);
            result.putString("stream","Capture stage completed: "+stage+"\n"); finish(Activity.RESULT_OK,result);
        } catch(Throwable e) {
            result.putString("stream",android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);
        }
    }
    private WidgetConfig config(String kind) throws Exception {
        WidgetConfig c=new WidgetConfig();
        boolean thumb=!kind.startsWith("grid")&&!kind.equals("strip");
        c.selectType(thumb?WidgetConfig.Type.THUMB:WidgetConfig.Type.GRID_COMPACT,true);
        c.useDefaultLabels=false;c.labels=false;c.iconSize=48;
        c.thumbCustomReach=false;java.util.Collections.fill(c.layerScales,100);
        c.layers.clear();c.layers.addAll(Arrays.asList(2,4,6));
        c.thumbCenterHeight=0;c.thumbLayerSpacing=100;c.thumbEdgeInset=0;
        if(kind.equals("left")) c.layout=WidgetConfig.Layout.LEFT_HAND;
        if(kind.equals("strip")) {c.columns=8;c.rows=1;}
        int count=kind.equals("strip")?8:12;
        if(kind.startsWith("grid")) {
            count=Integer.parseInt(kind.substring(4));c.columns=count==16?4:3;c.rows=count==9?3:4;
        }
        if(kind.equals("grid9")) {c.labels=true;c.paddingY=2;}
        if(kind.equals("grid12")) {Themes.apply(c,3);c.outerPadding=3;c.cornerRadius=20;}
        if(kind.equals("right")) {
            c=new WidgetConfig();c.selectType(WidgetConfig.Type.THUMB,true);
            c.useDefaultLabels=false;c.labels=false;count=13;
        }
        if(kind.equals("left")) {
            Themes.apply(c,5);c.effect=WidgetConfig.Effect.GRAYSCALE;c.iconSize=34;
            c.widgetBorder=1;c.widgetBorderColor=0xff6e8877;c.cornerRadius=20;
        }
        if(kind.equals("strip")) {
            Themes.apply(c,4);c.opacity=95;c.iconOpacity=80;c.iconSize=36;
            c.widgetBorder=1;c.widgetBorderColor=0xff5d6c69;c.cornerRadius=24;
        }
        if(kind.equals("styled")) {
            Themes.apply(c,6);c.artOpacity=95;c.cornerRadius=20;c.outerPadding=4;c.iconSize=30;
            c.thumbCenterHeight=35;c.thumbLayerSpacing=70;
            c.thumbEdgeInset=8;
            c.layerScales.set(0,80);c.layerScales.set(2,115);
        }
        Set<String> seen=new HashSet<>();
        for(int i=0;i<count;i++) {
            ComponentName component=ComponentName.unflattenFromString(APPS[i]);
            var info=context.getPackageManager().getActivityInfo(component,0);
            if(!seen.add(info.packageName)) throw new IllegalStateException("Duplicate package");
            var entry=new WidgetConfig.IconEntry("play-stock-"+i,component.flattenToString(),LABELS[i]);
            if(!entry.image.isEmpty()) throw new IllegalStateException("Imported artwork is forbidden");
            c.addIcon(entry);
        }
        c.normalize();return c;
    }
    private void prepare() throws Exception {
        new ConfigStore(context).defaultLabels(false);
        new ConfigStore(context).save(0,config("grid12"));
        JSONArray apps=new JSONArray();
        for(int i=0;i<APPS.length;i++) apps.put(new JSONObject().put("component",APPS[i]).put("label",LABELS[i]).put("artwork","PackageManager original icon"));
        write("icon-provenance.json",new JSONObject().put("source","Isolated AOSP Android 15 emulator; no phone data or imported images")
                .put("appVersion",context.getPackageManager().getPackageInfo(context.getPackageName(),0).versionName).put("apps",apps).toString(2));
        Bitmap wallpaper=Bitmap.createBitmap(1080,1920,Bitmap.Config.ARGB_8888);
        Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new LinearGradient(0,0,1080,1920,Color.rgb(15,28,32),Color.rgb(42,67,53),Shader.TileMode.CLAMP));
        new Canvas(wallpaper).drawRect(0,0,1080,1920,paint);
        getUiAutomation().adoptShellPermissionIdentity("android.permission.SET_WALLPAPER");
        try {WallpaperManager.getInstance(context).setBitmap(wallpaper,null,false,WallpaperManager.FLAG_SYSTEM);}
        finally {getUiAutomation().dropShellPermissionIdentity();wallpaper.recycle();}
    }
    private void pin(String kind) throws Exception {
        var store=new ConfigStore(context);WidgetConfig c=config(kind);store.save(0,c);
        String family=c.thumb()?"thumb":"grid";
        int existing=context.getSharedPreferences("play_capture",0).getInt(family,0);
        if(NavigatorWidget.owns(context,existing)) {
            store.save(existing,c);NavigatorWidget.update(context,existing);
            write("widget-ids.json",new JSONObject(context.getSharedPreferences("play_capture",0).getAll()).toString(2));
            return;
        }
        Activity activity=startActivitySync(new Intent(context,EditorActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        waitForIdleSync();SystemClock.sleep(300);
        Set<Integer> old=new HashSet<>();for(int id:WidgetProviders.ids(context)) old.add(id);
        boolean[] requested={false};
        runOnMainSync(() -> {
            Intent callback=new Intent(context,PinReceiver.class).setData(Uri.parse("iconnavigator://play-capture/"+System.nanoTime()))
                    .putExtra("snapshot",ConfigStore.encode(c));
            PendingIntent pending=PendingIntent.getBroadcast(context,0,callback,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_MUTABLE);
            requested[0]=AppWidgetManager.getInstance(context).requestPinAppWidget(WidgetProviders.component(context,c.type),null,pending);
        });
        if(!requested[0]) throw new IllegalStateException("Launcher rejected pin request");
        long deadline=SystemClock.uptimeMillis()+8000;boolean clicked=false;
        while(SystemClock.uptimeMillis()<deadline&&!clicked) {
            var root=getUiAutomation().getRootInActiveWindow();
            if(root!=null) clicked=clickAdd(root);
            if(!clicked) SystemClock.sleep(200);
        }
        if(!clicked) {capture("debug-pin.png");throw new IllegalStateException("Cannot find launcher Add button");}
        int id=0;deadline=SystemClock.uptimeMillis()+8000;
        while(SystemClock.uptimeMillis()<deadline&&id==0) {
            for(int candidate:WidgetProviders.ids(context)) if(!old.contains(candidate)) id=candidate;
            if(id==0)SystemClock.sleep(100);
        }
        if(id==0) throw new IllegalStateException("Widget pin callback not received");
        store.save(id,c);NavigatorWidget.update(context,id);
        context.getSharedPreferences("play_capture",0).edit().putInt(family,id).commit();
        write("widget-ids.json",new JSONObject(context.getSharedPreferences("play_capture",0).getAll()).toString(2));
        SystemClock.sleep(500);runOnMainSync(activity::finish);
    }
    private boolean clickAdd(AccessibilityNodeInfo node) {
        String value=node.getText()==null?"":node.getText().toString().toLowerCase(java.util.Locale.ROOT);
        if(node.isClickable()&&(value.equals("add")||value.equals("add automatically")||value.equals("add to home screen")))
            if(node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
        for(int i=0;i<node.getChildCount();i++) {var child=node.getChild(i);if(child!=null&&clickAdd(child))return true;}
        return false;
    }
    private int id(String family) {
        int id=context.getSharedPreferences("play_capture",0).getInt(family,0);
        if(!NavigatorWidget.owns(context,id)) throw new IllegalStateException("Missing real launcher widget: "+family);
        return id;
    }
    private void minimalHome() throws Exception {
        // A real AppWidgetHost without a reserved launcher dock matches free
        // widget placement on launchers that let users remove their dock.
        var host=new android.appwidget.AppWidgetHost(context,84020);
        int id=host.allocateAppWidgetId();Activity activity=null;
        try {
            getUiAutomation().adoptShellPermissionIdentity("android.permission.BIND_APPWIDGET");
            boolean bound=AppWidgetManager.getInstance(context).bindAppWidgetIdIfAllowed(id,WidgetProviders.component(context,WidgetConfig.Type.THUMB));
            getUiAutomation().dropShellPermissionIdentity();
            if(!bound)throw new IllegalStateException("Cannot bind the screenshot host");
            var c=config("right");new ConfigStore(context).save(id,c);
            Bundle options=new Bundle();options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,250);
            options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,300);
            AppWidgetManager.getInstance(context).updateAppWidgetOptions(id,options);NavigatorWidget.update(context,id);
            activity=startActivitySync(new Intent(context,PlayScreenshotHomeActivity.class)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitForIdleSync();SystemClock.sleep(800);
            var widget=findHost(getUiAutomation().getRootInActiveWindow(),"Thumb icon selector");
            if(widget==null||iconCount(widget)!=13||hasLauncherShortcut(getUiAutomation().getRootInActiveWindow()))
                throw new IllegalStateException("Expected only the 13-app real thumb widget");
            capture("04-thumb-right.png");
        } finally {
            getUiAutomation().dropShellPermissionIdentity();
            if(activity!=null){Activity closing=activity;runOnMainSync(closing::finish);}
            host.deleteAppWidgetId(id);new ConfigStore(context).delete(id);
        }
    }
    private void home(String family,String kind,String file) throws Exception {
        int id=id(family);WidgetConfig c=config(kind);new ConfigStore(context).save(id,c);NavigatorWidget.update(context,id);
        SystemClock.sleep(1200);
        String label=family.equals("grid")?"Grid":"Thumb icon selector";
        AccessibilityNodeInfo host=findHost(getUiAutomation().getRootInActiveWindow(),label);
        if(host==null)
            throw new IllegalStateException("Expected the real launcher widget to be visible: "+label);
        if(iconCount(host)!=c.icons.size())throw new IllegalStateException("Wrong visible app count for "+kind);
        Rect bounds=new Rect();host.getBoundsInScreen(bounds);
        if(bounds.top<1920*.4||bounds.bottom<1920*.7||bounds.height()>1920*.35
                ||bounds.width()>1080*(kind.equals("strip")?1:family.equals("grid")?.5:.65))
            throw new IllegalStateException("Widget must occupy a compact lower part of the home screen: "+bounds);
        if(kind.equals("right")&&(c.opacity!=0||c.backgroundArt!=WidgetConfig.BackgroundArt.NONE
                ||c.widgetBorder!=0||hasLauncherShortcut(getUiAutomation().getRootInActiveWindow()))) {
            capture("debug-clean-home.png");
            throw new IllegalStateException("Image 4 requires a transparent widget with no ordinary launcher shortcuts or dock icons");
        }
        capture(file);
    }
    private boolean hasLauncherShortcut(AccessibilityNodeInfo node) {
        if(node==null)return false;
        Rect bounds=new Rect();node.getBoundsInScreen(bounds);
        if(node.isVisibleToUser()&&bounds.intersect(0,0,1080,1920)
                &&"com.android.launcher3".contentEquals(node.getPackageName()==null?"":node.getPackageName())
                &&"android.widget.TextView".contentEquals(node.getClassName()==null?"":node.getClassName()))return true;
        for(int i=0;i<node.getChildCount();i++)if(hasLauncherShortcut(node.getChild(i)))return true;
        return false;
    }
    private AccessibilityNodeInfo findHost(AccessibilityNodeInfo node,String label) {
        if(node==null)return null;
        if(node.getClassName()!=null&&node.getClassName().toString().contains("AppWidgetHostView")
                &&label.contentEquals(node.getContentDescription()==null?"":node.getContentDescription()))return node;
        for(int i=0;i<node.getChildCount();i++){var found=findHost(node.getChild(i),label);if(found!=null)return found;}
        return null;
    }
    private int iconCount(AccessibilityNodeInfo node) {
        int count="dev.iconnavigator:id/icon".equals(node.getViewIdResourceName())?1:0;
        for(int i=0;i<node.getChildCount();i++){var child=node.getChild(i);if(child!=null)count+=iconCount(child);}
        return count;
    }
    private void editor(String family,String kind,String view,String file) throws Exception {
        int id=id(family);WidgetConfig c=config(kind);new ConfigStore(context).save(id,c);NavigatorWidget.update(context,id);
        Activity activity=startActivitySync(new Intent(context,EditorActivity.class).setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        waitForIdleSync();SystemClock.sleep(500);
        if(view!=null)runOnMainSync(() -> {
            View root=activity.getWindow().getDecorView();
            View tab=text(root,view.equals("layers")||view.equals("colors")?"Visual options":view);
            if(tab==null)throw new IllegalStateException("Missing tab");tab.performClick();
        });
        waitForIdleSync();SystemClock.sleep(350);
        if("layers".equals(view)) {
            runOnMainSync(() -> {
                View root=activity.getWindow().getDecorView();View heading=text(root,"Layer 1 icon scale (%)  ·  80");
                if(heading==null)throw new IllegalStateException("Missing capture control: "+view);
                ScrollView scroll=kind(root,ScrollView.class);int[] h=new int[2],s=new int[2];
                heading.getLocationInWindow(h);scroll.getLocationInWindow(s);scroll.scrollBy(0,h[1]-s[1]-12);
            });
            waitForIdleSync();SystemClock.sleep(250);
        }
        if("colors".equals(view)) {
            runOnMainSync(() -> {
                View caption=text(activity.getWindow().getDecorView(),"Icon colors");
                if(caption==null)throw new IllegalStateException("Missing icon colors control");
                ViewGroup parent=(ViewGroup)caption.getParent();
                View choices=parent.getChildAt(parent.indexOfChild(caption)+1);
                if(!(choices instanceof Spinner))throw new IllegalStateException("Expected icon color choices");
                choices.performClick();
            });
            waitForIdleSync();SystemClock.sleep(350);
        }
        capture(file);runOnMainSync(activity::finish);waitForIdleSync();
    }
    private View text(View root,String label) {
        if(root instanceof TextView t&&label.equals(t.getText().toString()))return root;
        if(root instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){View found=text(group.getChildAt(i),label);if(found!=null)return found;}
        return null;
    }
    private <T extends View>T kind(View root,Class<T> type) {
        if(type.isInstance(root))return type.cast(root);
        if(root instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){T found=kind(group.getChildAt(i),type);if(found!=null)return found;}
        return null;
    }
    private void capture(String name) throws Exception {
        waitForIdleSync();Bitmap screen=getUiAutomation().takeScreenshot();
        if(screen==null||screen.getWidth()!=1080||screen.getHeight()!=1920)throw new IllegalStateException("Expected a 1080 × 1920 capture");
        // Lossless format conversion only: preserve captured UI pixels, omit the opaque alpha channel.
        Bitmap rgb=Bitmap.createBitmap(screen.getWidth(),screen.getHeight(),Bitmap.Config.ARGB_8888);rgb.setHasAlpha(false);
        new Canvas(rgb).drawBitmap(screen,0,0,null);
        try(var stream=new FileOutputStream(new File(output,name))){rgb.compress(Bitmap.CompressFormat.PNG,100,stream);}
        screen.recycle();rgb.recycle();
    }
    private void write(String name,String value)throws Exception {
        try(var stream=new FileOutputStream(new File(output,name))){stream.write(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    }
    private String shell(String command)throws Exception {
        try(var stream=new android.os.ParcelFileDescriptor.AutoCloseInputStream(getUiAutomation().executeShellCommand(command))) {
            return new String(stream.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
