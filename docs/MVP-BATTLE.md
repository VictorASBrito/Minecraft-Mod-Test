# MVP-002 — Combate no mapa real e câmeras posicionadas no mundo

**Base:** Minecraft 1.21.1, NeoForge 21.1.265, Java 21.  
**Objetivo:** substituir o desenho de personagens de uma Screen pelo mapa e entidades Minecraft 3D reais.

## Implementado neste protótipo

- `/battle test`: posiciona um zumbi **real** num bloco de chão vazio próximo ao jogador.
- O zumbi de teste tem IA, som e dano físico desabilitados; o motor mantém HP do combate separados do HP vanilla.
- O personagem permanece na posição de início, recebe proteção temporária contra danos externos, tem a movimentação contida pelo servidor e fica voltado ao inimigo.
- O mundo, outros jogadores e outras entidades continuam normalmente.
- `ClientBattleCamera`: mantém o jogador como entidade da câmera, força temporariamente `THIRD_PERSON_BACK` e gera a posição/rotação; o `BattleCameraMixin` aplica no final de `Camera.setup`:
  - `ELEVATED`: vista oblíqua lateral com ambos os participantes;
  - `BEHIND`: vista do personagem por trás.
- `BattleHudOverlay`: registrador `RegisterGuiLayersEvent`, sem tela separada nem blur, e suprime a mira vanilla enquanto a sessão estiver ativa.
- `ClientBattleControls`: teclas `J` atacar, `K` defender, `L` sair e `V` alternar câmera.
- O modo de visão é persistido em arquivo local; primeira batalha abre na visão elevada.
- Ao concluir a batalha: estado original de câmera restaurado, proteção desativada e zumbi de teste descartado.
- Limpeza de sessão no logout e no desligamento do servidor; zombies de teste persistidos acidentalmente num crash serão removidos ao carregar chunks, via marcador NBT.
- Reaproveita `BattleEngine`, validação de ações, sincronização de snapshots e testes determinísticos já existentes.

## Roteiro manual (ambiente de teste)

1. Entrar num mundo de teste em área aberta e plana, sem cavalos/barcos; ficar no chão.
2. `/battle test`: confirmar que o mapa não desaparece nem é coberto por Screen; zumbi surge aproximadamente 3–5 blocos à frente.
3. Confirmar avatar visível na visão lateral elevada. Com `V`, alternar para a câmera atrás do personagem.
4. `J` ataca, `K` defende, `L` encerra; confirmar feedback e linha de iniciativa na HUD.
5. Confirmar que o personagem não se desloca no mundo enquanto espera a escolha e que a câmera acompanha apenas os alvos da sessão.
6. Ao vencer ou fugir, confirmar que o zumbi desaparece, a câmera normal retorna e os controles de movimento voltam.
7. Iniciar segundo combate: deve utilizar a última visão selecionada.
8. Fechar/reabrir o Minecraft: confirmar preferência persistida.
9. Testar desconexão no meio do encontro; confirmar ausência de zumbi preso ou jogador invulnerável.
10. Executar build no GitHub Actions e, depois, teste em servidor dedicado.

## Limitações importantes

- **O dano é simulado**; o zumbi real está visível e imobilizado para validar os modelos e câmeras. Ainda não há combate físico e XP real.
- A câmera tem um raycast básico para encurtar o enquadramento diante de blocos, mas ainda não resolve terrenos muito fechados, grupos grandes ou todas as colisões.
- Sem entrada automática por aggro, seleção de mobs existentes, multiplayer cooperativo, grupos/reforços ou animações 3D.
- Testar somente em mundo descartável: movimento, invulnerabilidade e spawn são alterações temporárias no mundo, sujeitas a imprevistos de crashes.

## Aceite

- [ ] O mundo real e as entidades são visíveis, sem Screen ou placeholders 2D.
- [ ] Ambas as câmeras enquadram os participantes e não influenciam os turnos.
- [ ] O jogo memoriza a câmera preferida.
- [ ] A HUD responde às teclas e o servidor continua autoridade de dano e iniciativa.
- [ ] Ao encerrar, câmera, posição, proteção e mundo retornam a estado seguro.
- [ ] Logout/reconexão e servidor dedicado sem crash.

## Correção após capturas (câmeras 3D)

As duas capturas mostraram `FIRST_PERSON` e braço na tela, embora o HUD alternasse de modo. O sistema agora usa `THIRD_PERSON_BACK` com `Camera.setup` alterado via Mixin **somente quando o combate está ativo**, o que deve permitir renderizar o próprio avatar. A referência arquitetural é o [Cobblemon Cinematics](https://github.com/P1neapplell0/Cobblemon-Cinematics).

### Reteste visual obrigatório

- [ ] Visão elevada mostra jogador e zumbi completos sobre o mapa.
- [ ] Visão atrás mostra costas do jogador e o zumbi à frente, sem braço de primeira pessoa.
- [ ] Alternar com V não move o personagem nem consome turno.
- [ ] A mira vanilla desaparece na batalha e retorna ao sair.
- [ ] Encerrar a batalha restaura o modo de câmera anterior (F5).
- [ ] Testar árvores, paredes e obstáculos para validar o raycast provisório.
