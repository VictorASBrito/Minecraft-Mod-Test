package dev.victorasbrito.minecraftmodtest.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.victorasbrito.minecraftmodtest.progression.ExperienceCurve;
import dev.victorasbrito.minecraftmodtest.progression.ProgressionService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Comandos de leitura para qualquer jogador; concessao de XP somente OP.
 */
public final class RpgProgressionCommand {
    private RpgProgressionCommand() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("rpg")
                        .executes(RpgProgressionCommand::showProfile)
                        .then(Commands.literal("profile")
                                .executes(RpgProgressionCommand::showProfile))
                        .then(Commands.literal("xp")
                                .then(Commands.literal("add")
                                        .requires(source -> source.hasPermission(2))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 100_000))
                                                        .executes(RpgProgressionCommand::addExperience)))))
        );
    }

    private static int showProfile(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ExperienceCurve.ExperienceProgress progress = ProgressionService.getProgress(player);

        if (progress.level() == ExperienceCurve.MAX_LEVEL) {
            context.getSource().sendSuccess(
                    () -> Component.translatable("command.minecraft_mod_test.profile_max",
                            progress.level(), progress.totalXp()),
                    false
            );
        } else {
            context.getSource().sendSuccess(
                    () -> Component.translatable("command.minecraft_mod_test.profile",
                            progress.level(), progress.xpInLevel(),
                            progress.xpToNextLevel(), progress.totalXp()),
                    false
            );
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int addExperience(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        int amount = IntegerArgumentType.getInteger(context, "amount");

        ProgressionService.GrantResult result = ProgressionService.addExperience(player, amount);
        context.getSource().sendSuccess(
                () -> Component.translatable(
                        "command.minecraft_mod_test.xp_added",
                        result.grantedXp(),
                        player.getDisplayName(),
                        result.progress().level()
                ),
                true
        );

        if (result.leveledUp()) {
            player.sendSystemMessage(
                    Component.translatable("message.minecraft_mod_test.level_up",
                            result.progress().level())
            );
        }
        return Command.SINGLE_SUCCESS;
    }
}
