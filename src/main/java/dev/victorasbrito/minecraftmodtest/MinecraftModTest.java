package dev.victorasbrito.minecraftmodtest;

import com.mojang.logging.LogUtils;
import dev.victorasbrito.minecraftmodtest.command.RpgStatusCommand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Entrada do mod. Novos sistemas devem ficar em classes e pacotes proprios.
 */
@Mod(MinecraftModTest.MOD_ID)
public final class MinecraftModTest {
    public static final String MOD_ID = "minecraft_mod_test";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MinecraftModTest(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(RpgStatusCommand::register);
        LOGGER.info("Minecraft Mod Test carregado (Minecraft 1.21.1 / NeoForge).");
    }
}
