package dev.victorasbrito.minecraftmodtest.battle.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.victorasbrito.minecraftmodtest.battle.network.BattleActionPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.Input;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Controles RPG + bloqueio de acoes vanilla no CLIENTE.
 * O servidor possui bloqueios complementares para pacotes forjados e delays.
 */
public final class ClientBattleControls {
    private static final String CATEGORY = "key.categories.minecraft_mod_test";
    private static final KeyMapping ATTACK = key("key.minecraft_mod_test.attack", GLFW.GLFW_KEY_J);
    private static final KeyMapping GUARD = key("key.minecraft_mod_test.guard", GLFW.GLFW_KEY_K);
    private static final KeyMapping FLEE = key("key.minecraft_mod_test.flee", GLFW.GLFW_KEY_L);
    private static final KeyMapping SWITCH_CAMERA = key("key.minecraft_mod_test.camera", GLFW.GLFW_KEY_V);

    private static boolean actionSent;
    private static String lastSession = "";
    private static int lastTurn = -1;

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

    /** Executado assim que o Minecraft atualiza impulsos de movimento. */
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!ClientBattleHandler.active() ||
                event.getEntity() != Minecraft.getInstance().player) return;
        Input input = event.getInput();
        input.forwardImpulse = 0;
        input.leftImpulse = 0;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    /** Bloqueia a pipeline vanilla de ataque, uso de item e coleta de bloco. */
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (ClientBattleHandler.active()) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    /** Sem pressionamento do mouse, mas movimento livre para orientacao visual. */
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (ClientBattleHandler.active() && event.getButton() >= 0 && event.getButton() <= 2) {
            event.setCanceled(true);
        }
    }

    /** Impede mudar a hotbar por rolagem em combate. */
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (ClientBattleHandler.active()) event.setCanceled(true);
    }

    /** Uma tela de inventario/baú jamais deve substituir a HUD de batalha. */
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (ClientBattleHandler.active() &&
                event.getNewScreen() instanceof AbstractContainerScreen<?>) {
            event.setCanceled(true);
        }
    }

    /** Complemento da zeragem de impulsos para impedir teclas vanilla pendentes. */
    private static void clearVanillaControls(Minecraft mc) {
        KeyMapping[] blocked = {
                mc.options.keyUp, mc.options.keyDown, mc.options.keyLeft, mc.options.keyRight,
                mc.options.keyJump, mc.options.keyShift, mc.options.keySprint,
                mc.options.keyAttack, mc.options.keyUse, mc.options.keyPickItem,
                mc.options.keyDrop, mc.options.keySwapOffhand, mc.options.keyInventory
        };
        for (KeyMapping key : blocked) {
            key.setDown(false);
            while (key.consumeClick()) {
                // Remove presses acumulados enquanto uma battle snapshot e recebida.
            }
        }
        for (KeyMapping slot : mc.options.keyHotbarSlots) {
            slot.setDown(false);
            while (slot.consumeClick()) { }
        }
        // Cancela pequenos impulsos residuais de uma tecla apertada antes da batalha.
        if (mc.player != null) mc.player.setDeltaMovement(Vec3.ZERO);
    }

    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientBattleHandler.tick();
        if (!ClientBattleHandler.active()) {
            actionSent = false;
            lastSession = "";
            lastTurn = -1;
            consumeUnusedKeys();
            return;
        }

        clearVanillaControls(mc);
        while (SWITCH_CAMERA.consumeClick()) {
            BattleViewPreference.toggle();
            var state = ClientBattleHandler.state();
            if (state != null) ClientBattleCamera.reposition(state);
        }

        var state = ClientBattleHandler.state();
        if (state == null || mc.screen != null || !state.playerTurn()) return;

        if (!lastSession.equals(state.sessionId()) || lastTurn != state.turnId()) {
            actionSent = false;
            lastSession = state.sessionId();
            lastTurn = state.turnId();
        }

        if (ATTACK.consumeClick()) send("ATTACK");
        if (GUARD.consumeClick()) send("GUARD");
        if (FLEE.consumeClick()) send("FLEE");
    }

    private static void consumeUnusedKeys() {
        while (ATTACK.consumeClick()) {}
        while (GUARD.consumeClick()) {}
        while (FLEE.consumeClick()) {}
        while (SWITCH_CAMERA.consumeClick()) {}
    }

    private static void send(String action) {
        var state = ClientBattleHandler.state();
        if (actionSent || state == null || !state.playerTurn()) return;
        actionSent = true;
        PacketDistributor.sendToServer(new BattleActionPayload(
                state.sessionId(), state.turnId(), action));
    }
}
