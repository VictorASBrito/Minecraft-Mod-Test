package dev.victorasbrito.minecraftmodtest.battle.server;

import dev.victorasbrito.minecraftmodtest.battle.engine.BattleEngine;
import dev.victorasbrito.minecraftmodtest.battle.engine.BattleTargeting;
import dev.victorasbrito.minecraftmodtest.battle.network.BattleActionPayload;
import dev.victorasbrito.minecraftmodtest.battle.network.BattleResultPayload;
import dev.victorasbrito.minecraftmodtest.battle.network.BattleStatePayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Uma sessao por jogador. A lista de alvos, seus indices, dano e vida ficam
 * sob autoridade EXCLUSIVA do servidor. Cliente recebe so nomes, IDs e vivos.
 */
public final class BattleSessionManager {
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private BattleSessionManager() {}

    public static void startOrResume(ServerPlayer player) {
        startOrResume(player, false, 1);
    }

    public static void startOrResume(ServerPlayer player, boolean authorizedDebugRewards) {
        startOrResume(player, authorizedDebugRewards, 1);
    }

    /** Novo teste 1x2, sem XP/drops por ser um encontro ainda simulado. */
    public static void startGroup(ServerPlayer player) {
        startOrResume(player, false, 2);
    }

    private static void startOrResume(ServerPlayer player, boolean debugRewards, int enemyCount) {
        Session current = SESSIONS.get(player.getUUID());
        if (current != null) {
            send(player, current);
            return;
        }
        if (!player.onGround() || player.isPassenger() || player.isSpectator()) {
            player.sendSystemMessage(Component.literal(
                    "Inicie a batalha no chao, sem estar montado ou em modo espectador."));
            return;
        }

        List<Zombie> mobs = new ArrayList<>();
        List<Vec3> anchors = new ArrayList<>();
        for (int i = 0; i < enemyCount; i++) {
            Zombie mob = BattleWorldFixture.spawn(player, anchors);
            if (mob == null) {
                for (Zombie spawned : mobs) BattleWorldFixture.clear(spawned);
                player.sendSystemMessage(Component.literal(
                        "Nao foi possivel posicionar o grupo. Teste em terreno aberto e plano."));
                return;
            }
            mobs.add(mob);
            anchors.add(mob.position());
        }

        Vec3 anchor = player.position();
        Session session = new Session(UUID.randomUUID().toString(),
                new BattleEngine(120,70,enemyCount), mobs, anchors, anchor, debugRewards);
        SESSIONS.put(player.getUUID(), session);
        player.setDeltaMovement(Vec3.ZERO);
        faceEachOther(player, mobs.get(0));
        send(player, session);
    }

    public static boolean isInBattle(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
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
        finish(player, session);
        return true;
    }

    public static void onAction(ServerPlayer player, BattleActionPayload packet) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || !session.id.equals(packet.sessionId())) return;
        if (!worldParticipantsValid(session)) {
            cancel(player);
            return;
        }

        BattleEngine.Action action;
        try {
            action = BattleEngine.Action.valueOf(packet.action());
        } catch (IllegalArgumentException ignored) {
            send(player, session);
            return;
        }

        int target = packet.targetIndex();
        if (!session.engine.play(action, packet.turnId(), target)) {
            send(player, session);
            return;
        }

        session.actionSequence++;
        session.resolvedAction = action.name();
        session.resolvedTarget = action == BattleEngine.Action.ATTACK ? target : -1;
        if (action == BattleEngine.Action.ATTACK) {
            Zombie mob = session.mobs.get(target);
            faceEachOther(player, mob);
            player.swing(InteractionHand.MAIN_HAND, true);
            if (!session.engine.isEnemyAlive(target)) BattleWorldFixture.clear(mob);
        }

        send(player, session);
        if (session.engine.outcome() != BattleEngine.Outcome.ACTIVE) finish(player, session);
    }

    private static boolean worldParticipantsValid(Session session) {
        for (int i = 0; i < session.mobs.size(); i++) {
            if (session.engine.isEnemyAlive(i)) {
                Zombie mob = session.mobs.get(i);
                if (mob.isRemoved() || !mob.isAlive()) return false;
            }
        }
        return true;
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) return;
        if (!worldParticipantsValid(session)) {
            cancel(player);
            return;
        }

        player.setDeltaMovement(Vec3.ZERO);
        if (player.position().distanceToSqr(session.playerAnchor) > 0.09) {
            player.teleportTo(session.playerAnchor.x,session.playerAnchor.y,session.playerAnchor.z);
        }
        for (int i = 0; i < session.mobs.size(); i++) {
            if (!session.engine.isEnemyAlive(i)) continue;
            Zombie mob = session.mobs.get(i);
            Vec3 anchor = session.enemyAnchors.get(i);
            mob.setDeltaMovement(Vec3.ZERO);
            if (mob.position().distanceToSqr(anchor) > 0.04) {
                mob.teleportTo(anchor.x,anchor.y,anchor.z);
            }
        }
    }

    private static void faceEachOther(ServerPlayer player, Zombie mob) {
        float yaw = BattleTargeting.yawTowards(
                player.getX(),player.getZ(),mob.getX(),mob.getZ());
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setYBodyRot(yaw);
        mob.setYRot(yaw + 180f);
        mob.setYBodyRot(yaw + 180f);
        mob.setYHeadRot(yaw + 180f);
    }

    private static void send(ServerPlayer player, Session session) {
        BattleEngine engine = session.engine;
        List<BattleStatePayload.TargetSnapshot> targets = new ArrayList<>();
        Vec3 middle = Vec3.ZERO;
        int alive = 0;
        for (int i = 0; i < session.mobs.size(); i++) {
            Vec3 point = session.enemyAnchors.get(i);
            boolean living = engine.isEnemyAlive(i);
            targets.add(new BattleStatePayload.TargetSnapshot(
                    i,session.mobs.get(i).getId(),session.mobs.size()==1 ? "Zumbi" : "Zumbi " + (i+1),
                    living,point.x,point.y,point.z));
            if (living) {
                middle = middle.add(point);
                alive++;
            }
        }
        Vec3 focus = alive > 0 ? middle.scale(1.0 / alive) : session.enemyAnchors.get(0);
        Vec3 p = session.playerAnchor;
        PacketDistributor.sendToPlayer(player,new BattleStatePayload(
                session.id,engine.turnId(),engine.playerHp(),engine.playerMaxHp(),
                engine.playerSpeed(),engine.enemySpeed(),engine.playerGauge(),engine.enemyGauge(),
                engine.isGuarding(),engine.isPlayerTurn(),engine.outcome().name(),
                engine.combatLog(),engine.previewTurns(5),
                p.x,p.y,p.z,focus.x,focus.y,focus.z,
                session.actionSequence,session.resolvedAction,session.resolvedTarget,targets
        ));
    }

    public static void onInvulnerabilityCheck(EntityInvulnerabilityCheckEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && SESSIONS.containsKey(player.getUUID())) {
            event.setInvulnerable(true);
        }
    }

    /** Remove sessao ANTES de qualquer XP/loot; nunca premia duas vezes. */
    private static void finish(ServerPlayer player, Session session) {
        if (SESSIONS.remove(player.getUUID(),session)) {
            for (Zombie mob : session.mobs) BattleWorldFixture.clear(mob);
            BattleResultPayload receipt = BattleRewardService.settle(
                    player,session.id,session.engine.outcome(),session.debugRewards);
            PacketDistributor.sendToPlayer(player,receipt);
        }
    }

    private static void end(ServerPlayer player, Session session) {
        if (SESSIONS.remove(player.getUUID(),session)) {
            for (Zombie mob : session.mobs) BattleWorldFixture.clear(mob);
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Session session = SESSIONS.get(player.getUUID());
            if (session != null) end(player,session);
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        for (Session session : SESSIONS.values()) {
            for (Zombie mob : session.mobs) BattleWorldFixture.clear(mob);
        }
        SESSIONS.clear();
    }

    private static final class Session {
        final String id;
        final BattleEngine engine;
        final List<Zombie> mobs;
        final List<Vec3> enemyAnchors;
        final Vec3 playerAnchor;
        final boolean debugRewards;
        int actionSequence;
        String resolvedAction = "";
        int resolvedTarget = -1;

        Session(String id,BattleEngine engine,List<Zombie> mobs,List<Vec3> enemyAnchors,
                Vec3 playerAnchor,boolean debugRewards) {
            this.id = id;
            this.engine = engine;
            this.mobs = List.copyOf(mobs);
            this.enemyAnchors = List.copyOf(enemyAnchors);
            this.playerAnchor = playerAnchor;
            this.debugRewards = debugRewards;
        }
    }
}
