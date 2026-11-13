package dev.victorasbrito.minecraftmodtest.battle.client;

import dev.victorasbrito.minecraftmodtest.battle.network.BattleStatePayload;
import net.minecraft.client.Minecraft;

/** Isolado no lado fisico do cliente; nao usado pelo servidor dedicado. */
public final class ClientBattleHandler {
    private ClientBattleHandler() {}

    public static void receive(BattleStatePayload snapshot) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.player == null) return;
            minecraft.setScreen(new BattleScreen(snapshot));
        });
    }
}
