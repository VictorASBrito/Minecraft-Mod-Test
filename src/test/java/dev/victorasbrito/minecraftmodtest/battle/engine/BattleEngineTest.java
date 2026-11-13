package dev.victorasbrito.minecraftmodtest.battle.engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BattleEngineTest {
    @Test void playerGetsTurnOnStart() {
        BattleEngine engine = new BattleEngine();
        assertEquals(BattleEngine.Outcome.ACTIVE, engine.outcome());
        assertTrue(engine.isPlayerTurn());
        assertEquals(1, engine.turnId());
        assertTrue(engine.previewTurns(4).startsWith("Voce"));
    }

    @Test void rejectsRepeatedOrOutOfTurnActions() {
        BattleEngine engine = new BattleEngine();
        assertFalse(engine.play(BattleEngine.Action.ATTACK, engine.turnId() + 1));
        assertEquals(90, engine.enemyHp());
        assertTrue(engine.play(BattleEngine.Action.ATTACK, 1));
        assertEquals(60, engine.enemyHp());
        assertFalse(engine.play(BattleEngine.Action.ATTACK, 1));
        assertEquals(60, engine.enemyHp());
    }

    @Test void threeAttacksDefeatTheZombie() {
        BattleEngine engine = new BattleEngine();
        int actions = 0;
        while (engine.outcome() == BattleEngine.Outcome.ACTIVE && actions++ < 10) {
            assertTrue(engine.isPlayerTurn());
            assertTrue(engine.play(BattleEngine.Action.ATTACK, engine.turnId()));
        }
        assertEquals(BattleEngine.Outcome.VICTORY, engine.outcome());
        assertEquals(0, engine.enemyHp());
        assertFalse(engine.isPlayerTurn());
    }

    @Test void guardMakesNextZombieAttackWeaker() {
        BattleEngine plain = new BattleEngine();
        plain.play(BattleEngine.Action.ATTACK, plain.turnId());
        int plainDamage = 120 - plain.playerHp();
        assertTrue(plainDamage > 0);

        BattleEngine defended = new BattleEngine();
        defended.play(BattleEngine.Action.GUARD, defended.turnId());
        for (int i = 0; i < 3 && defended.playerHp() == 120; i++) {
            defended.play(BattleEngine.Action.GUARD, defended.turnId());
        }
        assertTrue(defended.playerHp() < 120);
        assertTrue(120 - defended.playerHp() < plainDamage);
    }

    @Test void fleeEndsWithoutRewardsOrDamage() {
        BattleEngine engine = new BattleEngine();
        assertTrue(engine.play(BattleEngine.Action.FLEE, 1));
        assertEquals(BattleEngine.Outcome.FLED, engine.outcome());
        assertFalse(engine.play(BattleEngine.Action.ATTACK, 1));
    }

    @Test void slowPlayerCanLoseWithoutActing() {
        BattleEngine engine = new BattleEngine(1, 400);
        assertEquals(BattleEngine.Outcome.DEFEAT, engine.outcome());
        assertEquals(0, engine.playerHp());
    }

    @Test void invalidSpeedIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new BattleEngine(0, 90));
        assertThrows(IllegalArgumentException.class, () -> new BattleEngine(100, 1000));
    }

    @Test void fastPlayerReceivesMoreInitiative() {
        BattleEngine fast = new BattleEngine(150, 60);
        BattleEngine slow = new BattleEngine(60, 150);
        assertTrue(fast.isPlayerTurn());
        assertTrue(slow.playerHp() < fast.playerHp());
    }
}
