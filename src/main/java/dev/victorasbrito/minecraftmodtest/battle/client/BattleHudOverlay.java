package dev.victorasbrito.minecraftmodtest.battle.client;

import dev.victorasbrito.minecraftmodtest.MinecraftModTest;
import dev.victorasbrito.minecraftmodtest.battle.network.BattleStatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * Interface leve sobre o proprio mundo. Sem Screen, sem fundo pintado sobre o mapa.
 */
public final class BattleHudOverlay {
    private BattleHudOverlay() {}

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(MinecraftModTest.MOD_ID, "battle_hud"),
                (graphics, delta) -> render(graphics)
        );
    }

    private static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        BattleStatePayload s = ClientBattleHandler.state();
        if (s == null || mc.player == null || mc.screen != null) return;

        int w = g.guiWidth();
        int h = g.guiHeight();
        int mid = w / 2;
        boolean behind = BattleViewPreference.get() == BattleViewPreference.View.BEHIND;

        g.fill(mid - 184, 7, mid + 184, 43, 0xC8091420);
        g.drawCenteredString(mc.font, "BATALHA | " + (behind ? "CAMERA ATRAS" : "LATERAL ELEVADA") + "  [V: Alternar]",
                mid, 13, 0xFFF4DEB1);
        g.drawCenteredString(mc.font, "ORDEM: " + mc.font.plainSubstrByWidth(s.turnPreview(), 346),
                mid, 28, 0xFFF0F2F4);

        int base = h - 65;
        g.fill(mid - 220, base - 14, mid + 220, h - 5, 0xC8091420);

        g.drawString(mc.font, "VOCE: " + s.playerHp() + "/" + s.playerMaxHp(),
                mid - 210, base - 7, 0xFF9CE4B3);
        g.drawString(mc.font, "ZUMBI: " + s.enemyHp() + "/" + s.enemyMaxHp(),
                mid + 90, base - 7, 0xFFEBAD9C);
        progressBar(g, mid - 210, base + 5, 130, s.playerHp(), s.playerMaxHp(), 0xFF49BA82);
        progressBar(g, mid + 90, base + 5, 130, s.enemyHp(), s.enemyMaxHp(), 0xFFE27761);

        String message = s.finished() ? "RESULTADO: " + s.outcome() : s.combatLog();
        g.drawCenteredString(mc.font, mc.font.plainSubstrByWidth(message, 410),
                mid, base + 24, 0xFFEBDCB2);

        if (s.playerTurn() && !s.finished()) {
            if (behind) {
                g.fill(mid - 210, h / 2 - 25, mid - 72, h / 2 + 45, 0xB9091420);
                g.drawCenteredString(mc.font, "[J] ATACAR", mid - 141, h / 2 - 12, 0xFFF1E2B2);
                g.drawCenteredString(mc.font, "[K] DEFENDER", mid - 141, h / 2 + 6, 0xFFE1E9F1);
                g.drawCenteredString(mc.font, "[L] FUGIR", mid - 141, h / 2 + 24, 0xFFE1E9F1);
            } else {
                g.fill(mid - 150, h - 110, mid + 150, h - 81, 0xB9091420);
                g.drawCenteredString(mc.font, "[J] ATACAR      [K] DEFENDER      [L] FUGIR",
                        mid, h - 101, 0xFFF1E2B2);
            }
        }
    }

    private static void progressBar(GuiGraphics g, int x, int y, int width, int value, int max, int color) {
        g.fill(x, y, x + width, y + 7, 0xFF14212D);
        int filled = max <= 0 ? 0 : Math.max(0, Math.min(width, (width * value) / max));
        g.fill(x, y, x + filled, y + 7, color);
    }
}
