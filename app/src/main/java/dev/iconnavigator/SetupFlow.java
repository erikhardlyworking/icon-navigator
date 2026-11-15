package dev.iconnavigator;

/** The three editor views are peers; changes never lock another view. */
public final class SetupFlow {
    public int step;
    public boolean canVisit(int target) { return target >= 0 && target < 3; }
    public boolean canSave(WidgetConfig c) { return !c.icons.isEmpty() && c.icons.size() <= c.capacity(); }
}
