package dev.victorasbrito.minecraftmodtest.battle.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BattleHudActionHitTest {
    @Test void wideScreenCardsHaveCorrectClickTargets() {
        for (int index = 0; index < 5; index++) {
            var rect = BattleHudLayout.actionCard(854, 480, index);
            assertEquals(index, BattleHudLayout.actionIndexAt(854, 480,
                    rect.x() + rect.width() / 2.0, rect.y() + rect.height() / 2.0));
        }
    }

    @Test void compactScreenCardsHaveCorrectClickTargets() {
        for (int index = 0; index < 5; index++) {
            var rect = BattleHudLayout.actionCard(360, 240, index);
            assertEquals(index, BattleHudLayout.actionIndexAt(360, 240,
                    rect.x() + rect.width() / 2.0, rect.y() + rect.height() / 2.0));
        }
    }

    @Test void emptySpaceAndMarginsNeverActivateActions() {
        assertEquals(-1, BattleHudLayout.actionIndexAt(854, 480, 4, 4));
        var first = BattleHudLayout.actionCard(854, 480, 0);
        assertEquals(-1, BattleHudLayout.actionIndexAt(854, 480,
                first.right() + 2, first.y() + 10));
    }

    @Test void outsideMinimumScreenDoesNotAcceptClicks() {
        assertEquals(-1, BattleHudLayout.actionIndexAt(200, 120, 80, 80));
    }
}
