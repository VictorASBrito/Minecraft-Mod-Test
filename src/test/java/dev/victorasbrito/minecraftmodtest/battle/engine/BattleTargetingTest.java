package dev.victorasbrito.minecraftmodtest.battle.engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BattleTargetingTest {
    @Test void facesPositiveZ() { assertEquals(0f, BattleTargeting.yawTowards(0,0,0,4), 0.01f); }
    @Test void facesPositiveX() { assertEquals(-90f, BattleTargeting.yawTowards(0,0,4,0), 0.01f); }
    @Test void facesNegativeX() { assertEquals(90f, BattleTargeting.yawTowards(0,0,-4,0), 0.01f); }
    @Test void facesNegativeZ() { assertEquals(180f, Math.abs(BattleTargeting.yawTowards(0,0,0,-4)),0.01f); }
    @Test void handlesOffsetOrigin() { assertEquals(-90f,BattleTargeting.yawTowards(5,8,10,8),0.01f); }
}
