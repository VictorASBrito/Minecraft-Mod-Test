package dev.victorasbrito.minecraftmodtest.battle.network;

import dev.victorasbrito.minecraftmodtest.MinecraftModTest;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Intencao do jogador, nunca dano ou HP. Alvo e um indice da lista da
 * sessao, validado no servidor para cada ataque.
 */
public record BattleActionPayload(String sessionId, int turnId, String action, int targetIndex)
        implements CustomPacketPayload {
    public static final Type<BattleActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MinecraftModTest.MOD_ID, "battle_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BattleActionPayload> STREAM_CODEC =
            StreamCodec.of(BattleActionPayload::write, BattleActionPayload::read);

    private static void write(RegistryFriendlyByteBuf buf, BattleActionPayload p) {
        buf.writeUtf(p.sessionId, 64);
        buf.writeVarInt(p.turnId);
        buf.writeUtf(p.action, 16);
        buf.writeVarInt(p.targetIndex);
    }

    private static BattleActionPayload read(RegistryFriendlyByteBuf buf) {
        return new BattleActionPayload(buf.readUtf(64), buf.readVarInt(), buf.readUtf(16), buf.readVarInt());
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
