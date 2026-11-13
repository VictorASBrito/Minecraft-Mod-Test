package dev.victorasbrito.minecraftmodtest;

import com.mojang.logging.LogUtils;
import dev.victorasbrito.minecraftmodtest.command.RpgStatusCommand;
import dev.victorasbrito.minecraftmodtest.battle.command.BattleCommands;
import dev.victorasbrito.minecraftmodtest.battle.network.BattleNetwork;
import dev.victorasbrito.minecraftmodtest.battle.server.BattleSessionManager;
import dev.victorasbrito.minecraftmodtest.battle.server.BattleWorldFixture;
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
        NeoForge.EVENT_BUS.addListener(BattleCommands::register);
        NeoForge.EVENT_BUS.addListener(BattleSessionManager::onLogout);
        NeoForge.EVENT_BUS.addListener(BattleSessionManager::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(BattleSessionManager::onInvulnerabilityCheck);
        NeoForge.EVENT_BUS.addListener(BattleWorldFixture::onJoin);
        NeoForge.EVENT_BUS.addListener(BattleSessionManager::onServerStopped);
        modEventBus.addListener(BattleNetwork::register);
        LOGGER.info("Minecraft Mod Test carregado (Minecraft 1.21.1 / NeoForge).");
    }
}
