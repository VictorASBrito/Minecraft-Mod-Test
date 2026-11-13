package dev.victorasbrito.minecraftmodtest.progression;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExperienceCurveTest {
    @Test
    void startsAtLevelOne() {
        var state = ExperienceCurve.progress(0);
        assertEquals(1, state.level());
        assertEquals(0, state.xpInLevel());
        assertEquals(100, state.xpToNextLevel());
    }

    @Test
    void levelsUpAtExactlyOneHundredXp() {
        var state = ExperienceCurve.progress(100);
        assertEquals(2, state.level());
        assertEquals(0, state.xpInLevel());
        assertEquals(125, state.xpToNextLevel());
    }

    @Test
    void tracksPartialProgress() {
        var state = ExperienceCurve.progress(150);
        assertEquals(2, state.level());
        assertEquals(50, state.xpInLevel());
        assertEquals(125, state.xpToNextLevel());
    }

    @Test
    void handlesMultipleLevelUpsInOneGrant() {
        var state = ExperienceCurve.progress(225);
        assertEquals(3, state.level());
        assertEquals(0, state.xpInLevel());
        assertEquals(150, state.xpToNextLevel());
    }

    @Test
    void clampsToLevelCap() {
        int cap = ExperienceCurve.maxTotalXp();
        assertEquals(34300, cap);
        assertEquals(50, ExperienceCurve.progress(cap).level());
        assertEquals(0, ExperienceCurve.progress(cap + 500).xpToNextLevel());
        assertEquals(cap, ExperienceCurve.addExperience(cap - 5, 1000));
        assertEquals(cap, ExperienceCurve.addExperience(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Test
    void normalizesInvalidStoredValues() {
        assertEquals(1, ExperienceCurve.progress(-20).level());
        assertEquals(0, ExperienceCurve.normalize(-20));
    }

    @Test
    void rejectsNegativeGrants() {
        assertThrows(IllegalArgumentException.class, () -> ExperienceCurve.addExperience(10, -5));
    }
}
