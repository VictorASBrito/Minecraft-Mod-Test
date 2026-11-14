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

## Reteste de regressão — bloqueio de ações durante combate

Implementação: cliente com `MovementInputUpdateEvent` e `InputEvent` para impedir comandos vanilla; servidor com `BattleInteractionGuard` para rejeitar ataques e interações mesmo que um pacote chegue atrasado; orientação por ação de ataque confirmada via snapshot e `BattleTargeting`.

- [ ] Após `/battle test`, segurar WASD, pular, agachar e correr: o personagem **não anda nem volta tremendo ao ponto inicial**.
- [ ] Pressionar e segurar o mouse esquerdo contra zumbi e blocos: **sem dano vanilla e sem quebra de blocos**.
- [ ] Mouse direito em baú, bloco e entidade: **nenhum inventário de contêiner ou interação vanilla é aberto**.
- [ ] Rolar o mouse, usar números da hotbar, Q, F e tecla de inventário: sem mudanças indevidas na batalha.
- [ ] Pressionar J: ataque executado uma única vez no turno correto e o **personagem vira em direção ao zumbi**.
- [ ] Mover o mouse durante a escolha de ação: não muda o custo de iniciativa nem desloca o avatar; câmera selecionada continua válida.
- [ ] Pressionar V: alternância e memória da câmera continuam funcionando.
- [ ] Pressionar L ou vencer: movimento, mineração, baús e inventário voltam ao normal.
- [ ] Testar em criativo e sobrevivência, inclusive tentando abrir um baú durante o combate.
- [ ] Testar logout, reinício e dois jogadores em servidor dedicado para verificar que somente o participante fica bloqueado.

**Escopo:** ainda não há ataque físico do mob nem combate multiplayer; o bloqueio só é aplicado a sessões experimentais ativas.

## HUD compacta — checklist de aceitação visual

- [ ] Câmera atrás e lateral mostram a mesma HUD sem título superior.
- [ ] Ordem de turnos aparece no alto à esquerda com rostos de jogador/zumbi e seta abaixo do próximo ator.
- [ ] Vida exata do zumbi **não aparece** na UI nem está presente no pacote `BattleStatePayload`.
- [ ] Status do jogador exibe skin, vida RPG simulada e estado de defesa quando aplicável.
- [ ] Atacar/Defender/Fugir funcionam via J/K/L; Habilidade/Item aparecem desabilitados e não executam ações.
- [ ] O topo central e a maior parte do mapa permanecem desobstruídos.
- [ ] HUD vanilla (hotbar, corações, fome, XP e mira) fica oculta durante o combate e reaparece após vitória ou fuga.
- [ ] Renderização não sobrepõe painéis em GUI scale 2/3/4 e em janela pequena.
- [ ] O foco no inimigo e o bloqueio de movimento/interações da branch anterior continuam funcionando.

### Limites do MVP

A timeline atualmente usa nomes da previsão 1×1 (`Voce`/`Zumbi`) para escolher textura. Na fase de múltiplos participantes, o servidor enviará **IDs tipados, UUID e tipo de entidade** para permitir retratos corretos de outros jogadores e monstros sem inferir por texto. A futura tela de resultados não deve inventar XP ou itens enquanto o sistema de recompensas não estiver implementado.

## MVP 3 — Resultados e recompensas

- [ ] `/battle test` termina com tela de resultado, sem XP nem drop.
- [ ] `/battle test-rewards` exige permissão de operador; 1 vitória concede +20 XP RPG e 1 Carne Podre real.
- [ ] Sair pelo botão Continuar restaura o mundo e os controles.
- [ ] A barra XP anima de antes para depois, mostrando subidas de nível.
- [ ] `/rpg profile` confirma XP antes/depois e o XP permanece após reiniciar e morrer.
- [ ] Fuga, derrota, cancelamento e pacotes duplicados não concedem recompensas indevidas.
- [ ] Itens excedentes caem aos pés do jogador se o inventário estiver cheio.
- [ ] Nenhuma experiência de encantamento vanilla é concedida na batalha de teste.
- [ ] A vida exata do monstro não é enviada no snapshot.
- [ ] Testar cliente com servidor dedicado e dois jogadores independentes.

**Política temporária:** batalha de teste normal continua sem recompensas. Somente a modalidade OP `test-rewards` concede XP/loot. Nenhum monstro hostil do mundo real entra automaticamente em encontros nesta fase.
