package dev.victorasbrito.minecraftmodtest.battle.client;

import dev.victorasbrito.minecraftmodtest.battle.network.BattleActionPayload;
import dev.victorasbrito.minecraftmodtest.battle.network.BattleStatePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Interface jogavel de teste. As duas perspectivas sao disposicoes 2D
 * distintas. Camera 3D e modelos de entidades sao um marco posterior.
 */
public final class BattleScreen extends Screen {
    private final BattleStatePayload snapshot;
    private final BattleViewPreference.View view;
    private boolean submitted;

    public BattleScreen(BattleStatePayload snapshot) {
        super(Component.literal("Minecraft Mod Test | Batalha"));
        this.snapshot = snapshot;
        this.view = BattleViewPreference.get();
    }

    @Override
    protected void init() {
        super.init();
        int middle = this.width / 2;
        int bottom = this.height - 45;

        this.addRenderableWidget(Button.builder(
                Component.literal(view == BattleViewPreference.View.ELEVATED ? "Visao: Elevada" : "Visao: Atrás"),
                button -> {
                    BattleViewPreference.toggle();
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new BattleScreen(snapshot));
                    }
                }).bounds(Math.max(8, this.width - 144), 12, 134, 20).build());

        if (snapshot.finished()) {
            this.addRenderableWidget(Button.builder(Component.literal("Fechar"),
                    button -> {
                        if (this.minecraft != null) this.minecraft.setScreen(null);
                    }).bounds(middle - 65, bottom, 130, 22).build());
        } else if (snapshot.playerTurn()) {
            int gap = 8;
            this.addRenderableWidget(Button.builder(Component.literal("Atacar"),
                    button -> submit("ATTACK"))
                    .bounds(middle - 154, bottom, 98, 22).build());
            this.addRenderableWidget(Button.builder(Component.literal("Defender"),
                    button -> submit("GUARD"))
                    .bounds(middle - 49, bottom, 98, 22).build());
            this.addRenderableWidget(Button.builder(Component.literal("Sair do teste"),
                    button -> submit("FLEE"))
                    .bounds(middle + 56, bottom, 98, 22).build());
        }
    }

    private void submit(String action) {
        if (submitted || snapshot.finished() || !snapshot.playerTurn()) return;
        submitted = true;
        PacketDistributor.sendToServer(new BattleActionPayload(
                snapshot.sessionId(), snapshot.turnId(), action
        ));
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Nunca paralisa o mundo nem as outras batalhas.
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false; // Sair por "Sair do teste" para encerrar no servidor.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xEF0A1520);
        graphics.drawCenteredString(font, "MINECRAFT MOD TEST | BATALHA POR TURNOS",
                width / 2, 18, 0xF1E2AE);
        graphics.drawCenteredString(font, "Velocidade: Voce " + snapshot.playerSpeed()
                        + " | Zumbi " + snapshot.enemySpeed(),
                width / 2, 36, 0xB8C9D3);

        int left = Math.max(12, width / 2 - Math.min(width / 2 - 12, 250));
        int right = Math.min(width - 12, width / 2 + Math.min(width / 2 - 12, 250));
        int top = 57;
        int bottom = Math.max(top + 75, height - 113);
        graphics.fill(left, top, right, bottom, 0xFF1F3E40);
        graphics.fill(left + 3, bottom - 21, right - 3, bottom - 3, 0xFF4B6146);

        if (view == BattleViewPreference.View.ELEVATED) {
            renderElevated(graphics, left, right, top, bottom);
        } else {
            renderBehind(graphics, left, right, top, bottom);
        }

        int statsY = Math.max(top + 10, height - 104);
        graphics.drawString(font, "VOCE  HP " + snapshot.playerHp() + "/" + snapshot.playerMaxHp()
                + (snapshot.guarding() ? " [DEFENDENDO]" : ""), left, statsY, 0xB9EDC6);
        drawBar(graphics, left, statsY + 12, 150, 7,
                snapshot.playerHp(), snapshot.playerMaxHp(), 0xFF5EA97B);

        graphics.drawString(font, "ZUMBI  HP " + snapshot.enemyHp() + "/" + snapshot.enemyMaxHp(),
                right - 150, statsY, 0xF3C3B8);
        drawBar(graphics, right - 150, statsY + 12, 150, 7,
                snapshot.enemyHp(), snapshot.enemyMaxHp(), 0xFFCA755E);

        String timeline = snapshot.turnPreview();
        graphics.drawCenteredString(font,
                font.plainSubstrByWidth("INICIATIVA: " + timeline, Math.max(90, width - 30)),
                width / 2, Math.max(top, height - 81), 0xF1D38B);

        String log = font.plainSubstrByWidth(snapshot.combatLog(), Math.max(90, width - 30));
        graphics.drawCenteredString(font, log, width / 2, Math.max(top, height - 64), 0xE1E8EE);
        if (snapshot.finished()) {
            String outcome = switch (snapshot.outcome()) {
                case "VICTORY" -> "VITORIA";
                case "DEFEAT" -> "DERROTA";
                default -> "BATALHA ENCERRADA";
            };
            graphics.drawCenteredString(font, outcome, width / 2, top + 10, 0xFFE8D58B);
        }

        super.render(graphics, mouseX, mouseY, delta);
    }

    private void renderElevated(GuiGraphics g, int left, int right, int top, int bottom) {
        int area = right - left;
        g.drawString(font, "VISAO LATERAL ELEVADA - CENARIO PROVISORIO", left + 10, top + 6, 0xBCC9D0);
        figure(g, left + area * 24 / 100, bottom - 40, 0xFF8DAA7E, 18);
        figure(g, left + area * 76 / 100, bottom - 45, 0xFF608ED0, 22);
        g.drawCenteredString(font, "Zumbi", left + area * 24 / 100, bottom - 55, 0xE7E7D2);
        g.drawCenteredString(font, "Voce", left + area * 76 / 100, bottom - 60, 0xE1EFFA);
    }

    private void renderBehind(GuiGraphics g, int left, int right, int top, int bottom) {
        int center = (left + right) / 2;
        g.drawString(font, "VISAO POR TRAS - CENARIO PROVISORIO", left + 10, top + 6, 0xBCC9D0);
        figure(g, center, top + (bottom - top) / 2 - 3, 0xFF8DAA7E, 15);
        figure(g, center, bottom - 40, 0xFF608ED0, 36);
        g.drawCenteredString(font, "Zumbi", center, top + (bottom - top) / 2 - 25, 0xE7E7D2);
    }

    private static void figure(GuiGraphics g, int center, int bottom, int color, int size) {
        int bodyWidth = size;
        int bodyHeight = size + 8;
        g.fill(center - size / 3, bottom - bodyHeight - size / 2,
                center + size / 3, bottom - bodyHeight + size / 6, 0xFFD8BD9B);
        g.fill(center - bodyWidth / 2, bottom - bodyHeight,
                center + bodyWidth / 2, bottom, color);
        g.fill(center - bodyWidth / 2, bottom - 3,
                center - 2, bottom + 2, 0xFF27364A);
        g.fill(center + 2, bottom - 3,
                center + bodyWidth / 2, bottom + 2, 0xFF27364A);
    }

    private static void drawBar(GuiGraphics g, int x, int y, int width, int height,
                                int value, int max, int color) {
        g.fill(x, y, x + width, y + height, 0xFF19252B);
        int filled = max == 0 ? 0 : Math.max(0, Math.min(width, width * value / max));
        g.fill(x, y, x + filled, y + height, color);
    }
}
