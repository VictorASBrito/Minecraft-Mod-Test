package dev.victorasbrito.minecraftmodtest.battle.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Preferencia de APRESENTACAO local por cliente.
 * Neste MVP as duas visoes sao layouts 2D; modelos e cameras 3D virão depois.
 */
public final class BattleViewPreference {
    public enum View { ELEVATED, BEHIND }

    private static View current;

    private BattleViewPreference() {}

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("minecraft_mod_test-battle-view.properties");
    }

    public static View get() {
        if (current != null) return current;
        current = View.ELEVATED;
        Properties properties = new Properties();
        if (Files.exists(file())) {
            try (InputStream input = Files.newInputStream(file())) {
                properties.load(input);
                current = View.valueOf(properties.getProperty("view", View.ELEVATED.name()));
            } catch (IOException | IllegalArgumentException ignored) {
                current = View.ELEVATED;
            }
        }
        return current;
    }

    public static void toggle() {
        current = get() == View.ELEVATED ? View.BEHIND : View.ELEVATED;
        Properties properties = new Properties();
        properties.setProperty("view", current.name());
        try {
            Files.createDirectories(file().getParent());
            try (OutputStream output = Files.newOutputStream(file())) {
                properties.store(output, "Minecraft Mod Test - ultima visao de batalha");
            }
        } catch (IOException ignored) {
            // A preferencia continua valida na sessao mesmo sem acesso ao disco.
        }
    }
}
