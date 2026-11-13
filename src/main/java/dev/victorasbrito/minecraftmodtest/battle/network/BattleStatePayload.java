package dev.victorasbrito.minecraftmodtest.battle.network;

import dev.victorasbrito.minecraftmodtest.MinecraftModTest;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Snapshot completo enviado pelo servidor a cada turno. */
public record BattleStatePayload(
        String sessionId,
        int turnId,
        int playerHp,
        int playerMaxHp,
        int enemyHp,
        int enemyMaxHp,
        int playerSpeed,
        int enemySpeed,
        int playerGauge,
        int enemyGauge,
        boolean guarding,
        boolean playerTurn,
        String outcome,
        String combatLog,
        String turnPreview
) implements CustomPacketPayload {
    public static final Type<BattleStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MinecraftModTest.MOD_ID, "battle_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BattleStatePayload> STREAM_CODEC =
            StreamCodec.of(BattleStatePayload::write, BattleStatePayload::read);

    private static void write(RegistryFriendlyByteBuf buf, BattleStatePayload payload) {
        buf.writeUtf(payload.sessionId, 64);
        buf.writeVarInt(payload.turnId);
        buf.writeVarInt(payload.playerHp);
        buf.writeVarInt(payload.playerMaxHp);
        buf.writeVarInt(payload.enemyHp);
        buf.writeVarInt(payload.enemyMaxHp);
        buf.writeVarInt(payload.playerSpeed);
        buf.writeVarInt(payload.enemySpeed);
        buf.writeVarInt(payload.playerGauge);
        buf.writeVarInt(payload.enemyGauge);
        buf.writeBoolean(payload.guarding);
        buf.writeBoolean(payload.playerTurn);
        buf.writeUtf(payload.outcome, 16);
        buf.writeUtf(payload.combatLog, 512);
        buf.writeUtf(payload.turnPreview, 128);
    }

    private static BattleStatePayload read(RegistryFriendlyByteBuf buf) {
        return new BattleStatePayload(
                buf.readUtf(64),
                buf.readVarInt(),
                buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), buf.readVarInt(),
                buf.readBoolean(), buf.readBoolean(),
                buf.readUtf(16), buf.readUtf(512), buf.readUtf(128)
        );
    }

    public boolean finished() {
        return !"ACTIVE".equals(outcome);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
