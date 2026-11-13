package dev.victorasbrito.minecraftmodtest.battle.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.victorasbrito.minecraftmodtest.battle.network.BattleActionPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.event.tick.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * As teclas sao configuraveis em Controles. Nao ha Screen aberta durante batalha.
 */
public final class ClientBattleControls {
    private static final String CATEGORY = "key.categories.minecraft_mod_test";
    private static final KeyMapping ATTACK = key("key.minecraft_mod_test.attack", GLFW.GLFW_KEY_J);
    private static final KeyMapping GUARD = key("key.minecraft_mod_test.guard", GLFW.GLFW_KEY_K);
    private static final KeyMapping FLEE = key("key.minecraft_mod_test.flee", GLFW.GLFW_KEY_L);
    private static final KeyMapping SWITCH_CAMERA = key("key.minecraft_mod_test.camera", GLFW.GLFW_KEY_V);

    private static boolean actionSent;

    private ClientBattleControls() {}

    private static KeyMapping key(String name, int code) {
        return new KeyMapping(name, InputConstants.Type.KEYSYM, code, CATEGORY);
    }

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ATTACK);
        event.register(GUARD);
        event.register(FLEE);
        event.register(SWITCH_CAMERA);
    }

    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientBattleHandler.tick();

        if (!ClientBattleHandler.active()) {
            actionSent = false;
            ATTACK.consumeClick();
            GUARD.consumeClick();
            FLEE.consumeClick();
            SWITCH_CAMERA.consumeClick();
            return;
        }

        // Impede que o jogador ande enquanto o servidor fixa a posicao da batalha.
        mc.options.keyUp.setDown(false);
        mc.options.keyDown.setDown(false);
        mc.options.keyLeft.setDown(false);
        mc.options.keyRight.setDown(false);
        mc.options.keyJump.setDown(false);
        mc.options.keyShift.setDown(false);
        mc.options.keyAttack.setDown(false);
        mc.options.keyUse.setDown(false);

        while (SWITCH_CAMERA.consumeClick()) {
            BattleViewPreference.toggle();
            var state = ClientBattleHandler.state();
            if (state != null) ClientBattleCamera.reposition(state);
        }

        var state = ClientBattleHandler.state();
        if (state == null || !state.playerTurn() || mc.screen != null) return;

        // Cada novo turno tem um ID, impedindo duplicacao ao manter uma tecla apertada.
        if (actionSent && state != lastActionState) actionSent = false;
        lastActionState = state;
        if (ATTACK.consumeClick()) send("ATTACK");
        if (GUARD.consumeClick()) send("GUARD");
        if (FLEE.consumeClick()) send("FLEE");
    }

    private static dev.victorasbrito.minecraftmodtest.battle.network.BattleStatePayload lastActionState;

    private static void send(String action) {
        var state = ClientBattleHandler.state();
        if (actionSent || state == null || !state.playerTurn()) return;
        actionSent = true;
        PacketDistributor.sendToServer(new BattleActionPayload(state.sessionId(), state.turnId(), action));
    }
}
