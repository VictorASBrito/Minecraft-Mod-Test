package dev.victorasbrito.minecraftmodtest.command;

import com.mojang.brigadier.Command;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Comando basico de smoke test, disponivel no servidor.
 */
public final class RpgStatusCommand {
    private RpgStatusCommand() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("rpgstatus")
                        .executes(context -> {
                            context.getSource().sendSuccess(
                                    () -> Component.translatable("command.minecraft_mod_test.status"),
                                    false
                            );
                            return Command.SINGLE_SUCCESS;
                        })
        );
    }
}
