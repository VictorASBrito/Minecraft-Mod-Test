package dev.victorasbrito.minecraftmodtest.battle.network;

import dev.victorasbrito.minecraftmodtest.MinecraftModTest;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Snapshot validado pelo servidor. Nunca transfere HP/maxHP de monstros.
 * Cada alvo fornece apenas identificador, entidade visual, estado vivo
 * e coordenadas para mirar/enquadrar o mundo.
 */
public record BattleStatePayload(
        String sessionId, int turnId,
        int playerHp, int playerMaxHp,
        int playerSpeed, int enemySpeed, int playerGauge, int enemyGauge,
        boolean guarding, boolean playerTurn,
        String outcome, String combatLog, String turnPreview,
        double playerX, double playerY, double playerZ,
        double enemyX, double enemyY, double enemyZ,
        int actionSequence, String resolvedAction, int resolvedTargetIndex,
        List<TargetSnapshot> targets,
        boolean reinforcementPending,
        int reinforcementActionsRemaining
) implements CustomPacketPayload {
    public static final int MAX_TARGETS = 5;
    public static final Type<BattleStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MinecraftModTest.MOD_ID, "battle_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BattleStatePayload> STREAM_CODEC =
            StreamCodec.of(BattleStatePayload::write, BattleStatePayload::read);

    public BattleStatePayload {
        targets = List.copyOf(targets);
        if (targets.size() > MAX_TARGETS) throw new IllegalArgumentException("Excesso de alvos");
    }

    public record TargetSnapshot(int index, int entityId, String name, boolean alive,
                                 double x, double y, double z) {
    }

    private static void write(RegistryFriendlyByteBuf b, BattleStatePayload s) {
        b.writeUtf(s.sessionId, 64);
        b.writeVarInt(s.turnId);
        b.writeVarInt(s.playerHp);
        b.writeVarInt(s.playerMaxHp);
        b.writeVarInt(s.playerSpeed);
        b.writeVarInt(s.enemySpeed);
        b.writeVarInt(s.playerGauge);
        b.writeVarInt(s.enemyGauge);
        b.writeBoolean(s.guarding);
        b.writeBoolean(s.playerTurn);
        b.writeUtf(s.outcome, 16);
        b.writeUtf(s.combatLog, 512);
        b.writeUtf(s.turnPreview, 128);
        b.writeDouble(s.playerX);
        b.writeDouble(s.playerY);
        b.writeDouble(s.playerZ);
        b.writeDouble(s.enemyX);
        b.writeDouble(s.enemyY);
        b.writeDouble(s.enemyZ);
        b.writeVarInt(s.actionSequence);
        b.writeUtf(s.resolvedAction, 16);
        b.writeVarInt(s.resolvedTargetIndex);
        b.writeVarInt(s.targets.size());
        for (TargetSnapshot t : s.targets) {
            b.writeVarInt(t.index);
            b.writeVarInt(t.entityId);
            b.writeUtf(t.name, 48);
            b.writeBoolean(t.alive);
            b.writeDouble(t.x);
            b.writeDouble(t.y);
            b.writeDouble(t.z);
        }
        b.writeBoolean(s.reinforcementPending);
        b.writeVarInt(s.reinforcementActionsRemaining);
    }

    private static BattleStatePayload read(RegistryFriendlyByteBuf b) {
        String id = b.readUtf(64);
        int turn = b.readVarInt();
        int hp = b.readVarInt(), maxHp = b.readVarInt();
        int ps = b.readVarInt(), es = b.readVarInt(), pg = b.readVarInt(), eg = b.readVarInt();
        boolean guard = b.readBoolean(), playerTurn = b.readBoolean();
        String outcome = b.readUtf(16), log = b.readUtf(512), preview = b.readUtf(128);
        double px = b.readDouble(), py = b.readDouble(), pz = b.readDouble();
        double ex = b.readDouble(), ey = b.readDouble(), ez = b.readDouble();
        int sequence = b.readVarInt();
        String action = b.readUtf(16);
        int resolved = b.readVarInt();
        int length = b.readVarInt();
        if (length < 0 || length > MAX_TARGETS) throw new IllegalArgumentException("Alvos invalidos");
        List<TargetSnapshot> targets = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            targets.add(new TargetSnapshot(b.readVarInt(),b.readVarInt(),b.readUtf(48),b.readBoolean(),
                    b.readDouble(),b.readDouble(),b.readDouble()));
        }
        boolean pending=b.readBoolean();
        int remaining=b.readVarInt();
        return new BattleStatePayload(id,turn,hp,maxHp,ps,es,pg,eg,
                guard,playerTurn,outcome,log,preview,px,py,pz,ex,ey,ez,
                sequence,action,resolved,targets,pending,remaining);
    }

    public boolean finished() { return !"ACTIVE".equals(outcome); }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
