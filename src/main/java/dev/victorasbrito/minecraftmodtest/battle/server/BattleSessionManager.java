package dev.victorasbrito.minecraftmodtest.battle.server;

import dev.victorasbrito.minecraftmodtest.battle.engine.BattleEngine;
import dev.victorasbrito.minecraftmodtest.battle.network.BattleActionPayload;
import dev.victorasbrito.minecraftmodtest.battle.network.BattleStatePayload;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Gerencia batalhas de teste sem entidades reais, dano vanilla ou recompensas.
 * Chamadas devem ocorrer no thread principal do servidor.
 */
public final class BattleSessionManager {
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private BattleSessionManager() {}

    public static void startOrResume(ServerPlayer player) {
        Session session = SESSIONS.computeIfAbsent(
                player.getUUID(),
                unused -> new Session(UUID.randomUUID().toString(), new BattleEngine())
        );
        send(player, session);
    }

    public static boolean resume(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) return false;
        send(player, session);
        return true;
    }

    public static boolean cancel(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) return false;
        if (session.engine.isPlayerTurn()) {
            session.engine.play(BattleEngine.Action.FLEE, session.engine.turnId());
        }
        send(player, session);
        SESSIONS.remove(player.getUUID());
        return true;
    }

    public static void onAction(ServerPlayer player, BattleActionPayload packet) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || !session.id.equals(packet.sessionId())) {
            return;
        }
        BattleEngine.Action action;
        try {
            action = BattleEngine.Action.valueOf(packet.action());
        } catch (IllegalArgumentException exception) {
            send(player, session);
            return;
        }

        if (!session.engine.play(action, packet.turnId())) {
            send(player, session); // Re-sincroniza pacotes repetidos/antigos.
            return;
        }
        send(player, session);
        if (session.engine.outcome() != BattleEngine.Outcome.ACTIVE) {
            SESSIONS.remove(player.getUUID());
        }
    }

    private static void send(ServerPlayer player, Session session) {
        BattleEngine e = session.engine;
        PacketDistributor.sendToPlayer(player, new BattleStatePayload(
                session.id,
                e.turnId(),
                e.playerHp(), e.playerMaxHp(),
                e.enemyHp(), e.enemyMaxHp(),
                e.playerSpeed(), e.enemySpeed(),
                e.playerGauge(), e.enemyGauge(),
                e.isGuarding(), e.isPlayerTurn(),
                e.outcome().name(), e.combatLog(),
                e.previewTurns(5)
        ));
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SESSIONS.remove(player.getUUID());
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        SESSIONS.clear();
    }

    private record Session(String id, BattleEngine engine) {}
}
