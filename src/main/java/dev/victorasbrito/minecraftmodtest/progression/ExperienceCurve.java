package dev.victorasbrito.minecraftmodtest.progression;

/**
 * Regra pura e provisoria de XP de RPG. Separada do XP vanilla.
 * Nao depende de Minecraft; pode ser testada em testes de unidade.
 */
public final class ExperienceCurve {
    public static final int MAX_LEVEL = 50;
    public static final int BASE_XP_TO_NEXT = 100;
    public static final int XP_INCREMENT_PER_LEVEL = 25;

    private ExperienceCurve() {
    }

    /**
     * XP exigido para subir do nivel atual ao proximo.
     * Retorna 0 quando o jogador ja esta no nivel maximo.
     */
    public static int xpToNextLevel(int level) {
        if (level < 1 || level > MAX_LEVEL) {
            throw new IllegalArgumentException("Nivel fora do intervalo: " + level);
        }
        return level == MAX_LEVEL ? 0 : BASE_XP_TO_NEXT + (level - 1) * XP_INCREMENT_PER_LEVEL;
    }

    /** Total acumulado minimo para chegar a um dado nivel. */
    public static int totalXpForLevel(int level) {
        if (level < 1 || level > MAX_LEVEL) {
            throw new IllegalArgumentException("Nivel fora do intervalo: " + level);
        }
        int levelsGained = level - 1;
        return BASE_XP_TO_NEXT * levelsGained
                + XP_INCREMENT_PER_LEVEL * levelsGained * (levelsGained - 1) / 2;
    }

    public static int maxTotalXp() {
        return totalXpForLevel(MAX_LEVEL);
    }

    public static int normalize(int totalXp) {
        return Math.min(Math.max(totalXp, 0), maxTotalXp());
    }

    /**
     * Adiciona XP sem ultrapassar o maximo ou estourar o inteiro.
     */
    public static int addExperience(int currentTotalXp, int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("A quantidade de XP deve ser positiva");
        }
        long sum = (long) normalize(currentTotalXp) + amount;
        return (int) Math.min(sum, (long) maxTotalXp());
    }

    public static ExperienceProgress progress(int totalXp) {
        int normalized = normalize(totalXp);
        int level = 1;
        while (level < MAX_LEVEL && normalized >= totalXpForLevel(level + 1)) {
            level++;
        }
        if (level == MAX_LEVEL) {
            return new ExperienceProgress(level, 0, 0, normalized);
        }
        return new ExperienceProgress(
                level,
                normalized - totalXpForLevel(level),
                xpToNextLevel(level),
                normalized
        );
    }

    public record ExperienceProgress(int level, int xpInLevel, int xpToNextLevel, int totalXp) {
    }
}
