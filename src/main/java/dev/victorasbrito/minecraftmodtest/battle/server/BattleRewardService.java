package dev.victorasbrito.minecraftmodtest.battle.server;

import dev.victorasbrito.minecraftmodtest.battle.engine.BattleEngine;
import dev.victorasbrito.minecraftmodtest.battle.engine.BattleRewardPolicy;
import dev.victorasbrito.minecraftmodtest.battle.network.BattleResultPayload;
import dev.victorasbrito.minecraftmodtest.progression.ProgressionService;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Processa recompensas UMA VEZ por sessao concluida e autorizada.
 * Nunca executado a partir de mensagem solicitando recompensa do cliente.
 */
public final class BattleRewardService {
    private BattleRewardService() {}

    public static BattleResultPayload settle(ServerPlayer player, String sessionId,
                                              BattleEngine.Outcome outcome,
                                              boolean authorizedDebugRewards) {
        var before = ProgressionService.getProgress(player);
        var reward = BattleRewardPolicy.forOutcome(outcome, authorizedDebugRewards);
        int actualXp = 0;
        if (reward.experience() > 0) {
            actualXp = ProgressionService.addExperience(player, reward.experience()).grantedXp();
        }

        List<BattleResultPayload.LootEntry> items = new ArrayList<>();
        if (reward.rottenFlesh() > 0) {
            ItemStack stack = new ItemStack(Items.ROTTEN_FLESH, reward.rottenFlesh());
            // Inventario cheio: o restante cai fisicamente no chao.
            player.getInventory().add(stack);
            if (!stack.isEmpty()) {
                player.drop(stack, false);
            }
            items.add(new BattleResultPayload.LootEntry(
                    BuiltInRegistries.ITEM.getKey(Items.ROTTEN_FLESH).toString(),
                    reward.rottenFlesh()));
        }

        var after = ProgressionService.getProgress(player);
        return new BattleResultPayload(sessionId, outcome.name(), actualXp,
                before.totalXp(), after.totalXp(), items);
    }
}
