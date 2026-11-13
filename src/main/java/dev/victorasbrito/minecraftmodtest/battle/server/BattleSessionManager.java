package dev.victorasbrito.minecraftmodtest.battle.server;

import dev.victorasbrito.minecraftmodtest.battle.engine.BattleEngine;
import dev.victorasbrito.minecraftmodtest.battle.network.BattleActionPayload;
import dev.victorasbrito.minecraftmodtest.battle.network.BattleStatePayload;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Liga o motor deterministico ao cenario real (entidades do mapa).
 * MVP: zumbi de teste sem IA, vida e dano SIMULADOS, sem recompensas vanilla.
 * Sao preservados HP e inventario real do jogador.
 */
public final class BattleSessionManager {
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private BattleSessionManager() {}

    public static void startOrResume(ServerPlayer player) {
        Session existing = SESSIONS.get(player.getUUID());
        if (existing != null) {
            send(player, existing);
            return;
        }

        if (!player.onGround() || player.isPassenger() || player.isSpectator()) {
            player.sendSystemMessage(Component.literal(
                    "Inicie o teste no chao, sem estar montado ou em modo espectador."));
            return;
        }

        Zombie zombie = BattleWorldFixture.spawn(player);
        if (zombie == null) {
            player.sendSystemMessage(Component.literal(
                    "Nao encontramos espaco seguro para posicionar o zumbi. Teste em terreno plano."));
            return;
        }

        Vec3 anchor = player.position();
        Session session = new Session(UUID.randomUUID().toString(), new BattleEngine(),
                zombie, anchor, zombie.position());
        SESSIONS.put(player.getUUID(), session);
        player.setDeltaMovement(Vec3.ZERO);
        faceEachOther(player, zombie);
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
        session.engine.play(BattleEngine.Action.FLEE, session.engine.turnId());
        send(player, session);
        end(player, session);
        return true;
    }

    public static void onAction(ServerPlayer player, BattleActionPayload payload) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || !session.id.equals(payload.sessionId())) return;
        if (session.zombie.isRemoved() || !session.zombie.isAlive()) {
            cancel(player);
            return;
        }

        BattleEngine.Action action;
        try {
            action = BattleEngine.Action.valueOf(payload.action());
        } catch (IllegalArgumentException exception) {
            send(player, session);
            return;
        }
        if (!session.engine.play(action, payload.turnId())) {
            send(player, session); // Evita segundo ataque no mesmo turno.
            return;
        }

        send(player, session);
        if (session.engine.outcome() != BattleEngine.Outcome.ACTIVE) {
            end(player, session);
        }
    }

    /** Congela apenas os participantes deste teste, nunca o servidor inteiro. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) return;
        if (session.zombie.isRemoved() || !session.zombie.isAlive()) {
            cancel(player);
            return;
        }
        player.setDeltaMovement(Vec3.ZERO);
        if (player.position().distanceToSqr(session.playerAnchor) > 0.09) {
            player.teleportTo(session.playerAnchor.x, session.playerAnchor.y, session.playerAnchor.z);
        }
        session.zombie.setDeltaMovement(Vec3.ZERO);
        if (session.zombie.position().distanceToSqr(session.enemyAnchor) > 0.04) {
            session.zombie.teleportTo(session.enemyAnchor.x, session.enemyAnchor.y, session.enemyAnchor.z);
        }
    }

    private static void faceEachOther(ServerPlayer player, Zombie zombie) {
        Vec3 diff = zombie.position().subtract(player.position());
        float toEnemy = (float) Math.toDegrees(Math.atan2(-diff.x, diff.z));
        player.setYRot(toEnemy);
        player.setYHeadRot(toEnemy);
        player.setYBodyRot(toEnemy);
        zombie.setYRot(toEnemy + 180f);
        zombie.setYBodyRot(toEnemy + 180f);
        zombie.setYHeadRot(toEnemy + 180f);
    }

    private static void send(ServerPlayer player, Session session) {
        BattleEngine e = session.engine;
        Vec3 a = session.playerAnchor, b = session.enemyAnchor;
        PacketDistributor.sendToPlayer(player, new BattleStatePayload(
                session.id, e.turnId(),
                e.playerHp(), e.playerMaxHp(), e.enemyHp(), e.enemyMaxHp(),
                e.playerSpeed(), e.enemySpeed(), e.playerGauge(), e.enemyGauge(),
                e.isGuarding(), e.isPlayerTurn(), e.outcome().name(),
                e.combatLog(), e.previewTurns(5),
                a.x, a.y, a.z, b.x, b.y, b.z
        ));
    }

    /**
     * Protecao condicionada a uma batalha ATIVA, sem alterar/salvar flags do jogador.
     * Mesmo num crash, o proximo login nao preserva invulnerabilidade artificial.
     */
    public static void onInvulnerabilityCheck(EntityInvulnerabilityCheckEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && SESSIONS.containsKey(player.getUUID())) {
            event.setInvulnerable(true);
        }
    }

    private static void end(ServerPlayer player, Session session) {
        if (SESSIONS.remove(player.getUUID(), session)) {
            BattleWorldFixture.clear(session.zombie);
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Session session = SESSIONS.get(player.getUUID());
            if (session != null) end(player, session);
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        for (Session session : SESSIONS.values()) {
            BattleWorldFixture.clear(session.zombie);
        }
        SESSIONS.clear();
    }

    private static final class Session {
        final String id;
        final BattleEngine engine;
        final Zombie zombie;
        final Vec3 playerAnchor;
        final Vec3 enemyAnchor;

        Session(String id, BattleEngine engine, Zombie zombie, Vec3 playerAnchor,
                Vec3 enemyAnchor) {
            this.id = id;
            this.engine = engine;
            this.zombie = zombie;
            this.playerAnchor = playerAnchor;
            this.enemyAnchor = enemyAnchor;
        }
    }
}
