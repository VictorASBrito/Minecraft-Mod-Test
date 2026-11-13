package dev.victorasbrito.minecraftmodtest.battle.client;

import dev.victorasbrito.minecraftmodtest.battle.network.BattleStatePayload;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;

/**
 * Camera virtual EXCLUSIVAMENTE cliente, sem spawn no mundo.
 * O mundo e as entidades reais continuam sendo renderizados pelo Minecraft.
 * A camera do jogador e seu modo original sao restaurados quando a batalha termina.
 */
public final class ClientBattleCamera {
    private static ArmorStand cameraRig;
    private static Entity previousCamera;
    private static CameraType previousType;
    private static boolean active;

    private ClientBattleCamera() {}

    public static void update(BattleStatePayload state) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            restore();
            return;
        }
        if (state.finished()) {
            restore();
            return;
        }

        if (!active) {
            previousCamera = mc.getCameraEntity();
            previousType = mc.options.getCameraType();
            cameraRig = new ArmorStand(mc.level, mc.player.getX(), mc.player.getY(), mc.player.getZ());
            cameraRig.setInvisible(true);
            cameraRig.setNoGravity(true);
            active = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.setCameraEntity(cameraRig);
        }
        reposition(state);
    }

    public static void reposition(BattleStatePayload state) {
        Minecraft mc = Minecraft.getInstance();
        if (!active || cameraRig == null || mc.player == null || mc.level == null) return;
        if (cameraRig.level() != mc.level) {
            restore();
            return;
        }

        Vec3 player = new Vec3(state.playerX(), state.playerY(), state.playerZ());
        Vec3 enemy = new Vec3(state.enemyX(), state.enemyY(), state.enemyZ());
        Vec3 horizontal = new Vec3(enemy.x - player.x, 0, enemy.z - player.z);
        if (horizontal.lengthSqr() < 0.01) horizontal = new Vec3(0, 0, 1);
        Vec3 forward = horizontal.normalize();
        Vec3 right = new Vec3(forward.z, 0, -forward.x);
        Vec3 midpoint = player.add(enemy).scale(0.5).add(0, 1.35, 0);

        Vec3 viewpoint;
        Vec3 lookAt;
        if (BattleViewPreference.get() == BattleViewPreference.View.BEHIND) {
            viewpoint = player.subtract(forward.scale(4.5)).add(0, 2.8, 0);
            lookAt = enemy.add(0, 1.3, 0);
        } else {
            viewpoint = midpoint.add(right.scale(9)).add(0, 5.0, 0);
            lookAt = midpoint;
        }

        Vec3 gaze = lookAt.subtract(viewpoint);
        float yaw = (float) Math.toDegrees(Math.atan2(-gaze.x, gaze.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(gaze.y,
                Math.sqrt(gaze.x * gaze.x + gaze.z * gaze.z)));
        // ArmorStand is never added to the world. We move only the local camera entity.
        cameraRig.moveTo(viewpoint.x, viewpoint.y - cameraRig.getEyeHeight(), viewpoint.z, yaw, pitch);
        cameraRig.setOldPosAndRot();
        if (mc.getCameraEntity() != cameraRig) {
            mc.setCameraEntity(cameraRig);
        }
        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static void restore() {
        if (!active) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCameraEntity() == cameraRig) {
            Entity restoreTo = mc.player != null ? mc.player : previousCamera;
            if (restoreTo != null) mc.setCameraEntity(restoreTo);
        }
        if (previousType != null) mc.options.setCameraType(previousType);
        previousCamera = null;
        previousType = null;
        cameraRig = null;
        active = false;
    }
}
