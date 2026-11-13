package dev.victorasbrito.minecraftmodtest.battle.network;

import dev.victorasbrito.minecraftmodtest.battle.server.BattleSessionManager;
import dev.victorasbrito.minecraftmodtest.battle.client.ClientBattleHandler;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Pacotes obrigatorios para clientes modded, com handlers no game thread.
 */
public final class BattleNetwork {
    private BattleNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(BattleActionPayload.TYPE, BattleActionPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) {
                        BattleSessionManager.onAction(player, payload);
                    }
                });

        registrar.playToClient(BattleStatePayload.TYPE, BattleStatePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (FMLEnvironment.dist == Dist.CLIENT) {
                        ClientBattleHandler.receive(payload);
                    }
                });
    }
}
