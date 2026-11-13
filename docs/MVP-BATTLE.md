# MVP-001: Motor de combate (Minecraft 1.21.1)

## Escopo entregue

- `/battle test`: abre sessão simulada e tela de combate, sem controlar mobs reais.
- Servidor: `BattleEngine` calcula a iniciativa e resolve ações; `BattleSessionManager` valida turnos, pacote e sessão.
- Rede: pacotes separados C2S (`BattleActionPayload`) e S2C (`BattleStatePayload`); cliente nunca envia dano/HP.
- Ações: `ATTACK` (30 dano, custo 1000), `GUARD` (próximo golpe tem metade do dano, custo 700), `FLEE` (encerrar teste).
- Inimigo: zumbi simulado executa ataque automático de 18 de dano (9 com defesa).
- Iniciativa: acumuladores começam em 0, aumentam por velocidade (120 vs 70 por passo lógico). Quando >=1000, o participante age; após agir paga o custo. Tempo lógico para no turno do jogador. O servidor continua rodando.
- Interface provisória: dois layouts 2D, `Elevated` e `Behind`, persistindo última escolha no cliente; **não é ainda uma câmera 3D**.

## Testes sugeridos

1. Abra `/battle test` em mundo seguro.
2. Verifique 120/120 HP para jogador e 90/90 HP para zumbi e timeline `Voce > Zumbi...`.
3. Ataque, observe vida do inimigo caindo 30; próximo turno id aumenta.
4. Defenda e confirme o próximo ataque inimigo com 9 de dano em vez de 18.
5. Ataque até zerar o HP do zumbi e confirme tela de vitória.
6. Inicie outra batalha, alterne a visão, encerre, reinicie Minecraft e confirme a persistência.
7. Inicie e saia via botão; confirme que consegue começar outra sessão.
8. Use `/battle resume` se a UI foi fechada por outra tela.
9. Em multiplayer, confirme que dois jogadores podem iniciar sessões independentes.
10. Teste desconectar/reconectar para verificar limpeza de sessão, sem crash do servidor.

## Aceite técnico

- [x] Código do motor não depende das classes Minecraft.
- [x] Rede e comandos passam ações pelo servidor.
- [x] Testes de unidade do motor incluídos.
- [ ] Rodar build no GitHub Actions (verificar resultado do PR).
- [ ] Testar UI, trocas de layout e persistência dentro do jogo.
- [ ] Testar servidor dedicado e dois jogadores.

## Fora do escopo

**Não há captura de aggro, mobs reais, cena lateral 3D, árvore de habilidades, grupos ou reforços ainda.** Os designs correspondentes seguem as RFC-001/002/003 no PR de arquitetura e serão implementados incrementalmente.

Não combinar com o PR #2 automaticamente: XP por kill vanilla não deve recompensar mobs simulados. Ao migrar para combate real, as recompensas deverão ocorrer apenas uma vez na conclusão da sessão.
