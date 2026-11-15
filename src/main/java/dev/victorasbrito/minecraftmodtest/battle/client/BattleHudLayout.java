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

    /** Cartoes no alto a direita em telas largas; abaixo da timeline em telas estreitas. */
    public static Rect targetCard(int screenWidth,int screenHeight,int count,int index) {
        if (screenWidth < 240 || screenHeight < 160 || count < 1 || count > 5
                || index < 0 || index >= count) {
            throw new IllegalArgumentException("Area de alvos invalida");
        }
        int gap = 6;
        int columns = Math.min(count,screenWidth >= 700 ? 3 : 2);
        int cardWidth = Math.min(116,(screenWidth-24-(columns-1)*gap)/columns);
        int row=index/columns;
        int rowSize=Math.min(columns,count-row*columns);
        int startColumn=columns-rowSize;
        int col=startColumn+index%columns;
        int x=screenWidth-10-columns*cardWidth-(columns-1)*gap
                +col*(cardWidth+gap);
        int y=(screenWidth<600?67:14)+row*43;
        return new Rect(x,y,cardWidth,37);
    }

    public static int targetIndexAt(int width,int height,int count,double mx,double my) {
        if (width < 240 || height < 160 || count < 1 || count > 5) return -1;
        for (int i=0;i<count;i++) {
            if (targetCard(width,height,count,i).contains(mx,my)) return i;
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
