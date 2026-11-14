package dev.victorasbrito.minecraftmodtest.battle.engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BattleRewardPolicyTest {
    @Test void authorizedDebugVictoryGivesKnownReward() {
        var result = BattleRewardPolicy.forOutcome(BattleEngine.Outcome.VICTORY, true);
        assertEquals(20, result.experience());
        assertEquals(1, result.rottenFlesh());
    }

    @Test void normalTestVictoryHasNoLoot() {
        var result = BattleRewardPolicy.forOutcome(BattleEngine.Outcome.VICTORY, false);
        assertEquals(0, result.experience());
        assertEquals(0, result.rottenFlesh());
    }

    @Test void fleeingOrLosingNeverRewards() {
        for (var status : new BattleEngine.Outcome[]{
                BattleEngine.Outcome.FLED, BattleEngine.Outcome.DEFEAT,
                BattleEngine.Outcome.ACTIVE
        }) {
            assertEquals(0, BattleRewardPolicy.forOutcome(status, true).experience());
            assertEquals(0, BattleRewardPolicy.forOutcome(status, true).rottenFlesh());
        }
    }
}
