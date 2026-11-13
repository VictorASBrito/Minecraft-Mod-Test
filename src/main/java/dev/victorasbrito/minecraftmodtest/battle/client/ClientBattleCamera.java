package dev.victorasbrito.minecraftmodtest.battle.client;

import dev.victorasbrito.minecraftmodtest.battle.network.BattleStatePayload;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Camera de batalha no mundo real, aplicada ao Camera.setup por um mixin CLIENT.
 *
 * Ao contrario do prototipo anterior, nao cria uma entidade falsa para servir
 * de camera. O Minecraft continua renderizando a partir do jogador real, em
 * terceira pessoa; somente a posicao e a rotacao finais sao sobrescritas.
 *
 * A escolha entre lateral elevada e atras do personagem nao altera o motor
 * de batalha. A preferencia continua armazenada localmente no cliente.
 */
public final class ClientBattleCamera {
    private static BattleStatePayload snapshot;
    private static Entity originalCameraEntity;
    private static CameraType originalCameraType;
    private static boolean active;

    private ClientBattleCamera() {
    }

    public static void update(BattleStatePayload state) {
        if (state.finished()) {
            restore();
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            restore();
            return;
        }

        if (!active) {
            originalCameraEntity = minecraft.getCameraEntity();
            originalCameraType = minecraft.options.getCameraType();
            active = true;
        }

        snapshot = state;
        enforceThirdPerson();
    }

    /**
     * Chamado no tick do cliente para manter a terceira pessoa mesmo quando
     * F5 ou outro mod mudar a perspectiva durante uma batalha.
     */
    public static void reposition(BattleStatePayload state) {
        if (!active) {
            update(state);
            return;
        }
        if (state.finished()) {
            restore();
            return;
        }
        snapshot = state;
        enforceThirdPerson();
    }

    private static void enforceThirdPerson() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            restore();
            return;
        }
        if (minecraft.getCameraEntity() != minecraft.player) {
            minecraft.setCameraEntity(minecraft.player);
        }
        if (minecraft.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
    }

    /**
     * E calculado no momento do render, nao somente a cada tick.
     * CameraMixin aplica este transform apos a preparacao vanilla da camera.
     */
    public static CameraTransform currentTransform() {
        Minecraft minecraft = Minecraft.getInstance();
        BattleStatePayload state = snapshot;
        if (!active || state == null || state.finished()
                || minecraft.player == null || minecraft.level == null
                || minecraft.getCameraEntity() != minecraft.player) {
            return null;
        }

        Vec3 player = new Vec3(state.playerX(), state.playerY(), state.playerZ());
        Vec3 enemy = new Vec3(state.enemyX(), state.enemyY(), state.enemyZ());
        Vec3 distance = enemy.subtract(player);
        Vec3 horizontal = new Vec3(distance.x, 0.0, distance.z);
        if (horizontal.lengthSqr() < 0.01) horizontal = new Vec3(0, 0, 1);

        Vec3 forward = horizontal.normalize();
        Vec3 right = new Vec3(forward.z, 0.0, -forward.x);
        Vec3 middle = player.add(enemy).scale(0.5);

        // Os dois atores ficam visiveis dentro do enquadramento, nao renderizados
        // numa GUI. O ambiente real do Minecraft funciona como a arena.
        Vec3 desiredPosition;
        Vec3 lookAt;

        if (BattleViewPreference.get() == BattleViewPreference.View.BEHIND) {
            desiredPosition = player.subtract(forward.scale(3.8))
                    .add(right.scale(0.85)).add(0.0, 2.5, 0.0);
            lookAt = enemy.add(0.0, 1.25, 0.0);
        } else {
            desiredPosition = middle.add(right.scale(7.0)).add(0.0, 4.5, 0.0);
            lookAt = middle.add(0.0, 1.15, 0.0);
        }

        Vec3 position = avoidSolidBlocks(minecraft, lookAt, desiredPosition);
        Vec3 look = lookAt.subtract(position);
        double planar = Math.sqrt(look.x * look.x + look.z * look.z);
        float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(look.y, planar));
        return new CameraTransform(position, yaw, pitch);
    }

    /**
     * Raycast contra blocos visuais para evitar o ponto de vista dentro de
     * paredes. Nao altera os alvos nem sua posicao dentro do mundo.
     */
    private static Vec3 avoidSolidBlocks(Minecraft minecraft, Vec3 target, Vec3 desired) {
        Vec3 direction = desired.subtract(target);
        if (direction.lengthSqr() < 0.01) return desired;
        BlockHitResult hit = minecraft.level.clip(new ClipContext(
                target, desired, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE,
                minecraft.player
        ));
        if (hit.getType() == HitResult.Type.MISS) return desired;

        double safeDistance = Math.max(0.75, target.distanceTo(hit.getLocation()) - 0.35);
        return target.add(direction.normalize().scale(Math.min(direction.length(), safeDistance)));
    }

    public static boolean isActive() {
        return active;
    }

    public static void restore() {
        if (!active) return;

        Minecraft minecraft = Minecraft.getInstance();
        // Nao restaurar uma camera de uma dimensao/servidor antigo.
        if (minecraft.player != null) {
            Entity previous = originalCameraEntity != null
                    && originalCameraEntity.level() == minecraft.player.level()
                    ? originalCameraEntity : minecraft.player;
            minecraft.setCameraEntity(previous);
        }
        if (originalCameraType != null) {
            minecraft.options.setCameraType(originalCameraType);
        }
        snapshot = null;
        originalCameraEntity = null;
        originalCameraType = null;
        active = false;
    }

    public record CameraTransform(Vec3 position, float yaw, float pitch) {
    }
}
