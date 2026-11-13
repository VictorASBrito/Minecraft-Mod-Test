package dev.victorasbrito.minecraftmodtest.battle.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BattleHudLayoutTest {
    @Test void compactLayoutDoesNotOverlap() {
        var layout = BattleHudLayout.forScreen(427, 240);
        assertTrue(layout.compact());
        assertTrue(layout.status().bottom() < layout.actions().y());
        assertTrue(layout.actions().right() <= 427);
        assertTrue(layout.actions().bottom() <= 240);
    }

    @Test void widescreenLayoutAlignsPanelsOnSameRow() {
        var layout = BattleHudLayout.forScreen(854, 480);
        assertFalse(layout.compact());
        assertEquals(layout.status().y(), layout.actions().y());
        assertTrue(layout.status().right() < layout.actions().x());
        assertTrue(layout.actions().right() <= 854);
        assertTrue(layout.actions().bottom() <= 480);
    }

    @Test void minimumResolutionHasValidRectangles() {
        var layout = BattleHudLayout.forScreen(240, 160);
        assertTrue(layout.status().y() >= 0);
        assertTrue(layout.status().width() > 0);
        assertTrue(layout.actions().width() > 0);
        assertTrue(layout.actions().y() >= layout.status().bottom());
    }

    @Test void rejectsUnusableScreenSizes() {
        assertThrows(IllegalArgumentException.class,
                () -> BattleHudLayout.forScreen(150, 80));
    }
}
