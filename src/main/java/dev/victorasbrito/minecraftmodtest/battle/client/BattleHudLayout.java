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

    /** Mesma geometria utilizada para desenhar e para receber cliques. */
    public static Rect actionCard(int screenWidth, int screenHeight, int index) {
        if (index < 0 || index >= 5) {
            throw new IllegalArgumentException("Indice de acao invalido");
        }
        Rect actions = forScreen(screenWidth, screenHeight).actions();
        int gap = 5;
        int cardWidth = (actions.width() - gap * 4) / 5;
        return new Rect(actions.x() + index * (cardWidth + gap),
                actions.y(), cardWidth, actions.height());
    }

    public static int actionIndexAt(int screenWidth, int screenHeight,
                                    double mouseX, double mouseY) {
        if (screenWidth < 240 || screenHeight < 160) return -1;
        for (int i = 0; i < 5; i++) {
            if (actionCard(screenWidth, screenHeight, i).contains(mouseX, mouseY)) {
                return i;
            }
        }
        return -1;
    }

    public record Rect(int x, int y, int width, int height) {
        public boolean contains(double px, double py) {
            return px >= x && py >= y && px < right() && py < bottom();
        }

        public int right() { return x + width; }
        public int bottom() { return y + height; }
    }

    public record Layout(Rect status, Rect actions, boolean compact) {
    }
}
