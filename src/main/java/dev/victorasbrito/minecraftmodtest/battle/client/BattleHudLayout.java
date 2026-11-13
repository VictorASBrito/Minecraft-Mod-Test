package dev.victorasbrito.minecraftmodtest.battle.client;

/**
 * Geometria pura para adaptar a HUD a resolucoes/GUI scales diferentes.
 * Nada aqui depende do renderizador Minecraft.
 */
public final class BattleHudLayout {
    private BattleHudLayout() {
    }

    public static Layout forScreen(int width, int height) {
        if (width < 240 || height < 160) {
            throw new IllegalArgumentException("Tela pequena demais para o layout de combate");
        }
        int margin = 10;
        if (width < 680) {
            Rect status = new Rect(margin, height - 117, Math.min(220, width - 2 * margin), 50);
            Rect actions = new Rect(margin, height - 61, width - 2 * margin, 51);
            return new Layout(status, actions, true);
        }
        int statusWidth = Math.min(242, Math.max(205, width / 4));
        Rect status = new Rect(margin, height - 83, statusWidth, 73);
        Rect actions = new Rect(status.right() + 12, height - 83,
                width - status.right() - 2 * margin - 12, 73);
        return new Layout(status, actions, false);
    }

    public record Rect(int x, int y, int width, int height) {
        public int right() { return x + width; }
        public int bottom() { return y + height; }
    }

    public record Layout(Rect status, Rect actions, boolean compact) {
    }
}
