package dev.victorasbrito.minecraftmodtest.battle.client;

import dev.victorasbrito.minecraftmodtest.MinecraftModTest;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/** Entrada carregada somente no cliente fisico, segura para servidores dedicados. */
@Mod(value = MinecraftModTest.MOD_ID, dist = Dist.CLIENT)
public final class MinecraftModTestClient {
    public MinecraftModTestClient(IEventBus modEventBus) {
        modEventBus.addListener(BattleHudOverlay::register);
        modEventBus.addListener(ClientBattleControls::registerKeys);
        NeoForge.EVENT_BUS.addListener(ClientBattleControls::onTick);
        NeoForge.EVENT_BUS.addListener(ClientBattleControls::onMovementInput);
        NeoForge.EVENT_BUS.addListener(ClientBattleControls::onInteractionKey);
        NeoForge.EVENT_BUS.addListener(ClientBattleControls::onMouseButton);
        NeoForge.EVENT_BUS.addListener(ClientBattleControls::onMouseScroll);
        NeoForge.EVENT_BUS.addListener(ClientBattleControls::onScreenOpening);
        NeoForge.EVENT_BUS.addListener(BattleHudOverlay::hideVanillaBattleLayers);
    }
}
