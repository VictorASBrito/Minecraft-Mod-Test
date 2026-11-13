package dev.victorasbrito.minecraftmodtest.mixin.client;

import dev.victorasbrito.minecraftmodtest.battle.client.ClientBattleCamera;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Aplica o enquadramento ao Camera.setup real de Minecraft 1.21.1.
 *
 * Estrategia de camera semelhante ao Cobblemon Cinematics:
 * o jogador permanece em TERCEIRA pessoa e a Camera recebe a transformacao
 * no fim de setup, sem substituir a entidade da camera por um ArmorStand.
 */
@Mixin(Camera.class)
public abstract class BattleCameraMixin {
    @Shadow
    protected abstract void setPosition(Vec3 pos);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "setup", at = @At("TAIL"))
    private void minecraftModTest$applyBattleCamera(
            BlockGetter level,
            Entity cameraEntity,
            boolean detached,
            boolean thirdPersonReverse,
            float partialTick,
            CallbackInfo callback
    ) {
        ClientBattleCamera.CameraTransform transform = ClientBattleCamera.currentTransform();
        if (transform == null) return;

        setRotation(transform.yaw(), transform.pitch());
        setPosition(transform.position());
    }
}
