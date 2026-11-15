package dev.victorasbrito.minecraftmodtest.battle.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Motor autoritativo e deterministico. Nao depende do Minecraft.
 * O modo padrao 1x1 e preservado; o teste de grupos habilita 1x2.
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
    private final List<Fighter> enemies = new ArrayList<>();
    private Outcome outcome = Outcome.ACTIVE;
    private boolean playerWaiting;
    private int turnId;
    private String combatLog = "Inimigos se aproximam!";

    public BattleEngine() {
        this(120, 70, 1);
    }

    public BattleEngine(int playerSpeed, int enemySpeed) {
        this(playerSpeed, enemySpeed, 1);
    }

    public BattleEngine(int playerSpeed, int enemySpeed, int enemyCount) {
        if (playerSpeed < 1 || enemySpeed < 1 || playerSpeed > 400 || enemySpeed > 400) {
            throw new IllegalArgumentException("Velocidade fora do intervalo 1..400");
        }
        if (enemyCount < 1 || enemyCount > 3) {
            throw new IllegalArgumentException("O prototipo suporta 1 a 3 inimigos");
        }
        player = new Fighter(120, playerSpeed);
        for (int i = 0; i < enemyCount; i++) {
            int speed = Math.max(1, enemySpeed - i * 10);
            enemies.add(new Fighter(enemyCount == 1 ? 90 : 60, speed));
        }
        combatLog = enemyCount == 1 ? "Um zumbi apareceu!" : enemyCount + " zumbis apareceram!";
        advanceToPlayerTurn();
    }

    /** Compatibilidade com testes e comandos 1x1 preexistentes. */
    public boolean play(Action action, int expectedTurnId) {
        return play(action, expectedTurnId, firstAliveEnemy());
    }

    /**
     * Retorna false sem consumir turno ao receber alvo invalido, morto,
     * comando duplicado ou ID de turno obsoleto.
     */
    public boolean play(Action action, int expectedTurnId, int targetIndex) {
        if (action == null || outcome != Outcome.ACTIVE || !playerWaiting
                || expectedTurnId != turnId) {
            return false;
        }
        if (action == Action.ATTACK && !isEnemyAlive(targetIndex)) {
            return false;
        }
        playerWaiting = false;

        if (action == Action.FLEE) {
            outcome = Outcome.FLED;
            combatLog = "Voce encerrou o confronto de teste.";
            return true;
        }

        if (action == Action.ATTACK) {
            Fighter target = enemies.get(targetIndex);
            target.hp = Math.max(0, target.hp - PLAYER_DAMAGE);
            player.gauge -= ATTACK_COST;
            combatLog = "Seu ataque causou " + PLAYER_DAMAGE + " de dano ao " + enemyName(targetIndex) + ".";
            if (target.hp == 0) combatLog += " " + enemyName(targetIndex) + " derrotado!";
        } else if (action == Action.GUARD) {
            player.guarding = true;
            player.gauge -= GUARD_COST;
            combatLog = "Voce assumiu uma postura defensiva.";
        }

        if (aliveEnemyCount() == 0) {
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
            if (aliveEnemyCount() == 0) {
                outcome = Outcome.VICTORY;
                combatLog += " Vitoria!";
                return;
            }

            int[] gauges = currentGauges();
            int selected = nextActor(gauges);
            if (selected == Integer.MIN_VALUE) {
                int steps = ticksUntilReady(player.gauge, player.speed);
                for (Fighter e : enemies) {
                    if (e.hp > 0) steps = Math.min(steps, ticksUntilReady(e.gauge, e.speed));
                }
                player.gauge += steps * player.speed;
                for (Fighter e : enemies) if (e.hp > 0) e.gauge += steps * e.speed;
                selected = nextActor(currentGauges());
            }

            if (selected == -1) {
                playerWaiting = true;
                turnId++;
                return;
            }
            if (selected >= 0) {
                Fighter enemy = enemies.get(selected);
                enemy.gauge -= ATTACK_COST;
                int damage = player.guarding ? ENEMY_DAMAGE / 2 : ENEMY_DAMAGE;
                player.guarding = false;
                player.hp = Math.max(0, player.hp - damage);
                combatLog += " " + enemyName(selected) + " causou " + damage + " de dano.";
            }
        }
        if (outcome == Outcome.ACTIVE) {
            throw new IllegalStateException("Limite de resolucao automatica da batalha atingido");
        }
    }

    private int[] currentGauges() {
        int[] values = new int[enemies.size() + 1];
        values[0] = player.gauge;
        for (int i = 0; i < enemies.size(); i++) values[i + 1] = enemies.get(i).gauge;
        return values;
    }

    /** -1=player; indices >= 0=enemies; MIN_VALUE=nobody ready. */
    private int nextActor(int[] gauges) {
        int best = Integer.MIN_VALUE;
        int bestGauge = Integer.MIN_VALUE;
        int bestSpeed = -1;
        for (int i = -1; i < enemies.size(); i++) {
            Fighter fighter = i == -1 ? player : enemies.get(i);
            if (fighter.hp == 0) continue;
            int gauge = gauges[i + 1];
            if (gauge < READY) continue;
            if (gauge > bestGauge || (gauge == bestGauge && fighter.speed > bestSpeed)) {
                best = i;
                bestGauge = gauge;
                bestSpeed = fighter.speed;
            }
        }
        return best;
    }

    private static int ticksUntilReady(int gauge, int speed) {
        return Math.max(1, (READY - gauge + speed - 1) / speed);
    }

    /** Previsao nao mutavel para a HUD: cada zumbi possui iniciativa propria. */
    public String previewTurns(int count) {
        int[] gauges = currentGauges();
        List<String> turns = new ArrayList<>();
        for (int i = 0; i < count && outcome == Outcome.ACTIVE; i++) {
            int actor = nextActor(gauges);
            if (actor == Integer.MIN_VALUE) {
                int steps = ticksUntilReady(gauges[0], player.speed);
                for (int j = 0; j < enemies.size(); j++) {
                    Fighter enemy = enemies.get(j);
                    if (enemy.hp > 0) steps = Math.min(steps, ticksUntilReady(gauges[j + 1], enemy.speed));
                }
                gauges[0] += steps * player.speed;
                for (int j = 0; j < enemies.size(); j++) {
                    if (enemies.get(j).hp > 0) gauges[j + 1] += steps * enemies.get(j).speed;
                }
                actor = nextActor(gauges);
            }
            if (actor == Integer.MIN_VALUE) break;
            turns.add(actor == -1 ? "Voce" : enemyName(actor));
            gauges[actor + 1] -= ATTACK_COST;
        }
        return String.join(" > ", turns);
    }

    private int firstAliveEnemy() {
        for (int i = 0; i < enemies.size(); i++) if (isEnemyAlive(i)) return i;
        return -1;
    }

    private String enemyName(int index) {
        return enemies.size() == 1 ? "Zumbi" : "Zumbi " + (index + 1);
    }

    public int enemyCount() { return enemies.size(); }
    public int aliveEnemyCount() {
        int count = 0;
        for (Fighter e : enemies) if (e.hp > 0) count++;
        return count;
    }
    public boolean isEnemyAlive(int index) {
        return index >= 0 && index < enemies.size() && enemies.get(index).hp > 0;
    }
    public int enemyHp(int index) { return enemies.get(index).hp; }
    public int enemyHp() { return enemies.get(0).hp; }
    public int enemyMaxHp() { return enemies.get(0).maxHp; }
    public int enemyGauge() { return enemies.get(0).gauge; }
    public int enemySpeed() { return enemies.get(0).speed; }

    public Outcome outcome() { return outcome; }
    public boolean isPlayerTurn() { return playerWaiting && outcome == Outcome.ACTIVE; }
    public int turnId() { return turnId; }
    public int playerHp() { return player.hp; }
    public int playerMaxHp() { return player.maxHp; }
    public int playerSpeed() { return player.speed; }
    public int playerGauge() { return player.gauge; }
    public boolean isGuarding() { return player.guarding; }
    public String combatLog() { return combatLog; }

    private static final class Fighter {
        final int maxHp;
        final int speed;
        int hp;
        int gauge;
        boolean guarding;

        Fighter(int maxHp, int speed) {
            this.maxHp = maxHp;
            this.hp = maxHp;
            this.speed = speed;
        }
    }
}
