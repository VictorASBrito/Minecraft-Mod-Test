package dev.victorasbrito.minecraftmodtest.battle.server;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Prototipo: adiciona um zumbi REAL no mapa, com IA e dano desabilitados.
 * O HP de combate ainda pertence ao BattleEngine (nao ao Minecraft vanilla).
 *
 * Procuramos somente locais com piso e 2 blocos livres na altura do jogador.
 * Se nao houver local seguro, nao criamos a batalha.
 */
public final class BattleWorldFixture {
    private static final String TEST_MARKER = "minecraft_mod_test_battle_fixture";

    private BattleWorldFixture() {}

    public static Zombie spawn(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return null;
        Vec3 facing = player.getLookAngle();
        facing = new Vec3(facing.x, 0, facing.z);
        if (facing.lengthSqr() < 0.01) facing = new Vec3(0, 0, 1);
        facing = facing.normalize();
        Vec3 side = new Vec3(facing.z, 0, -facing.x);
        Vec3 origin = player.position();
        Vec3[] offsets = new Vec3[] {
                facing.scale(4), facing.scale(5), facing.scale(3),
                facing.scale(4).add(side.scale(2)), facing.scale(4).subtract(side.scale(2)),
                facing.scale(3).add(side.scale(3)), facing.scale(3).subtract(side.scale(3))
        };

        for (Vec3 offset : offsets) {
            double x = Math.floor(origin.x + offset.x) + 0.5;
            double z = Math.floor(origin.z + offset.z) + 0.5;
            int centerY = BlockPos.containing(origin).getY();

            for (int dy : new int[] {0, 1, -1, 2, -2}) {
                BlockPos feet = BlockPos.containing(x, centerY + dy, z);
                if (!level.isLoaded(feet)
                        || !level.getBlockState(feet.below()).isSolid()
                        || !level.isEmptyBlock(feet)
                        || !level.isEmptyBlock(feet.above())
                        || !level.getFluidState(feet).isEmpty()) {
                    continue;
                }

                Zombie zombie = EntityType.ZOMBIE.create(level);
                if (zombie == null) return null;
                zombie.moveTo(x, feet.getY(), z, 0, 0);
                zombie.setNoAi(true);
                zombie.setSilent(true);
                zombie.setInvulnerable(true);
                zombie.setDeltaMovement(Vec3.ZERO);
                if (!level.addFreshEntity(zombie)) {
                    return null;
                }
                // Marca SOMENTE depois do spawn para nao interferir no evento de entrada.
                zombie.getPersistentData().putBoolean(TEST_MARKER, true);
                return zombie;
            }
        }
        return null;
    }

    public static void clear(Zombie zombie) {
        if (zombie != null && !zombie.isRemoved()) {
            zombie.discard();
        }
    }

    /**
     * Se o servidor parou inesperadamente, elimina fixtures remanescentes
     * quando o chunk for carregado na proxima sessao.
     */
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()
                && event.getEntity() instanceof Zombie zombie
                && zombie.getPersistentData().getBoolean(TEST_MARKER)) {
            zombie.discard();
        }
    }
}
