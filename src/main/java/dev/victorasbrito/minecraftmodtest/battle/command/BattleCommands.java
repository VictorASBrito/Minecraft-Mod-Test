package dev.victorasbrito.minecraftmodtest.battle.command;

import com.mojang.brigadier.Command;
import dev.victorasbrito.minecraftmodtest.battle.server.BattleSessionManager;
import dev.victorasbrito.minecraftmodtest.battle.server.BattleEncounterManager;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Comandos de diagnostico da primeira batalha simulada. */
public final class BattleCommands {
    private BattleCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("battle")
                .then(Commands.literal("auto")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("on").executes(ctx -> {
                            BattleEncounterManager.setEnabled(true);
                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("Encontros automaticos ativados."),false);
                            return Command.SINGLE_SUCCESS;
                        }))
                        .then(Commands.literal("off").executes(ctx -> {
                            BattleEncounterManager.setEnabled(false);
                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("Encontros automaticos desativados."),false);
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(Commands.literal("test").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    BattleSessionManager.startOrResume(player);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("test-group").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    BattleSessionManager.startGroup(player);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("test-rewards")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            BattleSessionManager.startOrResume(player, true);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("resume").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    if (!BattleSessionManager.resume(player)) {
                        ctx.getSource().sendFailure(Component.literal("Nenhum combate de teste em andamento."));
                    }
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("cancel").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    if (!BattleSessionManager.cancel(player)) {
                        ctx.getSource().sendFailure(Component.literal("Nenhum combate de teste em andamento."));
                    }
                    return Command.SINGLE_SUCCESS;
                }))
        );
    }
}
