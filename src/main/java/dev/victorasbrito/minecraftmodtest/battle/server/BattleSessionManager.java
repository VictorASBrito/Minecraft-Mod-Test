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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Camada autoritativa entre AI vanilla, entidades reais e motor RPG.
 * Mobs reservados nao podem participar de duas batalhas; sempre restaurar
 * IA/invulnerabilidade de sobreviventes ao cancelar.
 *
 * Um zumbi REAL derrotado e descartado sem loot vanilla. O receipt de XP/itens
 * e entregue somente na vitoria e uma vez por sessao.
 */
public final class BattleSessionManager {
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final Map<UUID, String> RESERVED_MOBS = new HashMap<>();

    private BattleSessionManager() {}

    public static boolean isReserved(Zombie zombie) {
        return RESERVED_MOBS.containsKey(zombie.getUUID());
    }

    public static boolean isInBattle(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    public static void startOrResume(ServerPlayer player) {
        startOrResume(player, false, 1);
    }

    public static void startOrResume(ServerPlayer player, boolean debugRewards) {
        startOrResume(player, debugRewards, 1);
    }

    public static void startGroup(ServerPlayer player) {
        startOrResume(player, false, 2);
    }

    /** Compatibilidade: a batalha manual ainda gera seus proprios mobs de teste. */
    private static void startOrResume(ServerPlayer player, boolean debugRewards, int count) {
        Session existing = SESSIONS.get(player.getUUID());
        if (existing != null) {
            send(player,existing);
            return;
        }
        if (!player.onGround() || player.isPassenger() || player.isSpectator()) {
            player.sendSystemMessage(Component.literal("Inicie o teste no chao e sem montaria."));
            return;
        }

        List<Zombie> spawned = new ArrayList<>();
        List<Vec3> occupied = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Zombie zombie = BattleWorldFixture.spawn(player, occupied);
            if (zombie == null) {
                for (Zombie mob : spawned) BattleWorldFixture.clear(mob);
                player.sendSystemMessage(Component.literal("Nao ha espaco seguro para os zumbis de teste."));
                return;
            }
            spawned.add(zombie);
            occupied.add(zombie.position());
        }
        if (!startSession(player,spawned,true,debugRewards)) {
            for (Zombie mob : spawned) BattleWorldFixture.clear(mob);
            player.sendSystemMessage(Component.literal("Falha ao reservar o grupo de teste."));
        }
    }

    /**
     * Chamado SOMENTE pela verificacao de aggro em thread principal do servidor.
     * Nao clona ou gera mobs; a identidade do monstro vanilla e preservada.
     */
    public static boolean startNatural(ServerPlayer player, List<Zombie> group) {
        if (SESSIONS.containsKey(player.getUUID()) ||
                !BattleEncounterManager.canStartFor(player)
                || group.isEmpty() || group.size() > EncounterRules.MAX_INITIAL) return false;

        for (Zombie mob : group) {
            if (!BattleEncounterManager.eligibleZombie(
                    player,mob,EncounterRules.INITIAL_RADIUS)) return false;
        }
        return startSession(player,group,false,false);
    }

    private static boolean startSession(ServerPlayer player,List<Zombie> group,
                                        boolean spawned,boolean debugRewards) {
        if (SESSIONS.containsKey(player.getUUID())) return false;
        String sessionId = UUID.randomUUID().toString();
        List<MobSlot> prepared = new ArrayList<>();
        for (Zombie zombie : group) {
            MobSlot slot = new MobSlot(zombie,spawned);
            if (!reserve(sessionId,slot)) {
                for (MobSlot earlier : prepared) release(sessionId,earlier,false);
                return false;
            }
            prepared.add(slot);
        }

        Session session = new Session(sessionId,new BattleEngine(120,70,prepared.size()),
                prepared,player.position(),player,!spawned,debugRewards);
        SESSIONS.put(player.getUUID(),session);
        player.setDeltaMovement(Vec3.ZERO);
        faceEachOther(player,prepared.get(0).mob);
        send(player,session);
        return true;
    }

    private static boolean reserve(String id,MobSlot slot) {
        if (slot.mob.isRemoved() || !slot.mob.isAlive()
                || RESERVED_MOBS.putIfAbsent(slot.mob.getUUID(),id) != null) {
            return false;
        }
        if (!slot.spawned) {
            // Gravado ANTES do bloqueio para recuperar IA/invulnerabilidade
            // apos crash e reload de chunk, sem deixar zumbis permanentes.
            slot.mob.getPersistentData().putBoolean("minecraft_mod_test_reserved_real",true);
            slot.mob.getPersistentData().putBoolean("minecraft_mod_test_old_no_ai",slot.oldNoAi);
            slot.mob.getPersistentData().putBoolean("minecraft_mod_test_old_invuln",slot.oldInvulnerable);
        }
        slot.mob.setNoAi(true);
        slot.mob.setInvulnerable(true);
        slot.mob.setTarget(null);
        slot.mob.setDeltaMovement(Vec3.ZERO);
        return true;
    }

    /**
     * false = restore AI for natural survivors; true = definitive RPG death.
     * Fixture entities are ALWAYS removed as synthetic test participants.
     */
    private static void release(String sessionId,MobSlot slot,boolean defeated) {
        if (!RESERVED_MOBS.remove(slot.mob.getUUID(),sessionId)) return;
        if (!slot.spawned) {
            slot.mob.getPersistentData().remove("minecraft_mod_test_reserved_real");
            slot.mob.getPersistentData().remove("minecraft_mod_test_old_no_ai");
            slot.mob.getPersistentData().remove("minecraft_mod_test_old_invuln");
        }
        if (slot.spawned || defeated) {
            BattleWorldFixture.clear(slot.mob);
        } else if (!slot.mob.isRemoved()) {
            slot.mob.setNoAi(slot.oldNoAi);
            slot.mob.setInvulnerable(slot.oldInvulnerable);
            slot.mob.setTarget(slot.oldTarget != null && slot.oldTarget.isAlive()
                    ? slot.oldTarget : null);
        }
    }

    public static boolean resume(ServerPlayer player) {
        Session session=SESSIONS.get(player.getUUID());
        if (session==null) return false;
        send(player,session);
        return true;
    }

    public static boolean cancel(ServerPlayer player) {
        Session session=SESSIONS.get(player.getUUID());
        if (session==null) return false;
        session.engine.cancel();
        send(player,session);
        finish(player,session);
        return true;
    }

    public static void onAction(ServerPlayer player,BattleActionPayload packet) {
        Session session=SESSIONS.get(player.getUUID());
        if (session==null || !session.id.equals(packet.sessionId())) return;
        if (!worldParticipantsValid(player,session)) {
            cancel(player);
            return;
        }

        BattleEngine.Action action;
        try {
            action=BattleEngine.Action.valueOf(packet.action());
        } catch (IllegalArgumentException exception) {
            send(player,session);
            return;
        }

        int target=packet.targetIndex();
        if (!session.engine.play(action,packet.turnId(),target)) {
            send(player,session);
            return;
        }

        session.actionSequence++;
        session.resolvedAction=action.name();
        session.resolvedTarget=action == BattleEngine.Action.ATTACK ? target : -1;

        if (action == BattleEngine.Action.ATTACK) {
            MobSlot slot=session.slots.get(target);
            faceEachOther(player,slot.mob);
            player.swing(InteractionHand.MAIN_HAND,true);
            if (!session.engine.isEnemyAlive(target)) {
                release(session.id,slot,true);
            }
        }

        advancePending(player,session,BattleEncounterManager.currentTick());

        if (session.engine.outcome()==BattleEngine.Outcome.ACTIVE) {
            send(player,session);
        } else {
            send(player,session);
            finish(player,session);
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Session session=SESSIONS.get(player.getUUID());
        if (session==null) return;
        if (!worldParticipantsValid(player,session)) {
            cancel(player);
            return;
        }

        player.setDeltaMovement(Vec3.ZERO);
        if (player.position().distanceToSqr(session.playerAnchor) > 0.09) {
            player.teleportTo(session.playerAnchor.x,session.playerAnchor.y,session.playerAnchor.z);
        }
        for (int i=0;i<session.slots.size();i++) {
            if (session.engine.isEnemyAlive(i)) holdAtAnchor(session.slots.get(i));
        }
        if (session.pending!=null) holdAtAnchor(session.pending.slot);
    }

    private static void holdAtAnchor(MobSlot slot) {
        if (slot.mob.isRemoved()) return;
        slot.mob.setDeltaMovement(Vec3.ZERO);
        if (slot.mob.position().distanceToSqr(slot.anchor)>0.04) {
            slot.mob.teleportTo(slot.anchor.x,slot.anchor.y,slot.anchor.z);
        }
    }

    private static boolean worldParticipantsValid(ServerPlayer player,Session session) {
        if (!player.isAlive()) return false;
        for (int i=0;i<session.slots.size();i++) {
            if (!session.engine.isEnemyAlive(i)) continue;
            MobSlot slot=session.slots.get(i);
            if (slot.mob.isRemoved() || !slot.mob.isAlive()
                    || slot.mob.level()!=player.level()
                    || !session.id.equals(RESERVED_MOBS.get(slot.mob.getUUID()))) return false;
        }
        return true;
    }

    /**
     * Limitado por scan de 20 ticks, 1 pendencia por sessao, no maximo
     * 2 reforcos totais e 2 ondas. Nenhum mob novo e gerado artificialmente.
     */
    public static void tryScheduleReinforcement(ServerPlayer player,long tick) {
        Session session=SESSIONS.get(player.getUUID());
        if (session==null || session.pending!=null ||
                session.engine.outcome()!=BattleEngine.Outcome.ACTIVE ||
                !EncounterRules.canSchedule(session.engine.aliveEnemyCount(),
                    session.slots.size(),session.reinforcements,session.waves,
                    session.slots.size(),
                    session.engine.resolvedActions()-session.lastWaveAction)) return;

        Zombie candidate=BattleEncounterManager.findReinforcement(player);
        if (candidate==null) return;
        MobSlot slot=new MobSlot(candidate,false);
        if (!reserve(session.id,slot)) return;

        session.pending=new Pending(slot,session.engine.resolvedActions(),tick);
        session.waves++;
        session.engine.setPendingReinforcement(true);
        send(player,session);
    }

    public static void tickPendingReinforcements(long tick) {
        // Copy prevents structural modification while finishing sessions.
        for (var entry : new ArrayList<>(SESSIONS.entrySet())) {
            if (entry.getValue().pending==null) continue;
            ServerPlayer player=entry.getValue().player;
            if (player!=null) advancePending(player,entry.getValue(),tick);
        }
    }

    private static void advancePending(ServerPlayer player,Session session,long tick) {
        Pending pending=session.pending;
        if (pending==null) return;
        boolean invalid=pending.slot.mob.isRemoved() || !pending.slot.mob.isAlive()
                || pending.slot.mob.level()!=player.level()
                || !session.id.equals(RESERVED_MOBS.get(pending.slot.mob.getUUID()));
        if (invalid) {
            cancelPending(session);
            if (session.engine.outcome()==BattleEngine.Outcome.VICTORY) {
                send(player,session);
                finish(player,session);
            } else send(player,session);
            return;
        }

        int actions=session.engine.resolvedActions()-pending.startedAtActions;
        // Situacao limite: ultimo monstro caiu antes de 2 acoes; aguarda
        // um periodo pequeno, mas nunca deixa a batalha sem participante
        // ativo indefinidamente.
        boolean lastEnemyGone=session.engine.aliveEnemyCount()==0;
        boolean ready=actions>=EncounterRules.WARNING_ACTIONS
                || (lastEnemyGone && tick-pending.startedAtTick>=30);
        if (!ready) return;

        int speed=Math.max(40,70-(session.slots.size()*10));
        if (session.engine.addReinforcement(speed)) {
            session.slots.add(pending.slot);
            session.reinforcements++;
            session.lastWaveAction=session.engine.resolvedActions();
            session.pending=null;
            send(player,session);
        } else {
            cancelPending(session);
            if (session.engine.outcome()==BattleEngine.Outcome.VICTORY) {
                send(player,session);
                finish(player,session);
            }
        }
    }

    private static void cancelPending(Session session) {
        if (session.pending==null) return;
        release(session.id,session.pending.slot,false);
        session.pending=null;
        session.engine.setPendingReinforcement(false);
    }

    private static void faceEachOther(ServerPlayer player,Zombie mob) {
        float yaw=BattleTargeting.yawTowards(
                player.getX(),player.getZ(),mob.getX(),mob.getZ());
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setYBodyRot(yaw);
        mob.setYRot(yaw+180f);
        mob.setYBodyRot(yaw+180f);
        mob.setYHeadRot(yaw+180f);
    }

    private static void send(ServerPlayer player,Session session) {
        BattleEngine engine=session.engine;
        List<BattleStatePayload.TargetSnapshot> targets=new ArrayList<>();
        Vec3 middle=Vec3.ZERO;
        int alive=0;
        for (int i=0;i<session.slots.size();i++) {
            MobSlot slot=session.slots.get(i);
            boolean living=engine.isEnemyAlive(i);
            Vec3 p=slot.anchor;
            targets.add(new BattleStatePayload.TargetSnapshot(
                    i,slot.mob.getId(),session.slots.size()==1?"Zumbi":"Zumbi "+(i+1),
                    living,p.x,p.y,p.z));
            if (living) {
                middle=middle.add(p);
                alive++;
            }
        }
        Vec3 focus=alive>0?middle.scale(1.0/alive):session.slots.get(0).anchor;
        Vec3 p=session.playerAnchor;
        int remaining=session.pending==null?0:Math.max(0,
                EncounterRules.WARNING_ACTIONS-(engine.resolvedActions()
                        -session.pending.startedAtActions));
        PacketDistributor.sendToPlayer(player,new BattleStatePayload(
                session.id,engine.turnId(),engine.playerHp(),engine.playerMaxHp(),
                engine.playerSpeed(),engine.enemySpeed(),engine.playerGauge(),engine.enemyGauge(),
                engine.isGuarding(),engine.isPlayerTurn(),engine.outcome().name(),
                engine.combatLog(),engine.previewTurns(5),
                p.x,p.y,p.z,focus.x,focus.y,focus.z,
                session.actionSequence,session.resolvedAction,session.resolvedTarget,
                targets,session.pending!=null,remaining
        ));
    }

    public static void onInvulnerabilityCheck(EntityInvulnerabilityCheckEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && SESSIONS.containsKey(player.getUUID())) {
            event.setInvulnerable(true);
        }
    }

    private static void finish(ServerPlayer player,Session session) {
        if (!SESSIONS.remove(player.getUUID(),session)) return;
        if (session.pending!=null) cancelPending(session);

        for (int i=0;i<session.slots.size();i++) {
            MobSlot slot=session.slots.get(i);
            // Derrotados ja foram descartados quando sua vida RPG chegou a 0.
            boolean defeated=!session.engine.isEnemyAlive(i)
                    || session.engine.outcome()==BattleEngine.Outcome.VICTORY;
            release(session.id,slot,defeated);
        }

        BattleEncounterManager.onBattleEnded(player);
        int naturalKilled=session.natural
                && session.engine.outcome()==BattleEngine.Outcome.VICTORY
                ? session.slots.size() : 0;
        BattleResultPayload result=BattleRewardService.settle(
                player,session.id,session.engine.outcome(),session.debugRewards,naturalKilled);
        PacketDistributor.sendToPlayer(player,result);
    }

    private static void endWithoutRewards(ServerPlayer player,Session session) {
        if (!SESSIONS.remove(player.getUUID(),session)) return;
        if (session.pending!=null) cancelPending(session);
        for (int i=0;i<session.slots.size();i++) {
            release(session.id,session.slots.get(i),!session.engine.isEnemyAlive(i));
        }
        BattleEncounterManager.onBattleEnded(player);
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Session session=SESSIONS.get(player.getUUID());
            if (session!=null) endWithoutRewards(player,session);
        }
    }

    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Session session=SESSIONS.get(player.getUUID());
            if (session!=null) {
                session.engine.cancel();
                endWithoutRewards(player,session);
            }
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        for (var e : new ArrayList<>(SESSIONS.entrySet())) {
            Session session=e.getValue();
            if (session.pending!=null) cancelPending(session);
            for (int i=0;i<session.slots.size();i++) {
                release(session.id,session.slots.get(i),!session.engine.isEnemyAlive(i));
            }
        }
        SESSIONS.clear();
        RESERVED_MOBS.clear();
        BattleEncounterManager.onServerStopped();
    }

    /** Estado vanilla anterior a reserva, para restaurar sem salvar flags falsas. */
    private static final class MobSlot {
        final Zombie mob;
        final Vec3 anchor;
        final boolean spawned;
        final boolean oldNoAi;
        final boolean oldInvulnerable;
        final LivingEntity oldTarget;

        MobSlot(Zombie mob,boolean spawned) {
            this.mob=mob;
            this.anchor=mob.position();
            this.spawned=spawned;
            this.oldNoAi=mob.isNoAi();
            this.oldInvulnerable=mob.isInvulnerable();
            this.oldTarget=mob.getTarget();
        }
    }

    private record Pending(MobSlot slot,int startedAtActions,long startedAtTick) {}

    private static final class Session {
        final String id;
        final BattleEngine engine;
        final List<MobSlot> slots;
        final Vec3 playerAnchor;
        final ServerPlayer player;
        final boolean natural;
        final boolean debugRewards;
        int actionSequence;
        String resolvedAction="";
        int resolvedTarget=-1;
        int reinforcements;
        int waves;
        int lastWaveAction=-EncounterRules.MIN_ACTIONS_BETWEEN_WAVES;
        Pending pending;

        Session(String id,BattleEngine engine,List<MobSlot> slots,Vec3 playerAnchor,
                ServerPlayer player,boolean natural,boolean debugRewards) {
            this.id=id;
            this.engine=engine;
            this.slots=new ArrayList<>(slots);
            this.playerAnchor=playerAnchor;
            this.player=player;
            this.natural=natural;
            this.debugRewards=debugRewards;
        }
    }
}
