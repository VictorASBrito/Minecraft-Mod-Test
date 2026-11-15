package dev.victorasbrito.minecraftmodtest.battle.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BattleTargetSelectionTest {
    @Test void cardsCanBeSelectedInWidescreen() {
        for(int i=0;i<2;i++) {
            var box=BattleHudLayout.targetCard(854,480,2,i);
            assertEquals(i,BattleHudLayout.targetIndexAt(854,480,2,
                    box.x()+box.width()/2.0,box.y()+box.height()/2.0));
        }
    }
    @Test void compactLayoutDoesNotOverlapLowerActions() {
        var t=BattleHudLayout.targetCard(360,240,2,0);
        var actions=BattleHudLayout.forScreen(360,240).actions();
        assertTrue(t.bottom()<actions.y());
    }
    @Test void deadSpaceIsNotATarget() {
        assertEquals(-1,BattleHudLayout.targetIndexAt(854,480,2,10,200));
        assertEquals(-1,BattleHudLayout.targetIndexAt(120,80,2,10,20));
    }
}
