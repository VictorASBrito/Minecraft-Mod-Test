package dev.victorasbrito.minecraftmodtest.progression;

import com.mojang.serialization.Codec;
import dev.victorasbrito.minecraftmodtest.MinecraftModTest;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Salva XP total do RPG como anexo persistente do jogador.
 * O NeoForge replica automaticamente o valor na morte com copyOnDeath.
 */
public final class ProgressionAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MinecraftModTest.MOD_ID);

    public static final Supplier<AttachmentType<Integer>> RPG_TOTAL_XP = ATTACHMENTS.register(
            "rpg_total_xp",
            () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT)
                    .copyOnDeath()
                    .build()
    );

    private ProgressionAttachments() {
    }

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }
}
