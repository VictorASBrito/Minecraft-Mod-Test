package dev.victorasbrito.minecraftmodtest.battle.network;

import dev.victorasbrito.minecraftmodtest.MinecraftModTest;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Snapshot autoritativo de combate enviado pelo servidor ao cliente.
 * IMPORTANTE: HP/maxHP de inimigos nao sao transmitidos. Somente o motor
 * do servidor conhece a vida exata de monstros.
 */
public record BattleStatePayload(
        String sessionId,
        int turnId,
        int playerHp, int playerMaxHp,
        int playerSpeed, int enemySpeed, int playerGauge, int enemyGauge,
        boolean guarding, boolean playerTurn,
        String outcome, String combatLog, String turnPreview,
        double playerX, double playerY, double playerZ,
        double enemyX, double enemyY, double enemyZ,
        int actionSequence, String resolvedAction
) implements CustomPacketPayload {
    public static final Type<BattleStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MinecraftModTest.MOD_ID, "battle_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BattleStatePayload> STREAM_CODEC =
            StreamCodec.of(BattleStatePayload::write, BattleStatePayload::read);

    private static void write(RegistryFriendlyByteBuf buf, BattleStatePayload s) {
        buf.writeUtf(s.sessionId, 64);
        buf.writeVarInt(s.turnId);
        buf.writeVarInt(s.playerHp);
        buf.writeVarInt(s.playerMaxHp);
        buf.writeVarInt(s.playerSpeed);
        buf.writeVarInt(s.enemySpeed);
        buf.writeVarInt(s.playerGauge);
        buf.writeVarInt(s.enemyGauge);
        buf.writeBoolean(s.guarding);
        buf.writeBoolean(s.playerTurn);
        buf.writeUtf(s.outcome, 16);
        buf.writeUtf(s.combatLog, 512);
        buf.writeUtf(s.turnPreview, 128);
        buf.writeDouble(s.playerX);
        buf.writeDouble(s.playerY);
        buf.writeDouble(s.playerZ);
        buf.writeDouble(s.enemyX);
        buf.writeDouble(s.enemyY);
        buf.writeDouble(s.enemyZ);
        buf.writeVarInt(s.actionSequence);
        buf.writeUtf(s.resolvedAction, 16);
    }

    private static BattleStatePayload read(RegistryFriendlyByteBuf buf) {
        return new BattleStatePayload(
                buf.readUtf(64), buf.readVarInt(),
                buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readBoolean(), buf.readBoolean(),
                buf.readUtf(16), buf.readUtf(512), buf.readUtf(128),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readVarInt(), buf.readUtf(16)
        );
    }

    public boolean finished() {
        return !"ACTIVE".equals(outcome);
    }

    @Override public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
