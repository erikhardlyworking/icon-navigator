package dev.iconnavigator;

import org.junit.Test;
import static org.junit.Assert.*;

public class SetupFlowTest {
    @Test public void everyViewRemainsAvailableBeforeAndAfterAppRemoval() {
        var flow = new SetupFlow(); var c = LayoutEngineTest.fixture(WidgetConfig.Type.GRID_COMPACT, 1);
        for (int step = 0; step < 3; step++) { flow.step = step; assertTrue(flow.canVisit(step)); assertTrue(flow.canSave(c)); }
        c.icons.clear(); assertFalse(flow.canSave(c));
        for (int step = 0; step < 3; step++) assertTrue(flow.canVisit(step));
        assertFalse(flow.canVisit(-1)); assertFalse(flow.canVisit(3));
    }
}
