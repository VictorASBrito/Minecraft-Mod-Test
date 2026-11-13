# RFC-001 — Combate lateral com turnos dinâmicos

**Status:** Proposta de arquitetura (não implementada)  
**Projeto:** Minecraft Mod Test — Minecraft 1.21.1 / NeoForge / Java 21  
**Escopo:** Batalhas táticas em apresentação lateral 2.5D, integradas a RPG sem classes obrigatórias.

## 1. Decisões já tomadas

1. O jogador explora, minera e constrói no Minecraft convencional, mas o combate RPG terá uma interface lateral inspirada em JRPGs clássicos.
2. Os turnos não serão rodadas fixas. O combate usa **iniciativa dinâmica baseada em velocidade**, com ações capazes de adiantar ou atrasar o próximo turno.
3. O jogador **não fica preso a uma classe**. Sua build é formada por disciplinas, habilidades ativas, passivas e especializações.
4. **Não será possível maximizar todas as árvores simultaneamente.** Existem limites de afinidade, pontos e capacidade de passivas.
5. **Redefinir especializações será possível mediante custo ou desafio**, a definir depois.
6. O estado de combate e o cálculo de efeitos pertencem ao servidor; a interface cliente apenas apresenta o estado e solicita ações.

## 2. Conceito de experiência

- **Exploração:** gameplay convencional de Minecraft; encontro iniciado por evento explícito/controlado.
- **Transição:** o cliente recebe estado de batalha e exibe cena lateral.
- **Batalha:** jogadores e inimigos agem em turnos por iniciativa, com ações e alvos validados no servidor.
- **Conclusão:** recompensas/XP são concedidos uma única vez; personagens retornam ao controle normal.
- **Multiplayer:** uma batalha é uma sessão independente; o restante do servidor não é pausado.

## 3. Iniciativa dinâmica (proposta inicial de algoritmo)

Cada participante possui:
- `speed` — velocidade efetiva positiva;
- `gauge` — iniciativa acumulada (inteiro com sinal, podendo ser temporariamente negativa após uma ação lenta);
- efeitos temporários que modificam `speed` e `gauge`.

Constantes conceituais:
- `READY_THRESHOLD = 1000`;
- ação comum: custo 1000;
- Defender: custo 700;
- Golpe pesado: custo 1400;
- Magia comum: custo 1200.

Em cada **passo lógico de combate**, `gauge += speed` para participantes capazes de agir. O motor avança em saltos até o próximo participante alcançar `gauge >= 1000`; **o tempo lógico fica parado enquanto o jogador escolhe uma ação**. Isso não pausa os ticks do mundo Minecraft. Após executar e validar a ação, `gauge -= actionCost`. Custos maiores retardam a próxima oportunidade. Alterações de velocidade passam a valer nos passos seguintes.

**Empates:** maior `speed` efetiva, depois prioridade explícita da ação, depois identificador determinístico da sessão. Não usar tempo real do cliente ou ordem aleatória sem semente.

**Regras de segurança/balanceamento:** velocidade positiva com limites configuráveis; custos com valor mínimo; processamento limitado de ações por atualização de servidor; um único participante ativo por sessão; efeitos e recursos validados pelo servidor. Estados incapacitantes precisam de política de perda de turno para evitar travamentos.

**Exemplo:** velocidades 120 (jogador), 90 (esqueleto) e 60 (zumbi), com ações de custo 1000, produzem a sequência ilustrativa jogador → esqueleto → jogador → zumbi → esqueleto. A ordem mudará se forem usados outros custos ou efeitos.

### Turnos de jogadores e decisões

- Ao ficar pronto, o jogador recebe sua vez sem pressão de relógio no singleplayer.
- Em multiplayer, a sessão pode aplicar um tempo limite configurável e ação padrão `DEFEND` para desconectados/inativos. **Política de timeout ainda não aprovada.**
- Não enviar ação sem confirmar que `battleId`, participante, turno atual, habilidade, recursos e alvo são válidos.
- Cliente pode exibir previsão da ordem, mas o servidor sempre recalcula a sequência após uma ação.

## 4. Modelo de batalha no servidor

Fluxo de estados previsto:

`CREATING → PREPARING → ACTIVE → RESOLVING → FINISHED`

Saídas excepcionais: `CANCELED` / `INTERRUPTED`, sempre com limpeza idempotente.

Entidades previstas:
- `BattleSession`: UUID, dimensão/origem, estado, participantes, relógio lógico, turno, lista de eventos e resultado;
- `BattleParticipant`: referência ao jogador/mob, equipe, HP/MP de batalha, velocidade, gauge, efeitos, posição visual e estado de conexão;
- `BattleAction`: identificador, origem, alvo(s), custos, prioridade, efeitos e condições de execução;
- `BattleEngine`: função de resolução de turno, danos, efeitos, vitória/derrota e próxima iniciativa;
- `BattleSessionManager`: cria, recupera e finaliza sessões; impede o mesmo jogador/mob em duas batalhas simultâneas.

**Separação importante:** o motor de regras deve ser Java testável sem renderização; adaptações para Minecraft (entidades, networking, áudio e recompensas) ficam em camadas próprias.

## 5. Apresentação lateral (proposta)

**Primeira entrega:** tela `BattleScreen` simples, com retratos/representações provisórias, turn order, ações, seleção de alvo e feedback. Sem câmera livre, sem teleporte e sem tentar alterar o renderizador global.

**Etapa posterior:** modelos 3D das entidades em cena 2.5D, animações, efeitos de habilidades, fundos que remetem ao bioma de origem, transições e áudio.

O cliente recebe snapshots de batalha e eventos de ação, não executa regras de dano.

Interfaces potenciais: `BattleScreen`, `BattleHud`, `TurnTimelineWidget`, `TargetSelector`.

## 6. Comunicação

NeoForge 1.21.1 suporta telas personalizadas e payloads cliente-servidor registrados em `RegisterPayloadHandlersEvent`.

Mensagens conceituais:
- **S2C:** `BattleStarted`, `BattleSnapshot`, `TurnStarted`, `ActionResolved`, `BattleEnded`;
- **C2S:** `ChooseAction`, `ChooseTarget`, `ReadyForBattle`.

As mensagens enviam apenas campos necessários e são validadas. Evitar confiar em HP/dano/velocidade informados pelo cliente. Eventos do servidor alimentam a UI por snapshot, possibilitando reconexão.

## 7. Integração ao mundo (decisão de implementação em aberto)

**Opção recomendada para protótipo:** /battle test cria sessão **simulada** jogador versus zumbi sem sequestrar entidades reais. Isso comprova fluxo, sincronização e combate sem riscos para o mundo.

**Para encontros reais:** associar entidades do mundo à sessão e definir uma política de contenção:
- impedir que participantes se movam/ataquem/interajam fora da batalha;
- evitar interferência de dano externo, IA vanilla e física;
- não congelar os ticks do mundo nem outras batalhas;
- lidar com lava, sufocamento, queda, terceiros, desconexão, unload de chunks e morte;
- restaurar estados corretamente em toda saída (vitória, derrota, cancelamento, falha);
- aplicar morte/recompensa **exatamente uma vez**.

Não assumir que uma simples chamada para desativar IA resolve todos esses problemas. Esses casos precisarão de testes de integração.

## 8. Progressão e habilidades

O núcleo de XP/níveis está em desenvolvimento no **PR #2** (`feature/rpg-progression-core`), ainda não integrado à `main` no momento desta RFC.

Integração futura:
1. Habilidade escolhida é definida por dados (`id`, `cost`, `targets`, `effects`, `requirements`), não diretamente na tela.
2. Árvore de disciplinas define **quais ações podem ser equipadas**, com pontos, afinidades e pré-requisitos.
3. Passivas ocupam capacidade limitada.
4. Especializações podem ser redefinidas mediante custo ou desafio, com regras contra troca durante a batalha.
5. XP de combate é concedido **no resultado da batalha** em vez de depender apenas do evento vanilla de morte. Ao migrar, evitar conceder XP duas vezes.
6. Evolução e dados persistentes por jogador; estados transitórios da batalha ficam em `BattleSession`.

**Ainda em aberto:** se o domínio das técnicas se mantém integralmente após a redefinição; quais disciplinas e seus limites; materiais/custos da redefinição.

## 9. Multiplayer, compatibilidade e riscos

- Primeira versão: apenas sessão simulada 1 jogador × 1 inimigo.
- Evoluir para grupos e múltiplos inimigos somente após testes da máquina de estados.
- PvP e criaturas de mods não são parte do primeiro protótipo.
- Mods auxiliares (Waystones, mapas, backpacks e otimização) não serão dependências obrigatórias do motor de combate.
- Não congelar o servidor inteiro nem usar `Thread.sleep` para esperar uma ação.
- Construir mecanismos de recuperação de sessão em disconnect/crash; definir política de recompensas e estado físico.
- Evitar acoplar câmera/GUI ao cálculo de turno.

## 10. Primeiro marco técnico (MVP)

**MVP 1 — Combate simulado via comando**:
1. `/battle test` inicia confronto simulado de um jogador contra um zumbi.
2. Servidor cria sessão, velocidade e HP próprios.
3. UI lateral inicial mostra participantes e próxima ordem.
4. Ações disponíveis: `ATTACK` e `DEFEND`.
5. Motor resolve turnos e envia snapshots ao cliente.
6. Vitória/derrota encerra sessão, sem afetar entidades reais nem conceder XP.
7. Testes unitários de iniciativa, custos, empate, alvo, impedir turno duplicado e encerramento.

**Critérios de aceitação:** nenhuma ação fora do turno; ordem reproduzível; batalha termina; cliente não calcula dano; novo encontro funciona após encerrar o anterior; servidor prossegue normalmente.

**MVP 2 — Integração real**: somente após MVP 1, ligar encontros a entidades do mundo, contenção, dano/recompensa, persistência e multiplayer.

## 11. Pontos para fechar com o autor do mod

1. Encontros começam por **contato/aggro**, comando de interação, ou ambos?
2. Inimigos do mundo ficam no lugar, congelados/protegidos, ou devemos criar uma dimensão/arena física?
3. Batalhas com outros jogadores: adesão automática, convite ou apenas participantes próximos?
4. Definição de derrota, fuga, desconexão e timeout.
5. Interface 2.5D: estilo visual dos modelos 3D, cenário e animações.
6. Custo da redefinição de afinidades e progressão que persiste após a troca.

## Referências técnicas

- [NeoForge 1.21.1 — Screens](https://docs.neoforged.net/docs/1.21.1/gui/screens/)
- [NeoForge 1.21.1 — Networking](https://docs.neoforged.net/docs/1.21.1/networking/)
- [NeoForge 1.21.1 — Data Attachments](https://docs.neoforged.net/docs/1.21.1/datastorage/attachments/)

> RFC é proposta. Nenhum código de combate foi implementado por este documento.
