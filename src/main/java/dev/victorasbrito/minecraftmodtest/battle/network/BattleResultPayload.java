package dev.victorasbrito.minecraftmodtest.battle.network;

import dev.victorasbrito.minecraftmodtest.MinecraftModTest;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Resultado imutavel, calculado e entregue apenas pelo servidor.
 * Nenhum cliente pode solicitar XP/loot por este payload (S2C only).
 */
public record BattleResultPayload(
        String sessionId,
        String outcome,
        int awardedXp,
        int totalXpBefore,
        int totalXpAfter,
        List<LootEntry> items
) implements CustomPacketPayload {
    public static final int MAX_LOOT_ENTRIES = 8;
    public static final Type<BattleResultPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MinecraftModTest.MOD_ID, "battle_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BattleResultPayload> STREAM_CODEC =
            StreamCodec.of(BattleResultPayload::write, BattleResultPayload::read);

    public BattleResultPayload {
        items = List.copyOf(items);
        if (items.size() > MAX_LOOT_ENTRIES) {
            throw new IllegalArgumentException("Too many loot entries");
        }
        if (awardedXp < 0 || totalXpBefore < 0 || totalXpAfter < totalXpBefore) {
            throw new IllegalArgumentException("Invalid RPG XP receipt");
        }
    }

    public record LootEntry(String itemId, int count) {
        public LootEntry {
            if (count < 1 || count > 64 || itemId.length() > 128) {
                throw new IllegalArgumentException("Invalid loot entry");
            }
        }
    }

    private static void write(RegistryFriendlyByteBuf buf, BattleResultPayload result) {
        buf.writeUtf(result.sessionId, 64);
        buf.writeUtf(result.outcome, 16);
        buf.writeVarInt(result.awardedXp);
        buf.writeVarInt(result.totalXpBefore);
        buf.writeVarInt(result.totalXpAfter);
        buf.writeVarInt(result.items.size());
        for (LootEntry loot : result.items) {
            buf.writeUtf(loot.itemId, 128);
            buf.writeVarInt(loot.count);
        }
    }

    private static BattleResultPayload read(RegistryFriendlyByteBuf buf) {
        String sessionId = buf.readUtf(64);
        String outcome = buf.readUtf(16);
        int awardedXp = buf.readVarInt();
        int before = buf.readVarInt();
        int after = buf.readVarInt();
        int count = buf.readVarInt();
        if (count < 0 || count > MAX_LOOT_ENTRIES) {
            throw new IllegalArgumentException("Invalid loot packet count");
        }
        List<LootEntry> loot = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            loot.add(new LootEntry(buf.readUtf(128), buf.readVarInt()));
        }
        return new BattleResultPayload(sessionId, outcome, awardedXp, before, after, loot);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
