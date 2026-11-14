package dev.victorasbrito.minecraftmodtest.battle.engine;

/** Matematica pura de orientacao do avatar em direcao ao inimigo. */
public final class BattleTargeting {
    private BattleTargeting() {}

    /** Em Minecraft: Z positivo = 0 graus; X positivo = -90 graus. */
    public static float yawTowards(double fromX, double fromZ, double toX, double toZ) {
        double dx = toX - fromX;
        double dz = toZ - fromZ;
        if (dx * dx + dz * dz < 1e-8) return 0f;
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }
}
