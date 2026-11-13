package dev.victorasbrito.minecraftmodtest.progression;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Experiencia obtida ao matar mobs hostis.
 * Valor provisorio: 20 de XP RPG por monstro.
 */
public final class ProgressionEvents {
    public static final int XP_PER_HOSTILE_MOB = 20;

    private ProgressionEvents() {
    }

    public static void onMobDeath(LivingDeathEvent event) {
        if (event.isCanceled()
                || !(event.getEntity() instanceof Monster)
                || !(event.getEntity().level() instanceof ServerLevel)
                || !(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ProgressionService.GrantResult result =
                ProgressionService.addExperience(player, XP_PER_HOSTILE_MOB);

        if (result.grantedXp() == 0) {
            return;
        }

        player.displayClientMessage(
                Component.translatable("message.minecraft_mod_test.xp_gained", result.grantedXp()),
                true
        );

        if (result.leveledUp()) {
            player.sendSystemMessage(Component.translatable(
                    "message.minecraft_mod_test.level_up",
                    result.progress().level()
            ));
        }
    }
}
