package dev.victorasbrito.minecraftmodtest.battle.client;

import dev.victorasbrito.minecraftmodtest.battle.network.BattleStatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Selecao compacta de alvos, sem mostrar HP e sem obstruir o mapa. */
public final class BattleTargetHud {
    private static final ItemStack ZOMBIE_HEAD = new ItemStack(Items.ZOMBIE_HEAD);
    private BattleTargetHud() {}

    public static void render(GuiGraphics g, int mouseX, int mouseY) {
        BattleStatePayload state = ClientBattleHandler.state();
        if (state == null || state.targets().isEmpty()) return;
        Minecraft minecraft = Minecraft.getInstance();
        for (int i = 0; i < state.targets().size(); i++) {
            BattleStatePayload.TargetSnapshot target = state.targets().get(i);
            BattleHudLayout.Rect card = BattleHudLayout.targetCard(
                    g.guiWidth(),g.guiHeight(),state.targets().size(),i);
            int x = card.x(), y = card.y(), right = card.right(), bottom = card.bottom();
            boolean selected = target.index() == ClientBattleHandler.selectedTarget();
            boolean hovered = card.contains(mouseX,mouseY);
            int edge = !target.alive() ? 0xFF495356 :
                    selected ? 0xFFF6D18A : hovered ? 0xFFE5E6DF : 0xFF967B50;
            g.fill(x,y,right,bottom,0xDA101F29);
            g.fill(x,y,right,y+2,edge);
            g.fill(x,bottom-2,right,bottom,edge);
            g.fill(x,y,x+2,bottom,edge);
            g.fill(right-2,y,right,bottom,edge);
            if (target.alive()) {
                g.renderItem(ZOMBIE_HEAD,x+5,y+9);
            } else {
                g.fill(x+5,y+9,x+21,y+25,0xFF373C40);
                g.drawCenteredString(minecraft.font,"X",x+13,y+13,0xFFB1B0B0);
            }
            String name = minecraft.font.plainSubstrByWidth(target.name(),card.width()-29);
            g.drawString(minecraft.font,name,x+27,y+7,
                    target.alive() ? 0xFFF6F2E7 : 0xFF8C969B,false);
            String line = target.alive() ? selected ? "SELECIONADO" : "Clique / TAB" : "Derrotado";
            g.drawString(minecraft.font,line,x+27,y+21,
                    target.alive() ? edge : 0xFF778186,false);
        }
    }
}
