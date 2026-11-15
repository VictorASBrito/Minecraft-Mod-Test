package dev.victorasbrito.minecraftmodtest.battle.engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BattleReinforcementTest {
    @Test void advertisedReinforcementPreventsPrematureVictory() {
        BattleEngine battle = new BattleEngine(120,70,1);
        battle.setPendingReinforcement(true);
        for (int i = 0; i < 3; i++) {
            assertTrue(battle.play(BattleEngine.Action.ATTACK,battle.turnId(),0));
        }
        assertEquals(0,battle.aliveEnemyCount());
        assertEquals(BattleEngine.Outcome.ACTIVE,battle.outcome());
        assertFalse(battle.isPlayerTurn());
        assertTrue(battle.addReinforcement(60));
        assertEquals(1,battle.aliveEnemyCount());
        for(int i=0;i<2;i++) {
            assertTrue(battle.play(BattleEngine.Action.ATTACK,battle.turnId(),1));
        }
        assertEquals(BattleEngine.Outcome.VICTORY,battle.outcome());
    }

    @Test void canceledReinforcementAllowsVictory() {
        BattleEngine battle = new BattleEngine(120,70,1);
        battle.setPendingReinforcement(true);
        for(int i=0;i<3;i++) battle.play(BattleEngine.Action.ATTACK,battle.turnId(),0);
        battle.setPendingReinforcement(false);
        assertEquals(BattleEngine.Outcome.VICTORY,battle.outcome());
    }

    @Test void canceledBattleNeverAwardsVictory() {
        BattleEngine battle = new BattleEngine();
        battle.cancel();
        assertEquals(BattleEngine.Outcome.FLED,battle.outcome());
        assertFalse(battle.addReinforcement(60));
    }
}
