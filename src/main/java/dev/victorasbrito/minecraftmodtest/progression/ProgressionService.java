package dev.victorasbrito.minecraftmodtest.progression;

import net.minecraft.server.level.ServerPlayer;

/**
 * API de progressao executada exclusivamente no servidor.
 * O valor persistido e o XP total, e o nivel e calculado pela curva.
 */
public final class ProgressionService {
    private ProgressionService() {
    }

    public static ExperienceCurve.ExperienceProgress getProgress(ServerPlayer player) {
        return ExperienceCurve.progress(player.getData(ProgressionAttachments.RPG_TOTAL_XP));
    }

    public static GrantResult addExperience(ServerPlayer player, int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("XP negativo nao e permitido");
        }

        int before = ExperienceCurve.normalize(player.getData(ProgressionAttachments.RPG_TOTAL_XP));
        int after = ExperienceCurve.addExperience(before, amount);

        if (after != before) {
            player.setData(ProgressionAttachments.RPG_TOTAL_XP, after);
        }

        return new GrantResult(
                after - before,
                ExperienceCurve.progress(before).level(),
                ExperienceCurve.progress(after)
        );
    }

    public record GrantResult(
            int grantedXp,
            int previousLevel,
            ExperienceCurve.ExperienceProgress progress
    ) {
        public boolean leveledUp() {
            return progress.level() > previousLevel;
        }
    }
}
