package dev.iconnavigator;

public final class DesignCatalog {
    private DesignCatalog() {}
    public static String name(WidgetConfig.Layout layout) {
        return switch (layout) { case GRID -> "Grid"; case LEFT_HAND -> "Left thumb"; case RIGHT_HAND -> "Right thumb"; };
    }
}
