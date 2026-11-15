package dev.victorasbrito.minecraftmodtest.battle.server;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EncounterRulesTest {
    @Test void acceptsSmallReinforcement() {
        assertTrue(EncounterRules.canSchedule(2,2,0,0,2,2));
    }
    @Test void enforcesConcurrentAndLifetimeCaps() {
        assertFalse(EncounterRules.canSchedule(3,3,0,0,3,3));
        assertFalse(EncounterRules.canSchedule(1,5,1,1,5,4));
        assertFalse(EncounterRules.canSchedule(1,4,2,2,4,4));
        assertFalse(EncounterRules.canSchedule(1,4,1,2,4,4));
    }
    @Test void doesNotScheduleWithoutLivingEnemiesOrBetweenWaves() {
        assertFalse(EncounterRules.canSchedule(0,2,0,0,2,4));
        assertFalse(EncounterRules.canSchedule(1,2,0,0,2,1));
    }
    @Test void respectsThreatLimit() {
        assertFalse(EncounterRules.canSchedule(1,2,0,0,12,4));
    }
}
