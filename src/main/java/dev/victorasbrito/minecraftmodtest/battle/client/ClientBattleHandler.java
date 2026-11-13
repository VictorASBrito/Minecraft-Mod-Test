package dev.victorasbrito.minecraftmodtest.battle.client;

import dev.victorasbrito.minecraftmodtest.battle.network.BattleStatePayload;
import net.minecraft.client.Minecraft;
import dev.victorasbrito.minecraftmodtest.battle.engine.BattleTargeting;

/**
 * Mantem o ultimo snapshot para HUD e camera. Nunca abre uma Screen.
 * A batalha se desenrola visualmente no proprio mapa carregado.
 */
public final class ClientBattleHandler {
    private static BattleStatePayload state;
    private static int resultTicks;
    private static String appliedSessionId = "";
    private static int appliedActionSequence;
    private static int attackFocusTicks;

    private ClientBattleHandler() {}

    public static void receive(BattleStatePayload snapshot) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.player == null || minecraft.level == null) return;
            if (!snapshot.sessionId().equals(appliedSessionId)) {
                appliedSessionId = snapshot.sessionId();
                appliedActionSequence = 0;
                attackFocusTicks = 0;
            }
            if (snapshot.actionSequence() > appliedActionSequence) {
                appliedActionSequence = snapshot.actionSequence();
                if ("ATTACK".equals(snapshot.resolvedAction())) {
                    // Foco do personagem, nao da camera: o mouse continua livre.
                    attackFocusTicks = 10;
                    faceTarget(snapshot);
                }
            }
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
        if (active() && attackFocusTicks > 0) {
            faceTarget(state);
            attackFocusTicks--;
        }
    }

    private static void faceTarget(BattleStatePayload state) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        float yaw = BattleTargeting.yawTowards(
                state.playerX(), state.playerZ(), state.enemyX(), state.enemyZ());
        minecraft.player.setYRot(yaw);
        minecraft.player.setYHeadRot(yaw);
        minecraft.player.setYBodyRot(yaw);
    }

    public static void clear() {
        ClientBattleCamera.restore();
        state = null;
        resultTicks = 0;
        appliedSessionId = "";
        appliedActionSequence = 0;
        attackFocusTicks = 0;
    }
}
