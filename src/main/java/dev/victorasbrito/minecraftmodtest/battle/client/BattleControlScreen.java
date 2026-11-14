package dev.victorasbrito.minecraftmodtest.battle.client;

import dev.victorasbrito.minecraftmodtest.battle.network.BattleStatePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Captura mouse e teclado sem substituir o mundo 3D por uma arena GUI.
 *
 * Screen libera o cursor pelo fluxo normal de Minecraft. A renderizacao do
 * fundo foi desativada de proposito: a camera do combate continua mostrando
 * jogador, inimigo e terreno do mundo real, e a HUD e desenhada por cima.
 */
public final class BattleControlScreen extends Screen {
    public BattleControlScreen() {
        super(Component.literal("Comandos de combate"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Sem desfoque e sem painel em tela cheia: o mundo permanece visivel.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        BattleHudOverlay.renderForScreen(graphics);
        BattleStatePayload state = ClientBattleHandler.state();
        if (state != null && state.playerTurn() && !state.finished()) {
            int hovered = BattleHudLayout.actionIndexAt(width, height, mouseX, mouseY);
            if (hovered == 0 || hovered == 2 || hovered == 4) {
                BattleHudLayout.Rect card = BattleHudLayout.actionCard(width, height, hovered);
                int color = 0xFFF7DB9F;
                graphics.fill(card.x(), card.y(), card.right(), card.y() + 2, color);
                graphics.fill(card.x(), card.bottom() - 2, card.right(), card.bottom(), color);
                graphics.fill(card.x(), card.y(), card.x() + 2, card.bottom(), color);
                graphics.fill(card.right() - 2, card.y(), card.right(), card.bottom(), color);
            }
        }
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int index = BattleHudLayout.actionIndexAt(width, height, mouseX, mouseY);
            switch (index) {
                case 0 -> ClientBattleControls.submitAction("ATTACK");
                case 2 -> ClientBattleControls.submitAction("GUARD");
                case 4 -> ClientBattleControls.submitAction("FLEE");
                default -> { /* Itens e habilidades permanecem indisponiveis. */ }
            }
            return true; // Clique nunca deve atingir blocos, mobs ou itens vanilla.
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            // O jogador precisa escolher Fugir (L ou mouse) para sair da sessao.
            return true;
        }
        if (ClientBattleControls.handleScreenKey(keyCode, scanCode)) {
            return true;
        }
        // Nao reencaminhar teclas ao controle vanilla durante o combate.
        return true;
    }

    @Override
    public void onClose() {
        // Outro mod/caminho de fechamento nao deve liberar a camera e o
        // personagem sem encerrar a sessao no servidor.
        if (ClientBattleHandler.active()) {
            ClientBattleControls.submitAction("FLEE");
        } else {
            super.onClose();
        }
    }
}
