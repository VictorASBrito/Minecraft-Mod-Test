package dev.victorasbrito.minecraftmodtest.battle.client;

import dev.victorasbrito.minecraftmodtest.battle.network.BattleResultPayload;
import dev.victorasbrito.minecraftmodtest.progression.ExperienceCurve;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Painel de resultados APOS encerrar a sessao e restaurar a camera.
 * Apenas exibe o comprovante do servidor; nunca calcula ou concede recompensas.
 */
public final class BattleResultsScreen extends Screen {
    private static final int GOLD = 0xFFF2D18A;
    private static final int MUTED = 0xFFB0B8B3;
    private static final int WHITE = 0xFFF4F1E9;
    private final BattleResultPayload receipt;
    private int animationTicks;

    public BattleResultsScreen(BattleResultPayload receipt) {
        super(Component.literal("Resultado do combate"));
        this.receipt = receipt;
    }

    @Override
    protected void init() {
        super.init();
        int cardWidth = Math.min(440, width - 24);
        int cardHeight = Math.min(314, height - 24);
        int x = (width - cardWidth) / 2;
        int y = (height - cardHeight) / 2;
        addRenderableWidget(Button.builder(
                Component.literal("Continuar"),
                button -> onClose()
        ).bounds(x + Math.max(8, (cardWidth - 136) / 2),
                y + cardHeight - 32, Math.min(136, cardWidth - 16), 22).build());
    }

    @Override
    public void tick() {
        super.tick();
        if (animationTicks < 60) animationTicks++;
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Em servidor multiplayer, o mundo continua normalmente.
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Desenho proprio: evita o blur vanilla sobre a tela de resultados.
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int cardWidth = Math.min(440, width - 24);
        int cardHeight = Math.min(314, height - 24);
        int x = (width - cardWidth) / 2;
        int y = (height - cardHeight) / 2;

        g.fill(0, 0, width, height, 0xC900080D);
        panel(g, x, y, cardWidth, cardHeight);

        String headline = switch (receipt.outcome()) {
            case "VICTORY" -> "VITÓRIA";
            case "DEFEAT" -> "DERROTA";
            case "FLED" -> "RETIRADA";
            default -> "BATALHA ENCERRADA";
        };
        g.drawCenteredString(font, headline, width / 2, y + 13, GOLD);
        g.fill(x + 14, y + 30, x + cardWidth - 14, y + 31, 0xFF725F40);

        int currentTotal = animatedExperience();
        var current = ExperienceCurve.progress(currentTotal);
        int beforeLevel = ExperienceCurve.progress(receipt.totalXpBefore()).level();
        int afterLevel = ExperienceCurve.progress(receipt.totalXpAfter()).level();
        int left = x + 19;
        int right = x + cardWidth - 19;

        g.drawString(font, "EXPERIÊNCIA DE RPG", left, y + 42, WHITE, false);
        g.drawString(font, "+" + receipt.awardedXp() + " XP", right - font.width(
                "+" + receipt.awardedXp() + " XP"), y + 42, GOLD, false);

        String status = current.level() == ExperienceCurve.MAX_LEVEL
                ? "Nível " + current.level() + " (máximo)"
                : "Nível " + current.level() + "   " + current.xpInLevel()
                        + "/" + current.xpToNextLevel() + " XP";
        g.drawString(font, status, left, y + 59, MUTED, false);
        progressBar(g, left, y + 72, Math.max(10, right - left), 9,
                current.xpInLevel(), current.xpToNextLevel(), 0xFF6BC39A);

        int levelLine = y + 90;
        if (afterLevel > beforeLevel && current.level() > beforeLevel) {
            g.drawCenteredString(font, "NOVO NÍVEL! " + beforeLevel + "  >  " + afterLevel,
                    width / 2, levelLine, GOLD);
        } else if (receipt.awardedXp() == 0) {
            g.drawCenteredString(font, "Nenhuma experiência recebida.",
                    width / 2, levelLine, MUTED);
        } else {
            g.drawCenteredString(font, "Experiência adicionada ao seu personagem.",
                    width / 2, levelLine, MUTED);
        }

        int sectionY = y + 115;
        g.fill(left, sectionY - 7, right, sectionY - 6, 0xFF725F40);
        g.drawString(font, "ITENS OBTIDOS", left, sectionY, WHITE, false);
        if (receipt.items().isEmpty()) {
            g.drawString(font, "Nenhum item recebido.", left, sectionY + 21, MUTED, false);
        } else {
            int i = 0;
            for (BattleResultPayload.LootEntry entry : receipt.items()) {
                if (i >= 8) break;
                int col = i % 2;
                int row = i / 2;
                int itemX = left + col * Math.max(110, (right - left) / 2);
                int itemY = sectionY + 18 + row * 25;
                g.fill(itemX - 2, itemY - 1, itemX + 21, itemY + 19, 0xB32C312F);
                ItemStack icon = stackFor(entry);
                g.renderItem(icon, itemX + 1, itemY + 1);
                String label = font.plainSubstrByWidth(
                        icon.getHoverName().getString() + " x" + entry.count(),
                        Math.max(44, (right - left) / 2 - 29));
                g.drawString(font, label, itemX + 24, itemY + 5, WHITE, false);
                i++;
            }
        }

        if (!receipt.items().isEmpty()) {
            g.drawCenteredString(font, "Inventário cheio: o excedente cai no chão.",
                    width / 2, y + cardHeight - 48, MUTED);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    private int animatedExperience() {
        int delta = receipt.totalXpAfter() - receipt.totalXpBefore();
        double fraction = Math.min(1.0, animationTicks / 36.0);
        return receipt.totalXpBefore() + (int) Math.round(delta * fraction);
    }

    private static ItemStack stackFor(BattleResultPayload.LootEntry entry) {
        ResourceLocation key = ResourceLocation.tryParse(entry.itemId());
        Item item = key == null ? null : BuiltInRegistries.ITEM.get(key);
        return item == null ? ItemStack.EMPTY : new ItemStack(item, entry.count());
    }

    private static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xE30C1B22);
        g.fill(x, y, x + w, y + 2, 0xFFAC8954);
        g.fill(x, y + h - 2, x + w, y + h, 0xFFAC8954);
        g.fill(x, y, x + 2, y + h, 0xFFAC8954);
        g.fill(x + w - 2, y, x + w, y + h, 0xFFAC8954);
    }

    private static void progressBar(GuiGraphics g, int x, int y, int w, int h,
                                    int current, int maximum, int color) {
        g.fill(x, y, x + w, y + h, 0xFF24352E);
        if (maximum > 0) {
            int fill = Math.max(0, Math.min(w, w * current / maximum));
            g.fill(x, y, x + fill, y + h, color);
        } else {
            g.fill(x, y, x + w, y + h, color);
        }
    }
}
