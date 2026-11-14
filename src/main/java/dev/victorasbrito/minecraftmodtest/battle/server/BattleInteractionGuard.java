package dev.victorasbrito.minecraftmodtest.battle.server;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Defesa autoritativa no SERVIDOR. Nao confia no bloqueio do cliente:
 * um pacote atrasado ou mod externo tambem deve ser impedido de
 * atacar, usar itens, abrir baus e quebrar blocos durante a batalha.
 */
public final class BattleInteractionGuard {
    private BattleInteractionGuard() {}

    private static boolean locked(net.minecraft.world.entity.player.Player player) {
        return player instanceof ServerPlayer server && BattleSessionManager.isInBattle(server);
    }

    public static void onAttackEntity(AttackEntityEvent event) {
        if (locked(event.getEntity())) event.setCanceled(true);
    }

    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (locked(event.getEntity())) event.setCanceled(true);
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (locked(event.getEntity())) event.setCanceled(true);
    }

    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (locked(event.getEntity())) event.setCanceled(true);
    }

    public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (locked(event.getEntity())) event.setCanceled(true);
    }

    public static void onInteractEntitySpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (locked(event.getEntity())) event.setCanceled(true);
    }

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (locked(event.getPlayer())) event.setCanceled(true);
    }
}
