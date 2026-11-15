package dev.victorasbrito.minecraftmodtest.battle.server;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Zumbis visuais reais, com HP RPG simulado e sem IA ou drops vanilla. */
public final class BattleWorldFixture {
    private static final String TEST_MARKER = "minecraft_mod_test_battle_fixture";
    private BattleWorldFixture() {}

    public static Zombie spawn(ServerPlayer player) {
        return spawn(player, List.of());
    }

    /** Posiciona cada zumbi em piso livre, sem sobrepor outro participante. */
    public static Zombie spawn(ServerPlayer player, List<Vec3> occupied) {
        if (!(player.level() instanceof ServerLevel level)) return null;
        Vec3 facing = player.getLookAngle();
        facing = new Vec3(facing.x, 0, facing.z);
        if (facing.lengthSqr() < 0.01) facing = new Vec3(0, 0, 1);
        facing = facing.normalize();
        Vec3 side = new Vec3(facing.z, 0, -facing.x);
        Vec3 origin = player.position();

        Vec3[] offsets = {
                facing.scale(4), facing.scale(4).add(side.scale(2.5)),
                facing.scale(4).subtract(side.scale(2.5)),
                facing.scale(5).add(side.scale(3)),
                facing.scale(5).subtract(side.scale(3)),
                facing.scale(3), facing.scale(6)
        };
        for (Vec3 offset : offsets) {
            double x = Math.floor(origin.x + offset.x) + 0.5;
            double z = Math.floor(origin.z + offset.z) + 0.5;
            if (occupied.stream().anyMatch(p -> new Vec3(x, p.y, z).distanceToSqr(p) < 3.5)) {
                continue;
            }
            int centerY = BlockPos.containing(origin).getY();
            for (int dy : new int[]{0, 1, -1, 2, -2}) {
                BlockPos feet = BlockPos.containing(x, centerY + dy, z);
                if (!level.isLoaded(feet)
                        || !level.getBlockState(feet.below()).isSolid()
                        || !level.isEmptyBlock(feet)
                        || !level.isEmptyBlock(feet.above())
                        || !level.getFluidState(feet).isEmpty()) continue;

                Zombie zombie = EntityType.ZOMBIE.create(level);
                if (zombie == null) return null;
                zombie.moveTo(x, feet.getY(), z, 0, 0);
                zombie.setNoAi(true);
                zombie.setSilent(true);
                zombie.setInvulnerable(true);
                zombie.setDeltaMovement(Vec3.ZERO);
                if (!level.addFreshEntity(zombie)) return null;
                zombie.getPersistentData().putBoolean(TEST_MARKER, true);
                return zombie;
            }
        }
        return null;
    }

    public static void clear(Zombie zombie) {
        if (zombie != null && !zombie.isRemoved()) zombie.discard();
    }

    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Zombie zombie)) return;
        if (zombie.getPersistentData().getBoolean(TEST_MARKER)) {
            zombie.discard();
            return;
        }
        // Recuperacao apos travamento abrupto: a reserva de batalha nao existe
        // mais no novo processo, portanto o monstro volta ao comportamento vanilla.
        if (zombie.getPersistentData().getBoolean("minecraft_mod_test_reserved_real")) {
            boolean oldAi = zombie.getPersistentData().getBoolean("minecraft_mod_test_old_no_ai");
            boolean oldInvulnerable =
                    zombie.getPersistentData().getBoolean("minecraft_mod_test_old_invuln");
            zombie.getPersistentData().remove("minecraft_mod_test_reserved_real");
            zombie.getPersistentData().remove("minecraft_mod_test_old_no_ai");
            zombie.getPersistentData().remove("minecraft_mod_test_old_invuln");
            zombie.setNoAi(oldAi);
            zombie.setInvulnerable(oldInvulnerable);
            zombie.setTarget(null);
        }
    }
}
