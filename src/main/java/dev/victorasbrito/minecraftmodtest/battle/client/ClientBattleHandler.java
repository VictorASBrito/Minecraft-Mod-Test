package dev.victorasbrito.minecraftmodtest.battle.client;

import dev.victorasbrito.minecraftmodtest.battle.network.BattleStatePayload;
import net.minecraft.client.Minecraft;

/**
 * Mantem o ultimo snapshot para HUD e camera. Nunca abre uma Screen.
 * A batalha se desenrola visualmente no proprio mapa carregado.
 */
public final class ClientBattleHandler {
    private static BattleStatePayload state;
    private static int resultTicks;

    private ClientBattleHandler() {}

    public static void receive(BattleStatePayload snapshot) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.player == null || minecraft.level == null) return;
            state = snapshot;
            resultTicks = snapshot.finished() ? 100 : 0;
            ClientBattleCamera.update(snapshot);
        });
    }

    public static BattleStatePayload state() {
        return state;
    }

    public static boolean active() {
        return state != null && !state.finished();
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            clear();
            return;
        }
        if (state != null && state.finished()) {
            if (--resultTicks <= 0) clear();
        } else if (active()) {
            ClientBattleCamera.reposition(state);
        }
    }

    public static void clear() {
        ClientBattleCamera.restore();
        state = null;
        resultTicks = 0;
    }
}
