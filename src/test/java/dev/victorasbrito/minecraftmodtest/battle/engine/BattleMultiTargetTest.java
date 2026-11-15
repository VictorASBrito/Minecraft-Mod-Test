package dev.victorasbrito.minecraftmodtest.battle.engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BattleMultiTargetTest {
    @Test void bothZombiesMustBeDefeated() {
        BattleEngine engine = new BattleEngine(120, 70, 2);
        assertEquals(2, engine.aliveEnemyCount());
        assertEquals(2, engine.enemyCount());

        int steps = 0;
        while (engine.isEnemyAlive(1) && engine.outcome() == BattleEngine.Outcome.ACTIVE && steps++ < 10) {
            assertTrue(engine.play(BattleEngine.Action.ATTACK, engine.turnId(), 1));
        }
        assertFalse(engine.isEnemyAlive(1));
        assertEquals(1, engine.aliveEnemyCount());
        assertEquals(BattleEngine.Outcome.ACTIVE, engine.outcome());
        assertTrue(engine.previewTurns(5).contains("Zumbi 1"));
        assertFalse(engine.previewTurns(5).contains("Zumbi 2"));

        while (engine.outcome() == BattleEngine.Outcome.ACTIVE && steps++ < 20) {
            assertTrue(engine.play(BattleEngine.Action.ATTACK, engine.turnId(), 0));
        }
        assertEquals(BattleEngine.Outcome.VICTORY, engine.outcome());
    }

    @Test void invalidTargetsDoNotConsumeTurns() {
        BattleEngine engine = new BattleEngine(120,70,2);
        int id = engine.turnId();
        assertFalse(engine.play(BattleEngine.Action.ATTACK,id,-1));
        assertFalse(engine.play(BattleEngine.Action.ATTACK,id,99));
        assertEquals(id,engine.turnId());
        assertTrue(engine.isPlayerTurn());
        assertEquals(60,engine.enemyHp(0));
        assertTrue(engine.play(BattleEngine.Action.ATTACK,id,0));
        assertFalse(engine.play(BattleEngine.Action.ATTACK,id,0));
    }

    @Test void singleEnemyCompatibility() {
        BattleEngine engine = new BattleEngine();
        assertEquals(1, engine.enemyCount());
        assertEquals(90, engine.enemyHp());
        assertTrue(engine.play(BattleEngine.Action.ATTACK,engine.turnId()));
        assertEquals(60,engine.enemyHp());
    }

    @Test void rejectsUnsupportedEnemyCounts() {
        assertEquals(5, new BattleEngine(120,70,5).enemyCount());
        assertThrows(IllegalArgumentException.class, ()->new BattleEngine(120,70,0));
        assertThrows(IllegalArgumentException.class, ()->new BattleEngine(120,70,6));
    }
}
