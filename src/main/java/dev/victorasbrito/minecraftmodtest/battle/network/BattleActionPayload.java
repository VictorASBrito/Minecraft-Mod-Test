package dev.victorasbrito.minecraftmodtest.battle.network;

import dev.victorasbrito.minecraftmodtest.MinecraftModTest;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Pedido do cliente, nunca contem dano/vida calculados pelo jogador.
 */
public record BattleActionPayload(String sessionId, int turnId, String action) implements CustomPacketPayload {
    public static final Type<BattleActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MinecraftModTest.MOD_ID, "battle_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BattleActionPayload> STREAM_CODEC =
            StreamCodec.of(BattleActionPayload::write, BattleActionPayload::read);

    private static void write(RegistryFriendlyByteBuf buf, BattleActionPayload payload) {
        buf.writeUtf(payload.sessionId, 64);
        buf.writeVarInt(payload.turnId);
        buf.writeUtf(payload.action, 16);
    }

    private static BattleActionPayload read(RegistryFriendlyByteBuf buf) {
        return new BattleActionPayload(buf.readUtf(64), buf.readVarInt(), buf.readUtf(16));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
