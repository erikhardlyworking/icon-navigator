package dev.iconnavigator;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.*;
import java.io.File;
import java.io.FileOutputStream;
import java.util.*;
import java.util.function.IntConsumer;

public final class EditorActivity extends Activity {
    private static final int BG = 0xff101310, CARD = 0xff20261f, TEXT = 0xffedf3e9, MUTED = 0xffa2ae9c, ACCENT = 0xffb9f97b;
    private WidgetConfig config;
    private ConfigStore store;
    private SetupFlow flow = new SetupFlow();
    private int widgetId;
    private boolean configuring;
    private final int[] scrollPositions = new int[3];
    private String saved, imageEntry;
    private PreviewView preview;
    private float previewPage;
    private LinearLayout panel, previewContainer;
    private ScrollView scroll;
    private TextView progress, summary, warning;
    private Button next;
    private final List<Button> stepButtons = new ArrayList<>();
    private List<ResolveInfo> apps = new ArrayList<>();
    private boolean appsLoaded;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setDecorFitsSystemWindows(false);
        store = new ConfigStore(this);
        widgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0);
        if (state != null) widgetId = state.getInt("widgetId", widgetId);
        configuring = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE.equals(getIntent().getAction());
        if (widgetId != 0 && !NavigatorWidget.owns(this, widgetId)) { finish(); return; }
        setResult(RESULT_CANCELED);
        config = widgetId != 0 && !store.exists(widgetId) ? store.load(0) : store.load(widgetId);
        if (widgetId != 0) config.selectType(WidgetProviders.type(this, widgetId), !store.exists(widgetId));
        saved = ConfigStore.encode(config);
        if (state != null) {
            config = ConfigStore.decode(state.getString("draft", saved));
            saved = state.getString("saved", saved); imageEntry = state.getString("imageEntry");
            flow.step = WidgetConfig.clamp(state.getInt("step"), 0, 2);
            previewPage = state.getFloat("page");
            int[] positions = state.getIntArray("scrollPositions");
            if (positions != null && positions.length == 3) System.arraycopy(positions, 0, scrollPositions, 0, 3);
        }
        config.defaultLabels = store.defaultLabels();
        if(android.os.Build.VERSION.SDK_INT>=33)
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,this::confirmClose);
        build();
        new Thread(() -> {
            Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> found = getPackageManager().queryIntentActivities(query, 0);
            found.sort(Comparator.comparing(a -> a.loadLabel(getPackageManager()).toString(), String.CASE_INSENSITIVE_ORDER));
            Set<String> packages = new HashSet<>();
            found.removeIf(app -> !packages.add(app.activityInfo.packageName));
            runOnUiThread(() -> { if (!isDestroyed()) { apps = found; appsLoaded = true; } });
            for (int id : WidgetProviders.ids(this))
                NavigatorWidget.update(getApplicationContext(), id);
        }, "app-picker").start();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putInt("widgetId", widgetId); state.putString("draft", ConfigStore.encode(config));
        state.putString("saved", saved); state.putString("imageEntry", imageEntry);
        state.putInt("step", flow.step);
        state.putFloat("page", preview == null ? previewPage : preview.page);
        scrollPositions[flow.step] = scroll.getScrollY(); state.putIntArray("scrollPositions", scrollPositions);
        super.onSaveInstanceState(state);
    }
    private int dp(float n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private LinearLayout column() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private LinearLayout row() { LinearLayout v = new LinearLayout(this); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    private TextView text(String value, int size, int color) {
        TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color);
        v.setPadding(0, dp(6), 0, dp(6)); return v;
    }
    private Button button(String title, Runnable action) {
        Button b = new Button(this); b.setText(title); b.setAllCaps(false); b.setTextColor(TEXT);
        b.setTextSize(13); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff303a2c));
        b.setMinHeight(dp(48)); b.setOnClickListener(v -> action.run()); return b;
    }
    private void weighted(LinearLayout parent, View child) {
        parent.addView(child, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
    }
    private void card(View view) {
        GradientDrawable d = new GradientDrawable(); d.setColor(CARD); d.setCornerRadius(dp(16)); view.setBackground(d);
    }
    private void build() {
        LinearLayout root = column(); root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            var bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
        });
        LinearLayout header = row(); header.setPadding(dp(20), dp(6), dp(16), 0);
        weighted(header, text("ICON NAVIGATOR", 13, ACCENT));
        if (!configuring) header.addView(button(widgetId == 0 ? "New widget ▾" : "Widget " + widgetId + " ▾", this::chooseWidget));
        root.addView(header);
        LinearLayout steps = row(); steps.setPadding(dp(12), 0, dp(12), 0); stepButtons.clear();
        for (int i = 0; i < 3; i++) {
            final int target = i;
            Button b = button(new String[]{"Basic settings", "Select apps", "Visual options"}[i], () -> {
                if (flow.canVisit(target)) { scrollPositions[flow.step] = scroll.getScrollY(); flow.step = target; showSheet(); scroll.post(() -> scroll.scrollTo(0, scrollPositions[target])); }
            });
            stepButtons.add(b); weighted(steps, b);
        }
        root.addView(steps);
        progress = text("", 12, MUTED); progress.setPadding(dp(20), dp(4), dp(20), dp(8)); root.addView(progress);
        previewContainer = column(); previewContainer.setPadding(dp(20), 0, dp(20), dp(8)); root.addView(previewContainer);
        warning = text("", 12, 0xffffc785); warning.setPadding(dp(20), 0, dp(20), dp(6));
        warning.setContentDescription("Layout warning"); root.addView(warning);
        scroll = new ScrollView(this); scroll.setFillViewport(true);
        panel = column(); panel.setPadding(dp(20), 0, dp(20), dp(20)); scroll.addView(panel);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout footer = row(); footer.setPadding(dp(12), dp(6), dp(12), dp(6));
        next = button("Save widget", this::save);
        next.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ACCENT)); next.setTextColor(BG);
        weighted(footer, next); root.addView(footer);
        setContentView(root); root.requestApplyInsets(); showSheet(); scroll.post(() -> scroll.scrollTo(0, scrollPositions[flow.step]));
    }
    private void showSheet() {
        if (preview != null) previewPage = preview.page;
        preview = null; summary = null; panel.removeAllViews(); previewContainer.removeAllViews();
        addPreview(previewContainer, flow.step == 1);
        switch (flow.step) {
            case 0: basicSheet(); break;
            case 1: appsSheet(); break;
            case 2: appearanceSheet(); break;
        }
        updateNavigation();
    }
    private void updateNavigation() {
        progress.setText(config.title() + " · " + config.icons.size() + " apps · " + layoutSummary());
        for (int i = 0; i < stepButtons.size(); i++) {
            Button b = stepButtons.get(i); b.setEnabled(true);
            b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(i == flow.step ? ACCENT : 0xff303a2c));
            b.setTextColor(i == flow.step ? BG : TEXT);
        }
        String problem = validationWarning(); warning.setText(problem);
        warning.setVisibility(problem.isEmpty() ? View.GONE : View.VISIBLE);
        next.setEnabled(canSave()); next.setAlpha(canSave() ? 1 : .4f);
    }
    private float[] widgetSize() {
        float width = config.type.width, height = config.type.height;
        if (widgetId != 0) {
            Bundle options = AppWidgetManager.getInstance(this).getAppWidgetOptions(widgetId);
            width = Math.max(1, options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, (int) width));
            height = Math.max(1, options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, (int) height));
        }
        return new float[]{width, height};
    }
    private String layoutSummary() {
        float[] size = widgetSize(); var frame = LayoutEngine.frame(config, size[0], size[1], Math.round(previewPage));
        return frame.capacity() + " icon positions";
    }
    private String validationWarning() {
        String message = config.icons.size() > config.capacity()
                ? config.icons.size() + " apps selected, but only " + config.capacity() + " positions. Increase the layout capacity or remove "
                    + (config.icons.size() - config.capacity()) + " apps before saving." : "";
        if (config.thumb() && !config.icons.isEmpty()) {
            float[] size = widgetSize(); var frame = LayoutEngine.frame(config, size[0], size[1], 0);
            if (frame.iconScale() < .999f) {
                float smallest = Float.MAX_VALUE;
                for (var p : frame.placements()) smallest = Math.min(smallest, p.size());
                message += (message.isEmpty() ? "" : "\n") + "Icons shrink to fit this size (smallest " + Math.round(smallest)
                        + " dp). Enlarge the widget for larger icons and tap areas.";
            }
            if (config.showLabels() && frame.placements().stream().anyMatch(p -> !config.labelsAt(p.size())))
                message += (message.isEmpty() ? "" : "\n") + "App names are hidden for small icons.";
        }
        return message;
    }
    private boolean canSave() { return flow.canSave(config); }
    private void refresh(boolean rebuild) {
        config.normalize();
        if (preview != null) {
            preview.page = 0; preview.refresh();
            previewPage = preview.page;
        }
        if (summary != null) summary.setText(appSummary());
        updateNavigation();
        if (rebuild) {
            int y = scroll.getScrollY(); showSheet(); scroll.post(() -> scroll.scrollTo(0, y));
        }
    }
    private String appSummary() {
        return config.icons.isEmpty() ? "Add an app to enable Save. You can visit any view at any time."
                : "Tap to edit; hold and drag an icon in the preview to reorder. Each app can appear once.";
    }
    private void basicSheet() {
        panel.addView(text("Basic settings", 25, TEXT));
        if (widgetId == 0) spinner(panel, "Widget type", Arrays.stream(WidgetConfig.offeredTypes()).map(t -> t.title).toArray(String[]::new),
                config.thumb() ? 1 : 0, v -> { config.selectType(WidgetConfig.offeredTypes()[v], true); previewPage = 0; if (preview != null) preview.page = 0; refresh(true); });
        spinner(panel, "Show icon labels", new String[]{"Follow defaults", "Yes", "No"},
                config.useDefaultLabels ? 0 : config.labels ? 1 : 2,
                v -> { config.useDefaultLabels = v == 0; if (v == 0) config.defaultLabels = store.defaultLabels(); else config.labels = v == 1; refresh(false); });
        if (!config.thumb()) {
            slider(panel, "Columns", 1, 12, config.columns, v -> config.columns = v);
            slider(panel, "Rows", 1, 12, config.rows, v -> config.rows = v);
        } else {
            spinner(panel, "Thumb side", new String[]{"Right", "Left"}, config.layout == WidgetConfig.Layout.LEFT_HAND ? 1 : 0,
                    v -> { config.layout = v == 1 ? WidgetConfig.Layout.LEFT_HAND : WidgetConfig.Layout.RIGHT_HAND; refresh(true); });
            slider(panel, "Thumb center height (%)", 0, 100, config.thumbCenterHeight, v -> config.thumbCenterHeight = v);
            spinner(panel, "Thumb reach", new String[]{"Fit widget", "Set inner and outer reach"}, config.thumbCustomReach ? 1 : 0,
                    v -> {
                        config.thumbCustomReach = v == 1; refresh(true);
                        if (config.thumbCustomReach) scroll.post(() -> {
                            var controls = new ArrayList<View>();
                            panel.findViewsWithText(controls,"Outer reach (%)",View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
                            if (!controls.isEmpty()) {
                                View group=(View)controls.get(0).getParent().getParent();
                                revealAfterLayout(group);
                            }
                        });
                    });
            if (config.thumbCustomReach) {
                LinearLayout group=row(), inner=column(), outer=column();
                inner.setPadding(0,0,dp(6),0);outer.setPadding(dp(6),0,0,0);
                weighted(group,inner);weighted(group,outer);panel.addView(group);
                SeekBar[] reach = new SeekBar[2];
                reach[0] = slider(inner, "Inner reach (%)", 1, config.thumbOuterReach - 1, config.thumbInnerReach,
                        v -> { config.thumbInnerReach = v; reach[1].setMin(v + 1); });
                reach[1] = slider(outer, "Outer reach (%)", config.thumbInnerReach + 1, 100, config.thumbOuterReach,
                        v -> { config.thumbOuterReach = v; reach[0].setMax(v - 1); });
            } else slider(panel, "Layer spacing (%)", 25, 150, config.thumbLayerSpacing, v -> config.thumbLayerSpacing = v);
            spinner(panel, "Number of layers", new String[]{"1", "2", "3", "4", "5", "6"}, config.layers.size() - 1,
                    v -> { config.setLayerCount(v + 1); refresh(true); });
            for (int i = 0; i < config.layers.size(); i++) {
                final int layer = i;
                slider(panel, "Icons in layer " + (i + 1) + (i == 0 ? " (nearest thumb)" : ""), 1, WidgetConfig.MAX_LAYER_ICONS,
                        config.layers.get(i), v -> config.layers.set(layer, v));
            }
        }
    }
    private void appsSheet() {
        panel.addView(text("Select apps", 25, TEXT));
        summary = text(appSummary(), 12, MUTED); panel.addView(summary);
        panel.addView(button("+ Add apps", () -> pickApps(null)));
        appRows();
    }
    private void revealAfterLayout(View control) {
        if(control.isLaidOut()&&control.getHeight()>0) {
            control.requestRectangleOnScreen(new android.graphics.Rect(0,0,control.getWidth(),control.getHeight()),false);
        } else control.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() {
                control.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                control.requestRectangleOnScreen(new android.graphics.Rect(0,0,control.getWidth(),control.getHeight()),false);
            }
        });
    }
    private void addPreview(LinearLayout parent, boolean editable) {
        preview = new PreviewView(this, config); preview.page = previewPage; preview.editing = editable;
        if (editable) preview.listener = new PreviewView.Listener() {
            public void changed() { preview.post(() -> refresh(true)); }
            public void edit(int index) { editIcon(index); }
        };
        FrameLayout frame = new FrameLayout(this); card(frame); frame.setClipToOutline(true);
        float[] size = widgetSize(); float width = size[0], height = size[1];
        final float logicalWidth = width, logicalHeight = height;
        PreviewView current = preview;
        frame.addView(current, new FrameLayout.LayoutParams(dp(width), dp(height), Gravity.CENTER));
        parent.addView(frame, new LinearLayout.LayoutParams(-1, dp(Math.min(height, Math.max(120, Math.min(220, getResources().getDisplayMetrics().heightPixels / getResources().getDisplayMetrics().density * .28f))))));
        frame.post(() -> {
            float scale = Math.min(1, Math.min(frame.getWidth() / (float) dp(logicalWidth), frame.getHeight() / (float) dp(logicalHeight)));
            current.setScaleX(scale); current.setScaleY(scale);
        });
    }
    private void appearanceSheet() {
        panel.addView(text("Visual options", 25, TEXT));
        spinner(panel, "Appearance preset", Themes.NAMES, 0, this::applyAppearance);
        panel.addView(text("Presets are starting points. Customize the options below; your apps and widget type stay the same.", 12, MUTED));
        spinner(panel, "Icon colors", new String[]{"Original colors", "Grayscale only", "Single-color tint"},
                config.effect.ordinal(), v -> { config.effect = WidgetConfig.Effect.values()[v]; refresh(true); });
        if (config.effect == WidgetConfig.Effect.TINT)
            panel.addView(button("Tint color", () -> colorDialog("Icon tint", config.tint, v -> { config.tint = v; refresh(false); })));
        if (config.thumb()) {
            panel.addView(text("Layer icon sizes", 19, TEXT));
            for (int i = 0; i < config.layers.size(); i++) {
                final int layer = i;
                slider(panel, "Layer " + (i + 1) + " icon scale (%)", 50, 150, config.layerScales.get(i), v -> config.layerScales.set(layer, v));
            }
            panel.addView(text("Scales each layer's default icon size independently. Individual sizes take priority; all icons shrink together only when space requires it.", 12, MUTED));
        }
        expandable("Background & overlays", this::backgroundOptions);
        expandable("Advanced settings", form -> {
            form.addView(text("Icon sizing and spacing", 19, TEXT));
            slider(form, "Default icon size (dp)", 12, config.thumb() ? 48 : 96,
                    (int) Math.max(12, Math.min(config.thumb() ? 48 : 96, config.iconSize)), v -> config.iconSize = v);
            if (config.thumb()) {
                slider(form, "Exaggeration (%)", 0, 200, config.exaggeration, v -> config.exaggeration = v);
                form.addView(text("Adds subtle size differences between layers. Icons shrink automatically when the widget is crowded.", 12, MUTED));
            }
            slider(form, "Icon opacity (%)", 20, 100, config.iconOpacity, v -> config.iconOpacity = v);
            slider(form, "Padding beside each icon (dp)", 0, 32, (int) config.paddingX, v -> config.paddingX = v);
            slider(form, "Padding above / below (dp)", 0, 32, (int) config.paddingY, v -> config.paddingY = v);
            slider(form, "Icon border width (dp)", 0, 8, (int) config.iconBorder, v -> config.iconBorder = v);
            form.addView(button("Icon border color", () -> colorDialog("Icon border color", config.iconBorderColor, v -> { config.iconBorderColor = v; refresh(false); })));
            form.addView(button("Custom artwork for an icon…", this::chooseArtworkEntry));
            form.addView(text("Text readability", 19, TEXT));
            toggle(form, "Automatic text color", config.automaticTextColor, v -> config.automaticTextColor = v);
            form.addView(button("Custom text color", () -> colorDialog("Label color", config.textColor, v -> { config.textColor = v; refresh(false); })));
            form.addView(text("Turn off Automatic text color to use your custom color for app names.", 12, MUTED));
            toggle(form, "Shadow behind app names", config.labelShadow, v -> config.labelShadow = v);
            form.addView(text("Widget border and padding", 19, TEXT));
            slider(form, "Padding around the widget (dp)", 0, 64, (int) config.outerPadding, v -> config.outerPadding = v);
            slider(form, "Widget border width (dp)", 0, 12, (int) config.widgetBorder, v -> config.widgetBorder = v);
            form.addView(button("Widget border color", () -> colorDialog("Widget border color", config.widgetBorderColor, v -> { config.widgetBorderColor = v; refresh(false); })));
            slider(form, "Corner radius (dp)", 0, 48, (int) config.cornerRadius, v -> config.cornerRadius = v);
        });
        panel.addView(button("Privacy & support", () -> startActivity(new Intent(this,PrivacyActivity.class))));
    }
    private void backgroundOptions(LinearLayout form) {
        form.removeAllViews();
        form.addView(button("Background / gradient start color", () -> colorDialog("Background color", config.background, v -> { config.background = v; refresh(false); })));
        slider(form, "Background opacity (%)", 0, 100, config.opacity, v -> config.opacity = v);
        spinner(form, "Overlay", new String[]{"None", "Soft icon color wash", "Faint icon collage", "Color gradient", "Image overlay"},
                config.backgroundArt.ordinal(), v -> { config.backgroundArt = WidgetConfig.BackgroundArt.values()[v]; backgroundOptions(form); refresh(false); });
        if (config.backgroundArt == WidgetConfig.BackgroundArt.GRADIENT) {
            form.addView(button("Gradient end color", () -> colorDialog("Gradient end", config.gradientEnd, v -> { config.gradientEnd = v; refresh(false); })));
            slider(form, "Gradient direction (degrees)", 0, 360, config.gradientAngle, v -> config.gradientAngle = v);
        }
        if (config.backgroundArt == WidgetConfig.BackgroundArt.IMAGE) {
            form.addView(button(config.backgroundImage.isEmpty() ? "Choose overlay image…" : "Replace overlay image…", () -> startActivityForResult(
                    new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE), 42)));
            if (!config.backgroundImage.isEmpty()) form.addView(button("Remove overlay image", () -> { config.backgroundImage = ""; backgroundOptions(form); refresh(false); }));
        }
        if (config.backgroundArt != WidgetConfig.BackgroundArt.NONE)
            slider(form, "Overlay opacity (%)", 0, 100, config.artOpacity, v -> config.artOpacity = v);
        form.addView(text("Overlays sit behind the icons. Every foreground icon still opens its assigned app.", 12, MUTED));
    }
    private void expandable(String title, java.util.function.Consumer<LinearLayout> populate) {
        LinearLayout contents = column(); contents.setPadding(dp(12), dp(4), dp(12), dp(12)); card(contents);
        contents.setVisibility(View.GONE);
        Button toggle = button(title + " ▸", () -> {});
        toggle.setOnClickListener(v -> {
            boolean show = contents.getVisibility() != View.VISIBLE;
            if (show && contents.getChildCount() == 0) populate.accept(contents);
            toggle.setText(title + (show ? " ▾" : " ▸"));
            contents.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show) contents.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                @Override public void onGlobalLayout() {
                    contents.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    if (contents.isShown()) contents.requestRectangleOnScreen(new android.graphics.Rect(0, 0, contents.getWidth(), Math.min(contents.getHeight(), dp(160))), false);
                }
            });
        });
        panel.addView(toggle); panel.addView(contents);
    }
    private void applyAppearance(int choice) {
        Themes.apply(config, choice);
        if (choice > 0) refresh(true);
    }
    private void chooseArtworkEntry() {
        String[] labels = config.icons.stream().map(e -> e.label).toArray(String[]::new);
        new AlertDialog.Builder(this).setTitle("Customize icon artwork").setItems(labels, (d, index) -> {
            var entry = config.icons.get(index);
            new AlertDialog.Builder(this).setTitle(entry.label).setItems(new String[]{"Choose image", "Use original app icon"}, (dialog, choice) -> {
                if (choice == 1) { entry.image = ""; refresh(false); }
                else { imageEntry = entry.id; startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*")
                        .addCategory(Intent.CATEGORY_OPENABLE), 41); }
            }).show();
        }).show();
    }
    private void spinner(LinearLayout parent, String label, String[] choices, int selected, IntConsumer update) {
        parent.addView(text(label, 14, TEXT));
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, choices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter); spinner.setSelection(selected); spinner.setContentDescription(label);
        parent.addView(spinner, new LinearLayout.LayoutParams(-1, dp(48)));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private int previous = selected;
            public void onNothingSelected(AdapterView<?> view) {}
            public void onItemSelected(AdapterView<?> view, View child, int position, long id) {
                if (position != previous) {
                    previous = position; update.accept(position);
                    if (choices == Themes.NAMES && position != 0) spinner.setSelection(0);
                }
            }
        });
    }
    private SeekBar slider(LinearLayout parent, String label, int min, int max, int value, IntConsumer update) {
        TextView caption = text(label + "  ·  " + value, 14, TEXT); parent.addView(caption);
        SeekBar bar = new SeekBar(this); bar.setMin(min); bar.setMax(max); bar.setProgress(value);
        bar.setContentDescription(label); parent.addView(bar, new LinearLayout.LayoutParams(-1, dp(42)));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStartTrackingTouch(SeekBar b) {}
            public void onStopTrackingTouch(SeekBar b) {}
            public void onProgressChanged(SeekBar b, int value, boolean user) {
                caption.setText(label + "  ·  " + value);
                if (user) { update.accept(value); refresh(false); }
            }
        });
        return bar;
    }
    private void toggle(LinearLayout parent, String label, boolean value, java.util.function.Consumer<Boolean> update) {
        Switch control = new Switch(this); control.setText(label); control.setTextColor(TEXT); control.setChecked(value);
        control.setPadding(0, dp(10), 0, dp(10));
        control.setOnCheckedChangeListener((v, checked) -> { update.accept(checked); refresh(false); });
        parent.addView(control, new LinearLayout.LayoutParams(-1, dp(56)));
    }
    private String hex(int color) { return String.format(Locale.ROOT, "#%06X", color & 0xffffff); }
    private void colorDialog(String title, int initial, IntConsumer update) {
        LinearLayout content = column(); content.setPadding(dp(20), dp(8), dp(20), dp(8));
        EditText input = new EditText(this); input.setSingleLine(); input.setText(hex(initial)); input.setHint("#RRGGBB"); content.addView(input);
        LinearLayout swatches = row();
        for (int color : new int[]{0xff000000, 0xffffffff, 0xffb9f97b, 0xffa9c8ff, 0xffffb6cc}) {
            Button b = button("●", () -> input.setText(hex(color))); b.setTextColor(color);
            b.setContentDescription(hex(color)); weighted(swatches, b);
        }
        content.addView(swatches);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(title).setView(content)
                .setNegativeButton("Cancel", null).setPositiveButton("Use color", null).create();
        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            String value = input.getText().toString().trim();
            if (!value.matches("#[0-9a-fA-F]{6}")) { input.setError("Enter a color like #B9F97B"); return; }
            update.accept(Color.parseColor(value)); dialog.dismiss();
        })); dialog.show();
    }
    private void appRows() {
        IconRenderer renderer = new IconRenderer(this);
        for (int i = 0; i < config.icons.size(); i++) {
            final int index = i; var entry = config.icons.get(i);
            LinearLayout item = row();
            ImageView image = new ImageView(this); image.setImageBitmap(renderer.icon(entry)); image.setColorFilter(IconRenderer.filter(config));
            item.addView(image, new LinearLayout.LayoutParams(dp(36), dp(36)));
            weighted(item, button((i + 1) + ". " + entry.label, () -> editIcon(index)));
            Button up = button("↑", () -> { config.move(index, index - 1); refresh(true); });
            up.setContentDescription("Move " + entry.label + " earlier"); up.setEnabled(i > 0);
            Button down = button("↓", () -> { config.move(index, index + 1); refresh(true); });
            down.setContentDescription("Move " + entry.label + " later"); down.setEnabled(i + 1 < config.icons.size());
            item.addView(up, new LinearLayout.LayoutParams(dp(48), dp(48)));
            item.addView(down, new LinearLayout.LayoutParams(dp(48), dp(48)));
            Button remove = button("×", () -> { config.icons.remove(entry); refresh(true); });
            remove.setContentDescription("Remove " + entry.label); item.addView(remove, new LinearLayout.LayoutParams(dp(48), dp(48)));
            panel.addView(item);
        }
    }
    private String component(ResolveInfo info) {
        return new ComponentName(info.activityInfo.packageName, info.activityInfo.name).flattenToString();
    }
    private void pickApps(WidgetConfig.IconEntry replacing) {
        if (!appsLoaded) { Toast.makeText(this, "Loading your apps. Try again in a moment.", Toast.LENGTH_SHORT).show(); return; }
        LinearLayout content = column(); content.setPadding(dp(16), 0, dp(16), 0);
        content.addView(text("Already selected apps are hidden. Each app can be added once.", 12, MUTED));
        EditText search = new EditText(this); search.setSingleLine(); search.setHint("Search installed apps"); content.addView(search);
        ListView list = new ListView(this); content.addView(list, new LinearLayout.LayoutParams(-1, dp(320)));
        Set<String> selected = new LinkedHashSet<>();
        List<ResolveInfo> filtered = new ArrayList<>(apps);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_multiple_choice, new ArrayList<>());
        list.setAdapter(adapter); list.setChoiceMode(replacing == null ? ListView.CHOICE_MODE_MULTIPLE : ListView.CHOICE_MODE_SINGLE);
        Runnable refill = () -> {
            filtered.clear(); adapter.clear();
            String query = search.getText().toString().toLowerCase(Locale.ROOT);
            for (ResolveInfo app : apps) {
                String label = app.loadLabel(getPackageManager()).toString();
                if (!config.containsApp(component(app), replacing) && label.toLowerCase(Locale.ROOT).contains(query)) { filtered.add(app); adapter.add(label); }
            }
            list.clearChoices();
            for (int i = 0; i < filtered.size(); i++) list.setItemChecked(i, selected.contains(component(filtered.get(i))));
            adapter.notifyDataSetChanged();
        };
        list.setOnItemClickListener((v, child, position, id) -> {
            String key = component(filtered.get(position));
            if (replacing != null) selected.clear();
            if (list.isItemChecked(position)) selected.add(key); else selected.remove(key);
        });
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int count, int after) {}
            public void onTextChanged(CharSequence s, int st, int before, int count) { refill.run(); }
            public void afterTextChanged(Editable e) {}
        }); refill.run();
        new AlertDialog.Builder(this).setTitle(replacing == null ? "Choose apps" : "Replace app").setView(content)
                .setNegativeButton("Cancel", null).setPositiveButton(replacing == null ? "Add selected" : "Replace", (d, which) -> {
                    
                    for (ResolveInfo app : apps) if (selected.contains(component(app)) && !config.containsApp(component(app), replacing)) {
                        if (replacing != null) {
                            replacing.component = component(app); replacing.label = app.loadLabel(getPackageManager()).toString(); break;
                        }
                        if (config.icons.size() >= WidgetConfig.MAX_ICONS) {
                            Toast.makeText(this, "The limit is 400 icons per widget.", Toast.LENGTH_LONG).show(); break;
                        }
                        var e = new WidgetConfig.IconEntry(UUID.randomUUID().toString(), component(app), app.loadLabel(getPackageManager()).toString());
                        config.addIcon(e);
                    }
                    refresh(true);
                }).show();
    }
    private void editIcon(int index) {
        if (index < 0 || index >= config.icons.size()) return;
        var entry = config.icons.get(index);
        String[] actions = {"Label & individual size", "Replace assigned app", "Move to position…", "Remove icon"};
        new AlertDialog.Builder(this).setTitle(entry.label).setItems(actions, (dialog, action) -> {
            switch (action) {
                case 0: iconProperties(entry); break;
                case 1: pickApps(entry); break;
                case 2:
                    EditText position = new EditText(this); position.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                    position.setText(String.valueOf(index + 1));
                    AlertDialog move = new AlertDialog.Builder(this).setTitle("Position (1–" + config.icons.size() + ")").setView(position)
                            .setNegativeButton("Cancel", null).setPositiveButton("Move", null).create();
                    move.setOnShowListener(v -> move.getButton(-1).setOnClickListener(b -> {
                        try {
                            int to = Integer.parseInt(position.getText().toString()) - 1;
                            if (to < 0 || to >= config.icons.size()) throw new NumberFormatException();
                            config.move(config.icons.indexOf(entry), to); refresh(true); move.dismiss();
                        } catch (NumberFormatException e) { position.setError("Choose an existing position"); }
                    })); move.show(); break;
                case 3:
                    config.icons.remove(entry);
                    refresh(true); break;
            }
        }).show();
    }
    private void iconProperties(WidgetConfig.IconEntry entry) {
        LinearLayout form = column(); form.setPadding(dp(20), dp(8), dp(20), dp(8));
        EditText label = new EditText(this); label.setText(entry.label); label.setSingleLine(); label.setHint("Label"); form.addView(label);
        int[] size = {(int) entry.size};
        form.addView(text(config.thumb() ? "0 uses the layer default. Icons shrink when needed to fit the widget." : "0 uses the default. Icons fit their grid cells.", 12, MUTED));
        slider(form, "This icon’s size (dp)", 0, config.thumb() ? 48 : 128, Math.min(config.thumb() ? 48 : 128, size[0]), v -> size[0] = v);
        new AlertDialog.Builder(this).setTitle("Icon properties").setView(form)
                .setNegativeButton("Cancel", null).setNeutralButton("Reset size", (d, w) -> { entry.size = 0; refresh(true); })
                .setPositiveButton("Apply", (d, w) -> {
                    if (!label.getText().toString().trim().isEmpty()) entry.label = label.getText().toString().trim();
                    entry.size = size[0] == 0 ? 0 : Math.max(12, size[0]); refresh(true);
                }).show();
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if ((request != 41 && request != 42) || result != RESULT_OK || data == null || data.getData() == null) return;
        WidgetConfig.IconEntry target = null;
        for (var e : config.icons) if (e.id.equals(imageEntry)) target = e;
        if (request == 41 && target == null) return;
        try {
            Bitmap bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), data.getData()), (decoder, info, source) -> {
                int w = info.getSize().getWidth(), h = info.getSize().getHeight();
                float scale = Math.min(1, (request == 42 ? 1024f : 512f) / Math.max(w, h));
                decoder.setTargetSize(Math.max(1, Math.round(w * scale)), Math.max(1, Math.round(h * scale)));
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            });
            String filename = UUID.randomUUID() + ".png";
            try (FileOutputStream stream = new FileOutputStream(new File(getFilesDir(), filename))) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            }
            if (request == 42) { config.backgroundImage = filename; config.backgroundArt = WidgetConfig.BackgroundArt.IMAGE; }
            else target.image = filename;
            refresh(true);
        } catch (Exception e) { Toast.makeText(this, "That image could not be opened. Try a PNG or JPEG.", Toast.LENGTH_LONG).show(); }
        imageEntry = null;
    }
    private void save() {
        if (!canSave()) return;
        if (config.defaultLabels != store.defaultLabels()) {
            config.defaultLabels = store.defaultLabels(); refresh(false);
        }
        store.save(widgetId, config); saved = ConfigStore.encode(config);
        if (widgetId != 0) {
            NavigatorWidget.update(this, widgetId);
            setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId));
        }
        if (configuring) finish();
        else if (widgetId != 0) Toast.makeText(this, "Widget saved", Toast.LENGTH_SHORT).show();
        else new AlertDialog.Builder(this).setTitle("Your widget is ready")
                .setMessage("Configuration saved. Add it to your home screen now.")
                .setNegativeButton("Done", null).setPositiveButton("Add widget", (d, w) -> pin()).show();
    }
    private void pin() {
        if (!canSave()) return;
        if (config.icons.isEmpty()) { return; }
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        if (!manager.isRequestPinAppWidgetSupported()) {
            new AlertDialog.Builder(this).setMessage("Save your draft, then long-press your home screen and choose Widgets → Icon Navigator.")
                    .setPositiveButton("OK", null).show(); return;
        }
        store.save(0, config);
        Intent callback = new Intent(this, PinReceiver.class).setData(android.net.Uri.parse("iconnavigator://pin/" + UUID.randomUUID()))
                .putExtra("snapshot", ConfigStore.encode(config));
        PendingIntent result = PendingIntent.getBroadcast(this, 0, callback,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        manager.requestPinAppWidget(WidgetProviders.component(this, config.thumb() ? WidgetConfig.Type.THUMB : WidgetConfig.Type.GRID_COMPACT), null, result);
    }
    private void chooseWidget() {
        int[] widgets = WidgetProviders.ids(this);
        List<String> labels = new ArrayList<>(); labels.add("New widget / draft");
        for (int id : widgets) { WidgetConfig c = WidgetProviders.load(this, id); labels.add("Widget " + id + " · " + c.icons.size() + " apps · " + DesignCatalog.name(c.layout)); }
        new AlertDialog.Builder(this).setTitle("Choose a widget").setItems(labels.toArray(new String[0]), (d, which) -> {
            Runnable change = () -> {
                widgetId = which == 0 ? 0 : widgets[which - 1]; config = widgetId == 0 ? store.load(0) : WidgetProviders.load(this, widgetId);
                Arrays.fill(scrollPositions, 0);
                saved = ConfigStore.encode(config); flow = new SetupFlow(); preview = null; previewPage = 0; build();
            };
            if (!saved.equals(ConfigStore.encode(config))) new AlertDialog.Builder(this).setMessage("Discard unsaved changes?")
                    .setNegativeButton("Keep editing", null).setPositiveButton("Discard", (dialog, w) -> change.run()).show();
            else change.run();
        }).show();
    }
    // Android 12/12L fallback. API 33+ uses the platform callback registered above.
    @android.annotation.SuppressLint("GestureBackNavigation")
    @Override public void onBackPressed() { confirmClose(); }
    private void confirmClose() {
        if (saved.equals(ConfigStore.encode(config))) { finish(); return; }
        new AlertDialog.Builder(this).setMessage("Discard unsaved changes?").setNegativeButton("Keep editing", null)
                .setPositiveButton("Discard", (d, w) -> finish()).show();
    }
}
