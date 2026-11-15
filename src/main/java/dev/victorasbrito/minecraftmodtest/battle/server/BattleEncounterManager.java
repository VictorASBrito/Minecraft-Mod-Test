package dev.victorasbrito.minecraftmodtest.battle.server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Observa aggro REAL de zumbis em jogadores no servidor.
 * Nao cria mobs, e nao inicia batalhas apenas pela proximidade.
 */
public final class BattleEncounterManager {
    private static final Map<UUID, AggroObservation> OBSERVED = new HashMap<>();
    private static final Map<UUID, Long> GRACE_UNTIL = new HashMap<>();
    private static boolean enabled = true;
    private static long tick;

    private BattleEncounterManager() {}

    public static boolean isEnabled() { return enabled; }
    public static long currentTick() { return tick; }
    public static void setEnabled(boolean value) {
        enabled = value;
        if (!enabled) OBSERVED.clear();
    }

    public static void onBattleEnded(ServerPlayer player) {
        GRACE_UNTIL.put(player.getUUID(), tick + EncounterRules.POST_BATTLE_GRACE_TICKS);
        OBSERVED.entrySet().removeIf(entry ->
                entry.getValue().playerId().equals(player.getUUID()));
    }

    public static void onServerStopped() {
        OBSERVED.clear();
        GRACE_UNTIL.clear();
        tick = 0;
        enabled = true;
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        tick++;
        // Pendencias sao pequenas (1 por sessao) e exigem verificacao de validade.
        BattleSessionManager.tickPendingReinforcements(tick);

        if (tick % 5 != 0) return;
        if (tick % 100 == 0) {
            OBSERVED.entrySet().removeIf(e -> tick - e.getValue().lastSeen() > 20);
            GRACE_UNTIL.entrySet().removeIf(e -> e.getValue() <= tick);
        }

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (BattleSessionManager.isInBattle(player)) {
                if (enabled && tick % EncounterRules.REINFORCEMENT_SCAN_TICKS == 0) {
                    BattleSessionManager.tryScheduleReinforcement(player, tick);
                }
                continue;
            }
            if (!enabled || !canStartFor(player)) continue;

            List<Zombie> local = player.level().getEntitiesOfClass(
                    Zombie.class,
                    player.getBoundingBox().inflate(
                            EncounterRules.TRIGGER_RADIUS, EncounterRules.MAX_VERTICAL,
                            EncounterRules.TRIGGER_RADIUS));
            local.sort(Comparator.<Zombie>comparingDouble(z -> player.distanceToSqr(z))
                    .thenComparing(mob -> mob.getUUID().toString()));

            for (Zombie zombie : local) {
                if (!eligibleZombie(player, zombie, EncounterRules.TRIGGER_RADIUS)) {
                    OBSERVED.remove(zombie.getUUID());
                    continue;
                }
                AggroObservation previous = OBSERVED.get(zombie.getUUID());
                if (previous == null || !previous.playerId().equals(player.getUUID())
                        || tick - previous.lastSeen() > 5) {
                    OBSERVED.put(zombie.getUUID(), new AggroObservation(player.getUUID(), tick, tick));
                    continue;
                }
                OBSERVED.put(zombie.getUUID(),
                        new AggroObservation(player.getUUID(), previous.firstSeen(), tick));
                if (tick - previous.firstSeen() >= EncounterRules.AGGRO_MIN_TICKS) {
                    List<Zombie> group = chooseInitialGroup(player, zombie);
                    if (BattleSessionManager.startNatural(player, group)) {
                        OBSERVED.remove(zombie.getUUID());
                        break;
                    }
                }
            }
        }
    }

    public static boolean canStartFor(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && !player.isCreative()
                && !player.isPassenger() && player.onGround()
                && !BattleSessionManager.isInBattle(player)
                && GRACE_UNTIL.getOrDefault(player.getUUID(), 0L) <= tick;
    }

    /** Primeiro release: apenas zumbis vanilla com alvo e visao do jogador. */
    public static boolean eligibleZombie(ServerPlayer player, Zombie zombie, int radius) {
        if (!zombie.isAlive() || zombie.isRemoved() || zombie.isNoAi()
                || zombie.isInvulnerable() || BattleSessionManager.isReserved(zombie)
                || zombie.level() != player.level() || zombie.getTarget() != player) return false;
        if (Math.abs(zombie.getY() - player.getY()) > EncounterRules.MAX_VERTICAL) return false;
        if (zombie.distanceToSqr(player) > radius * radius) return false;
        return zombie.getSensing().hasLineOfSight(player);
    }

    /** Iniciador primeiro; depois hostis que ja perseguem o MESMO jogador. */
    private static List<Zombie> chooseInitialGroup(ServerPlayer player, Zombie initiator) {
        List<Zombie> selected = new ArrayList<>();
        selected.add(initiator);
        List<Zombie> candidates = player.level().getEntitiesOfClass(
                Zombie.class,
                player.getBoundingBox().inflate(
                        EncounterRules.INITIAL_RADIUS, EncounterRules.MAX_VERTICAL,
                        EncounterRules.INITIAL_RADIUS));
        candidates.removeIf(z -> z == initiator ||
                !eligibleZombie(player, z, EncounterRules.INITIAL_RADIUS));
        candidates.sort(Comparator.<Zombie>comparingDouble(z -> player.distanceToSqr(z))
                .thenComparing(z -> z.getUUID().toString()));
        int threat = EncounterRules.THREAT_ZOMBIE;
        for (Zombie zombie : candidates) {
            if (selected.size() >= EncounterRules.MAX_INITIAL) break;
            if (threat + EncounterRules.THREAT_ZOMBIE > EncounterRules.INITIAL_THREAT_BUDGET) break;
            selected.add(zombie);
            threat += EncounterRules.THREAT_ZOMBIE;
        }
        return selected;
    }

    /** Somente monstros reais que atualmente perseguem o jogador. */
    public static Zombie findReinforcement(ServerPlayer player) {
        List<Zombie> candidates = player.level().getEntitiesOfClass(
                Zombie.class,
                player.getBoundingBox().inflate(
                        EncounterRules.REINFORCEMENT_RADIUS, EncounterRules.MAX_VERTICAL,
                        EncounterRules.REINFORCEMENT_RADIUS));
        candidates.removeIf(z -> !eligibleZombie(player, z, EncounterRules.REINFORCEMENT_RADIUS));
        candidates.sort(Comparator.<Zombie>comparingDouble(z -> player.distanceToSqr(z))
                .thenComparing(z -> z.getUUID().toString()));
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private record AggroObservation(UUID playerId, long firstSeen, long lastSeen) {}
}
