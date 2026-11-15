package dev.victorasbrito.minecraftmodtest.battle.server;

/**
 * Parametros conservadores para a PRIMEIRA integracao com o mundo.
 * Baseados na RFC-003, mas limites mais baixos enquanto o HUD suporta
 * um jogador e cinco alvos historicos. Ajustar por config futuramente.
 */
public final class EncounterRules {
    public static final int TRIGGER_RADIUS = 6;
    public static final int AGGRO_MIN_TICKS = 20;
    public static final int INITIAL_RADIUS = 12;
    public static final int REINFORCEMENT_RADIUS = 16;
    public static final int MAX_VERTICAL = 5;
    public static final int MAX_INITIAL = 3;
    public static final int MAX_CONCURRENT = 3;
    public static final int MAX_REINFORCEMENTS = 2;
    public static final int MAX_WAVES = 2;
    public static final int MAX_TOTAL = 5;
    public static final int INITIAL_THREAT_BUDGET = 6;
    public static final int TOTAL_THREAT_BUDGET = 12;
    public static final int THREAT_ZOMBIE = 1;
    public static final int WARNING_ACTIONS = 2;
    public static final int MIN_ACTIONS_BETWEEN_WAVES = 2;
    public static final int REINFORCEMENT_SCAN_TICKS = 20;
    public static final int POST_BATTLE_GRACE_TICKS = 300;

    private EncounterRules() {}

    public static boolean canSchedule(int active, int total, int reinforced,
                                      int waves, int threat, int actionsSinceLastWave) {
        return active > 0 && active < MAX_CONCURRENT
                && total < MAX_TOTAL && reinforced < MAX_REINFORCEMENTS
                && waves < MAX_WAVES && threat + THREAT_ZOMBIE <= TOTAL_THREAT_BUDGET
                && actionsSinceLastWave >= MIN_ACTIONS_BETWEEN_WAVES;
    }
}
