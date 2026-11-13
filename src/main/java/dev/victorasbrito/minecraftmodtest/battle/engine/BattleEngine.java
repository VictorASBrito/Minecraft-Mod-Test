package dev.victorasbrito.minecraftmodtest.battle.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Motor deterministico e puro: nenhuma dependencia do cliente ou do mundo.
 * O servidor e a unica autoridade sobre vida, dano e ordem de iniciativa.
 */
public final class BattleEngine {
    public enum Action { ATTACK, GUARD, FLEE }
    public enum Outcome { ACTIVE, VICTORY, DEFEAT, FLED }

    public static final int READY = 1000;
    private static final int ATTACK_COST = 1000;
    private static final int GUARD_COST = 700;
    private static final int PLAYER_DAMAGE = 30;
    private static final int ENEMY_DAMAGE = 18;
    private static final int MAX_AUTOMATIC_ACTIONS = 128;

    private final Fighter player;
    private final Fighter enemy;
    private Outcome outcome = Outcome.ACTIVE;
    private boolean playerWaiting;
    private int turnId;
    private String combatLog = "Um zumbi apareceu!";

    public BattleEngine() {
        this(120, 70);
    }

    public BattleEngine(int playerSpeed, int enemySpeed) {
        if (playerSpeed < 1 || enemySpeed < 1 || playerSpeed > 400 || enemySpeed > 400) {
            throw new IllegalArgumentException("Velocidade fora do intervalo 1..400");
        }
        this.player = new Fighter(120, playerSpeed);
        this.enemy = new Fighter(90, enemySpeed);
        advanceToPlayerTurn();
    }

    /**
     * Garante que um pacote antigo ou uma segunda acao no mesmo turno
     * nao tenha efeito. O turno e validado pelo servidor.
     */
    public boolean play(Action action, int expectedTurnId) {
        if (action == null || outcome != Outcome.ACTIVE || !playerWaiting || expectedTurnId != turnId) {
            return false;
        }
        playerWaiting = false;

        if (action == Action.FLEE) {
            outcome = Outcome.FLED;
            combatLog = "Voce encerrou o confronto de teste.";
            return true;
        }

        if (action == Action.ATTACK) {
            enemy.hp = Math.max(0, enemy.hp - PLAYER_DAMAGE);
            player.gauge -= ATTACK_COST;
            combatLog = "Seu ataque causou " + PLAYER_DAMAGE + " de dano ao zumbi.";
        } else if (action == Action.GUARD) {
            player.guarding = true;
            player.gauge -= GUARD_COST;
            combatLog = "Voce assumiu uma postura defensiva.";
        }

        if (enemy.hp == 0) {
            outcome = Outcome.VICTORY;
            combatLog += " Vitoria!";
        } else {
            advanceToPlayerTurn();
        }
        return true;
    }

    private void advanceToPlayerTurn() {
        for (int i = 0; i < MAX_AUTOMATIC_ACTIONS && outcome == Outcome.ACTIVE; i++) {
            if (player.hp <= 0) {
                outcome = Outcome.DEFEAT;
                combatLog += " Derrota.";
                return;
            }
            if (enemy.hp <= 0) {
                outcome = Outcome.VICTORY;
                combatLog += " Vitoria!";
                return;
            }
            if (player.gauge < READY && enemy.gauge < READY) {
                int steps = Math.min(
                        ticksUntilReady(player.gauge, player.speed),
                        ticksUntilReady(enemy.gauge, enemy.speed)
                );
                player.gauge += steps * player.speed;
                enemy.gauge += steps * enemy.speed;
            }

            if (isPlayerNext(player.gauge, player.speed, enemy.gauge, enemy.speed)) {
                playerWaiting = true;
                turnId++;
                return;
            }

            if (enemy.gauge >= READY) {
                enemy.gauge -= ATTACK_COST;
                int damage = player.guarding ? ENEMY_DAMAGE / 2 : ENEMY_DAMAGE;
                player.guarding = false;
                player.hp = Math.max(0, player.hp - damage);
                combatLog += " Zumbi causou " + damage + " de dano.";
            }
        }
        if (outcome == Outcome.ACTIVE) {
            throw new IllegalStateException("Limite de resolucao automatica da batalha atingido");
        }
    }

    private static int ticksUntilReady(int gauge, int speed) {
        return Math.max(1, (READY - gauge + speed - 1) / speed);
    }

    private static boolean isPlayerNext(int playerGauge, int playerSpeed, int enemyGauge, int enemySpeed) {
        if (playerGauge < READY) return false;
        if (enemyGauge < READY) return true;
        if (playerGauge != enemyGauge) return playerGauge > enemyGauge;
        return playerSpeed >= enemySpeed;
    }

    /**
     * Previsao localmente calculada a partir do estado AUTORITATIVO do motor.
     * Simula custos normais sem aplicar dano nem modificar o estado real.
     */
    public String previewTurns(int count) {
        int p = player.gauge;
        int e = enemy.gauge;
        List<String> turns = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if (p < READY && e < READY) {
                int steps = Math.min(ticksUntilReady(p, player.speed), ticksUntilReady(e, enemy.speed));
                p += steps * player.speed;
                e += steps * enemy.speed;
            }
            if (isPlayerNext(p, player.speed, e, enemy.speed)) {
                turns.add("Voce");
                p -= ATTACK_COST;
            } else {
                turns.add("Zumbi");
                e -= ATTACK_COST;
            }
        }
        return String.join(" > ", turns);
    }

    public Outcome outcome() { return outcome; }
    public boolean isPlayerTurn() { return playerWaiting && outcome == Outcome.ACTIVE; }
    public int turnId() { return turnId; }
    public int playerHp() { return player.hp; }
    public int enemyHp() { return enemy.hp; }
    public int playerMaxHp() { return player.maxHp; }
    public int enemyMaxHp() { return enemy.maxHp; }
    public int playerSpeed() { return player.speed; }
    public int enemySpeed() { return enemy.speed; }
    public int playerGauge() { return player.gauge; }
    public int enemyGauge() { return enemy.gauge; }
    public boolean isGuarding() { return player.guarding; }
    public String combatLog() { return combatLog; }

    private static final class Fighter {
        private final int maxHp;
        private final int speed;
        private int hp;
        private int gauge;
        private boolean guarding;

        private Fighter(int maxHp, int speed) {
            this.maxHp = maxHp;
            this.hp = maxHp;
            this.speed = speed;
        }
    }
}
