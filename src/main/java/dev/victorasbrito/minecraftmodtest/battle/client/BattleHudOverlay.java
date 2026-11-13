package dev.victorasbrito.minecraftmodtest.battle.client;

import dev.victorasbrito.minecraftmodtest.MinecraftModTest;
import dev.victorasbrito.minecraftmodtest.battle.network.BattleStatePayload;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * HUD de combate desenhada sobre o mundo 3D: nenhum menu Screen, camera ou
 * entidade e alterado aqui. O inimigo NAO revela HP, nem em texto nem em barra.
 *
 * Este MVP possui apenas dois participantes no motor. Os retratos sao gerados
 * a partir da previsao de turnos e ficam preparados para receber IDs tipados
 * quando implementarmos grupos/refo rcos.
 */
public final class BattleHudOverlay {
    private static final int BG = 0xC60B1920;
    private static final int BG_LIGHT = 0xC9222930;
    private static final int BORDER = 0xFF947348;
    private static final int GOLD = 0xFFF0CC81;
    private static final int GOLD_DIM = 0xFFAE9162;
    private static final int WHITE = 0xFFF3ECDD;
    private static final int MUTED = 0xFF9CA5A2;
    private static final int HP = 0xFF57BA83;
    private static final int HP_EMPTY = 0xFF232B29;
    private static final ResourceLocation ZOMBIE_SKIN =
            ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png");

    private static final ItemStack ATTACK_ICON = new ItemStack(Items.IRON_SWORD);
    private static final ItemStack SKILL_ICON = new ItemStack(Items.ENCHANTED_BOOK);
    private static final ItemStack GUARD_ICON = new ItemStack(Items.SHIELD);
    private static final ItemStack ITEM_ICON = new ItemStack(Items.POTION);
    private static final ItemStack FLEE_ICON = new ItemStack(Items.LEATHER_BOOTS);

    private BattleHudOverlay() {
    }

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(MinecraftModTest.MOD_ID, "battle_hud"),
                (graphics, delta) -> render(graphics)
        );
    }

    /** Oculta HUD vanilla sobreposto, mas preserva chat, mensagens e debug. */
    public static void hideVanillaBattleLayers(RenderGuiLayerEvent.Pre event) {
        if (ClientBattleHandler.state() == null) return;
        ResourceLocation name = event.getName();
        if (VanillaGuiLayers.CROSSHAIR.equals(name)
                || VanillaGuiLayers.HOTBAR.equals(name)
                || VanillaGuiLayers.PLAYER_HEALTH.equals(name)
                || VanillaGuiLayers.ARMOR_LEVEL.equals(name)
                || VanillaGuiLayers.FOOD_LEVEL.equals(name)
                || VanillaGuiLayers.AIR_LEVEL.equals(name)
                || VanillaGuiLayers.EXPERIENCE_BAR.equals(name)
                || VanillaGuiLayers.EXPERIENCE_LEVEL.equals(name)
                || VanillaGuiLayers.SELECTED_ITEM_NAME.equals(name)) {
            event.setCanceled(true);
        }
    }

    private static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        BattleStatePayload state = ClientBattleHandler.state();
        if (state == null || mc.player == null || mc.screen != null) return;

        int w = g.guiWidth();
        int h = g.guiHeight();
        if (w < 240 || h < 160) return;

        drawTurnPortraits(g, mc, state, w);
        BattleHudLayout.Layout layout = BattleHudLayout.forScreen(w, h);
        drawPlayerStatus(g, mc, state, layout.status(), layout.compact());
        drawActions(g, mc, state, layout.actions(), layout.compact());

        // Log discreto, sem painéis centrais grandes que escondam o terreno.
        String message = state.finished() ? resultName(state.outcome()) : state.combatLog();
        if (message != null && !message.isBlank()) {
            int maxWidth = Math.min(w - 24, 360);
            String line = mc.font.plainSubstrByWidth(message, maxWidth - 18);
            int textWidth = mc.font.width(line);
            int cx = w / 2;
            int y = layout.compact() ? layout.status().y() - 20 : layout.actions().y() - 22;
            y = Math.max(56, y);
            g.fill(cx - textWidth / 2 - 7, y - 4, cx + textWidth / 2 + 7, y + 14, 0x9A09161D);
            g.drawCenteredString(mc.font, line, cx, y, state.finished() ? GOLD : WHITE);
        }
    }

    private static void drawTurnPortraits(GuiGraphics g, Minecraft mc,
                                           BattleStatePayload state, int screenWidth) {
        String[] actors = state.turnPreview().split(" > ");
        int size = screenWidth < 480 ? 20 : 26;
        int step = size + 11;
        int count = Math.min(6, Math.min(actors.length, (screenWidth - 28) / step));
        if (count <= 0) return;

        int x = 12;
        int y = 10;
        int panelWidth = count * step + 13;
        panel(g, x - 5, y - 5, panelWidth, size + 20, false);

        for (int i = 0; i < count; i++) {
            int px = x + i * step;
            boolean next = i == 0;
            g.fill(px - 2, y - 2, px + size + 2, y + size + 2, next ? GOLD : BORDER);
            g.fill(px, y, px + size, y + size, 0xFF17242A);
            drawPortrait(g, mc, actors[i].trim(), px + 1, y + 1, size - 2);
            if (i < count - 1) {
                g.drawCenteredString(mc.font, ">", px + size + 6, y + size / 2 - 4, GOLD_DIM);
            }
            if (next) {
                // Triangulo/ponteiro vertical sob o retrato do proximo ator.
                int mid = px + size / 2;
                g.fill(mid - 5, y + size + 5, mid + 6, y + size + 7, GOLD);
                g.fill(mid - 3, y + size + 7, mid + 4, y + size + 9, GOLD);
                g.fill(mid - 1, y + size + 9, mid + 2, y + size + 11, GOLD);
            }
        }
    }

    /**
     * Cliente conhece somente a propria skin e a textura do zumbi no MVP 1x1.
     * Outras especies e outros jogadores receberao retratos por UUID/tipo de
     * entidade no futuro, nao inferidos pelo nome localizado.
     */
    private static void drawPortrait(GuiGraphics g, Minecraft mc, String actor,
                                      int x, int y, int size) {
        String label = actor.toLowerCase(Locale.ROOT);
        if (label.equals("voce") || label.equals("você") || label.equals("you")) {
            ResourceLocation skin = mc.player.getSkin().texture();
            g.blit(skin, x, y, size, size, 8.0f, 8.0f, 8, 8, 64, 64);
            g.blit(skin, x, y, size, size, 40.0f, 8.0f, 8, 8, 64, 64);
        } else if (label.equals("zumbi") || label.equals("zombie")) {
            g.blit(ZOMBIE_SKIN, x, y, size, size, 8.0f, 8.0f, 8, 8, 64, 64);
        } else {
            // Fallback discreto enquanto nao temos rede com IDs de entidade.
            g.fill(x, y, x + size, y + size, 0xFF3B4240);
            g.drawCenteredString(mc.font, "?", x + size / 2, y + size / 2 - 4, GOLD);
        }
    }

    private static void drawPlayerStatus(GuiGraphics g, Minecraft mc,
                                         BattleStatePayload state, BattleHudLayout.Rect box,
                                         boolean compact) {
        panel(g, box.x(), box.y(), box.width(), box.height(), false);
        int head = compact ? 26 : 36;
        int px = box.x() + 8;
        int py = box.y() + (box.height() - head) / 2;
        g.fill(px - 1, py - 1, px + head + 1, py + head + 1, BORDER);
        drawPortrait(g, mc, "Você", px, py, head);

        int sx = px + head + 10;
        int available = Math.max(24, box.right() - sx - 10);
        String name = mc.font.plainSubstrByWidth(mc.player.getName().getString(), available);
        g.drawString(mc.font, name, sx, box.y() + 8, WHITE, false);

        int hpTextY = compact ? box.y() + 23 : box.y() + 27;
        g.drawString(mc.font, "PV " + state.playerHp() + "/" + state.playerMaxHp(),
                sx, hpTextY, GOLD, false);
        int barY = compact ? box.y() + 37 : box.y() + 43;
        progressBar(g, sx, barY, available, compact ? 6 : 8,
                state.playerHp(), state.playerMaxHp(), HP);

        if (!compact) {
            String condition = state.guarding() ? "DEFENDENDO" : "SEM EFEITOS";
            g.drawString(mc.font, condition, sx, box.y() + 56,
                    state.guarding() ? GOLD : MUTED, false);
        }
    }

    private static void drawActions(GuiGraphics g, Minecraft mc,
                                    BattleStatePayload state, BattleHudLayout.Rect box,
                                    boolean compact) {
        int gap = 5;
        int width = (box.width() - gap * 4) / 5;
        int x = box.x();
        int y = box.y();
        boolean turn = state.playerTurn() && !state.finished();

        ActionEntry[] entries = {
                new ActionEntry("Atacar", "J", ATTACK_ICON, true),
                new ActionEntry("Habilidade", "-", SKILL_ICON, false),
                new ActionEntry("Defender", "K", GUARD_ICON, true),
                new ActionEntry("Item", "-", ITEM_ICON, false),
                new ActionEntry("Fugir", "L", FLEE_ICON, true)
        };

        for (int i = 0; i < entries.length; i++) {
            ActionEntry action = entries[i];
            int cardX = x + i * (width + gap);
            boolean enabled = action.available() && turn;
            boolean highlighted = i == 0 && enabled;
            panel(g, cardX, y, width, box.height(), highlighted);
            if (!action.available() || !turn) {
                g.fill(cardX + 2, y + 2, cardX + width - 2,
                        y + box.height() - 2, 0x54101115);
            }

            int iconX = cardX + (width - 16) / 2;
            g.renderItem(action.icon(), iconX, y + (compact ? 4 : 7));
            String caption = action.label();
            if (mc.font.width(caption) > width - 6) {
                caption = action.label().equals("Habilidade") ? "Habil." : caption;
                caption = mc.font.plainSubstrByWidth(caption, width - 6);
            }
            int captionY = y + (compact ? 23 : 31);
            int color = enabled ? WHITE : MUTED;
            g.drawCenteredString(mc.font, caption, cardX + width / 2, captionY, color);

            String key = action.available() ? "[" + action.key() + "]" : "Em breve";
            if (!action.available() && width < 65) key = "-";
            g.drawCenteredString(mc.font, key, cardX + width / 2,
                    y + box.height() - 13, highlighted ? GOLD : MUTED);
        }
    }

    private static void panel(GuiGraphics g, int x, int y, int width, int height,
                              boolean highlighted) {
        if (width <= 3 || height <= 3) return;
        g.fill(x, y, x + width, y + height, highlighted ? 0xE02C251C : BG);
        int edge = highlighted ? GOLD : BORDER;
        g.fill(x, y, x + width, y + 1, edge);
        g.fill(x, y + height - 1, x + width, y + height, edge);
        g.fill(x, y, x + 1, y + height, edge);
        g.fill(x + width - 1, y, x + width, y + height, edge);
        g.fill(x + 3, y + 3, x + width - 3, y + 4,
                highlighted ? GOLD_DIM : 0x774D665E);
        // Ornamentos discretos nos cantos, sem textura externa.
        g.fill(x + 1, y + 1, x + 5, y + 3, GOLD_DIM);
        g.fill(x + width - 5, y + 1, x + width - 1, y + 3, GOLD_DIM);
        g.fill(x + 1, y + height - 3, x + 5, y + height - 1, GOLD_DIM);
        g.fill(x + width - 5, y + height - 3,
                x + width - 1, y + height - 1, GOLD_DIM);
    }

    private static void progressBar(GuiGraphics g, int x, int y, int width,
                                    int height, int value, int maximum, int color) {
        g.fill(x, y, x + width, y + height, HP_EMPTY);
        int filled = maximum <= 0 ? 0
                : (int) Math.round(width * (Math.max(0, Math.min(value, maximum))
                / (double) maximum));
        if (filled > 0) g.fill(x, y, x + filled, y + height, color);
    }

    private static String resultName(String outcome) {
        return switch (outcome) {
            case "VICTORY" -> "VITÓRIA! Recompensas serão implementadas na próxima etapa.";
            case "DEFEAT" -> "DERROTA";
            case "FLED" -> "BATALHA ENCERRADA";
            default -> "";
        };
    }

    private record ActionEntry(String label, String key, ItemStack icon,
                               boolean available) {
    }
}
