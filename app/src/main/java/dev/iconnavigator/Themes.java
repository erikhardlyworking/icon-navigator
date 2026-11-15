package dev.iconnavigator;

/** Every preset value is also exposed in Visual options or Advanced settings. */
public final class Themes {
    private Themes() {}
    public static final String[] NAMES = {"Customize below", "Transparent", "Clean minimal", "Light", "Dark", "Forest", "Dusk gradient"};
    public static void apply(WidgetConfig c, int theme) {
        if (theme <= 0 || theme >= NAMES.length) return;
        c.iconBorder = c.widgetBorder = 0;
        c.backgroundArt = WidgetConfig.BackgroundArt.NONE;
        c.paddingX = c.paddingY = 4; c.outerPadding = 0;
        c.background = 0xff000000; c.opacity = 0; c.effect = WidgetConfig.Effect.ORIGINAL;
        if (theme == 2) c.effect = WidgetConfig.Effect.GRAYSCALE;
        if (theme == 3) { c.background = 0xfff3f0e7; c.opacity = 90; }
        if (theme == 4) { c.background = 0xff15191f; c.opacity = 85; }
        if (theme == 5) { c.background = 0xff15251d; c.opacity = 80; c.outerPadding = 8; }
        if (theme == 6) {
            c.background = 0xff192742; c.gradientEnd = 0xff673e5d; c.gradientAngle = 45;
            c.backgroundArt = WidgetConfig.BackgroundArt.GRADIENT; c.artOpacity = 85;
        }
    }
}
