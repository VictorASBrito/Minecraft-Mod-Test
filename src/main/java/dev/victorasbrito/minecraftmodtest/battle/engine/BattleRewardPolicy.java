package dev.victorasbrito.minecraftmodtest.battle.engine;

/**
 * Regras puras do prototipo. Mobs simulados NAO concedem recompensas comuns;
 * apenas o teste explicitamente autorizado por operador pode conceder loot.
 * Futuramente uma tabela de encounters substituira esta politica de teste.
 */
public final class BattleRewardPolicy {
    public static final int DEBUG_XP = 20;
    public static final int DEBUG_ROTTEN_FLESH = 1;

    private BattleRewardPolicy() {}

    public static RewardDefinition forOutcome(BattleEngine.Outcome outcome,
                                               boolean authorizedDebugRewards) {
        if (authorizedDebugRewards && outcome == BattleEngine.Outcome.VICTORY) {
            return new RewardDefinition(DEBUG_XP, DEBUG_ROTTEN_FLESH);
        }
        return new RewardDefinition(0, 0);
    }

    /**
     * Um encontro por aggro real, concluido em vitoria, paga somente pelo
     * resultado do RPG; zombies discardados NAO geram drops vanilla.
     * Limite protege contra entidades malformadas e exploits de grupo.
     */
    public static RewardDefinition forNaturalVictory(BattleEngine.Outcome outcome, int defeated) {
        if (outcome != BattleEngine.Outcome.VICTORY || defeated <= 0) {
            return new RewardDefinition(0,0);
        }
        int count=Math.min(defeated,5);
        return new RewardDefinition(count * DEBUG_XP, count * DEBUG_ROTTEN_FLESH);
    }

    public record RewardDefinition(int experience, int rottenFlesh) {
        public RewardDefinition {
            if (experience < 0 || rottenFlesh < 0) {
                throw new IllegalArgumentException("Recompensa negativa nao permitida");
            }
        }
    }
}
