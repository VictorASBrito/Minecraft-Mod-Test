# RFC-003 — Formação de grupos inimigos e reforços dinâmicos

**Estado:** conceito aprovado; parâmetros numéricos ainda propostos para testes  
**Projeto:** Minecraft Mod Test — Minecraft 1.21.1 + NeoForge + Java 21  
**Relacionado:** [RFC-001: motor de combate](./RFC-001-COMBAT-TURNOS.md) · [RFC-002: câmeras e encontros](./RFC-002-TELA-COMBATE-E-ENCONTROS.md)

> **Decisão confirmada:** todas as batalhas podem receber **reforços dinâmicos**, desde que haja inimigos elegíveis, respeitando limites rígidos de ameaça, participantes e frequência. Isso NÃO significa que obrigatoriamente chegarão reforços em todos os encontros. Os inimigos não aparecem artificialmente sem uma causa (salvo encontros especiais explicitamente definidos no futuro).

## 1. Objetivo

Formar encontros coerentes com a exploração de Minecraft, sem transformar qualquer grupo próximo em uma horda instantânea. Utilizar **proximidade + percepção/hostilidade + custo de ameaça** para escolher o grupo inicial. Durante a batalha, permitir reforços reais de outros monstros elegíveis, mantendo os turnos dinâmicos, o foco visual das duas câmeras e o servidor responsivo.

## 2. Regras aprovadas

1. Um inimigo hostil que **detectou o jogador** e cumpre as condições de aproximação inicia o encontro (RFC-002). Não exigir contato ou ataque primeiro.
2. O iniciador e outros monstros elegíveis **podem formar um único grupo inicial**. Distância sozinha não basta: é preciso verificar hostilidade, área acessível, estado e limites de ameaça.
3. **Qualquer batalha** (comum ou especial) é elegível a **reforços dinâmicos** enquanto estiver ativa. Reforços entram apenas se existir candidato qualificado e ainda houver capacidade de participantes/ameaça/ondas.
4. Reforços são **anunciados previamente na interface e na linha de iniciativa**, para permitir reação estratégica. A câmera atual do jogador é mantida.
5. O jogador **não deve receber encontros infinitos** nem múltiplas batalhas sobrepostas. Encerrar a batalha limpa reservas, evita duplicação de XP/itens e aplica intervalo de proteção no mundo.

## 3. Parâmetros iniciais sugeridos (NÃO aprovados como balanceamento definitivo)

| Configuração | Padrão para protótipo | Significado |
| --- | ---: | --- |
| `encounterTriggerDistance` | 6 blocos | Monstro com alvo válido pode iniciar batalha ao se aproximar |
| `encounterAggroMinTicks` | 20 ticks (1 s) | Tempo mínimo de detecção/hostilidade estável |
| `initialGroupSearchRadius` | 12 blocos | Busca de candidatos iniciais a partir da posição do encontro |
| `reinforcementSearchRadius` | 16 blocos | Busca de candidatos a reforços durante a batalha |
| `maxVerticalSeparation` | 5 blocos | Filtro preliminar de diferenças de altura; não substitui acessibilidade real |
| `maxInitialEnemies` | 4 | Total de inimigos na formação inicial |
| `initialThreatBudget` | 6 pontos | Teto de ameaça dos inimigos iniciais |
| `maxConcurrentEnemies` | 6 | Total de inimigos vivos/ativos simultaneamente |
| `maxReinforcementsPerBattle` | 2 | Total de novos inimigos adicionais ao longo da sessão |
| `maxReinforcementWaves` | 2 | Número máximo de ondas no mesmo encontro |
| `reinforcementsPerWave` | 1 | Limite de novos participantes numa onda |
| `totalEncounterThreatBudget` | 12 pontos | Ameaça acumulada inicial + reforços, independentemente de mortes |
| `reinforcementWarningActions` | 2 ações | Janela de aviso antes de o reforço entrar |
| `reinforcementScanIntervalTicks` | 20 ticks | Frequência máxima sugerida da avaliação de candidatos em batalha |
| `reinforcementMinResolvedActions` | 2 ações | Evita várias ondas sem dar turnos aos participantes |
| `postBattleGraceTicks` | 300 ticks (15 s) | Proteção contra reabertura imediata de encontros após a batalha |

Esses números devem ser guardados como **configuração de servidor** e avaliados em testes singleplayer, multiplayer e masmorras. Um "tick" acima é o tick normal do servidor; "ação" é uma ação resolvida pelo motor de batalha. **Não confundir o relógio do mundo com o relógio lógico de iniciativa.**

### Valores ilustrativos de ameaça

| Inimigo | Custo sugerido |
| --- | ---: |
| Zumbi | 1 |
| Esqueleto | 2 |
| Aranha | 2 |
| Creeper | 3 |
| Elite configurado pelo mod | 5 |

Valores são exemplos e exigem teste. Inimigos de mods externos devem usar regras/fallback de custo explícitos. Chefes e encounters roteirizados poderão ter orçamento próprio, **nunca ilimitado por padrão**.

## 4. Formação do grupo inicial

**Entradas:** jogador detectado, monstro iniciador, mundo/dimensão, posição de encontro, configuração do servidor.

1. **Validar o gatilho:** mob iniciador vivo e hostil, alvo correto, distância/aggro mínimos, jogador elegível, sessão ainda não iniciada.
2. **Reservar o iniciador:** criar proposta de encontro contendo obrigatoriamente o inimigo que detectou o jogador. Não iniciar se ele não puder ser reservado.
3. **Buscar candidatos próximos:** apenas mobs da mesma dimensão, vivos, não reservados, hostis ao jogador ou associados a comportamento de grupo elegível.
4. **Filtrar acessibilidade:** distância tridimensional, diferença vertical, condições de visão/linha de perseguição ou caminho conhecido; excluir mobs atrás de paredes sólidas ou em cavernas desconectadas. A checagem exata de navegação precisa ser validada com APIs reais.
5. **Ordenar deterministicamente:** a partir de regras de prioridade explícitas (por exemplo: perseguindo o jogador, mesma patrulha, menor distância, identificador estável para empate).
6. **Preencher a formação:** selecionar candidatos enquanto respeitarem `maxInitialEnemies` e `initialThreatBudget`, sem exceder custo acumulado. Iniciador nunca é descartado por haver candidatos mais baratos.
7. **Reservar atomicamente:** antes de mostrar a batalha, vincular jogador e todos os mobs selecionados a **uma única `BattleSession`**. Se algum candidato deixou de ser elegível, reavaliá-lo/descartá-lo; nenhum participante fica em duas batalhas.
8. **Criar a apresentação:** organizar posições laterais do grupo e enviar snapshot autorizado pelo servidor às duas modalidades de câmera.

**Sem spawn de inimigos extras:** a formação básica seleciona somente entidades existentes e elegíveis no mundo. Monstros excluídos permanecem no mundo e não recebem dano/recompensa da sessão.

## 5. Reforços dinâmicos durante a batalha

### Elegibilidade

Um candidato a reforço deve:
- existir no mundo, estar vivo e não estar em outra sessão;
- ter sido detectado pelo sistema durante a batalha por aproximação/aggro, pela hostilidade compartilhada ou por alerta explícito de facção (a definir por comportamento);
- estar na mesma dimensão/área alcançável; não poder surgir através de paredes apenas por estar no raio;
- não ultrapassar `maxConcurrentEnemies`, `maxReinforcementsPerBattle`, `maxReinforcementWaves`, `reinforcementsPerWave` ou `totalEncounterThreatBudget`;
- ter uma **reserva atômica** antes de ser anunciado. Não reservar candidatos indiscriminadamente.

### Ciclo do reforço

1. A sessão ativa, em frequência limitada, verifica possíveis reforços perto da posição de origem/área do encontro. O servidor usa orçamento de processamento e não faz varredura completa em todo tick de cada batalha.
2. Encontrando candidato válido e capacidade disponível, o servidor o **reserva** e cria `PendingReinforcement` com identidade, custo, onda e entrada prevista.
3. A interface de combate mostra **"Reforço se aproximando"** e um marcador na linha de ações. O sistema prevê a chegada após **duas ações resolvidas** (parâmetro), independentemente do tempo de escolha do jogador.
4. Na chegada, o servidor verifica novamente vida, existência, reserva e elegibilidade; se o candidato estiver indisponível, cancela a entrada sem duplicar entidades.
5. Se válido, o reforço entra no grupo inimigo, ocupa uma posição e recebe iniciativa inicial definida pelo motor (sem agir instantaneamente antes de o aviso ser útil).
6. Após a entrada, o sistema mantém o registro dos custos **cumulativos** e aplica o intervalo mínimo entre ondas.

**Nunca** adicionar um reforço diretamente durante a animação de resolução de uma ação: aplicar alterações de grupo em uma fronteira consistente da máquina de estados e emitir snapshots de batalha versionados. Cliente não cria participantes nem decide quem chega.

### Se os inimigos atuais forem derrotados

- Se existir um **reforço já reservado e anunciado**, a batalha só pode aguardar sua chegada por uma janela curta/definida; ao cancelar/perder a elegibilidade, finalizar a sessão imediatamente.
- Se **não houver reforço pendente**, finalizar a vitória no momento normal, **sem começar novas varreduras** na tentativa de prolongar a luta.
- Reforços não são criados após vitória/derrota/encerramento e não prorrogam indefinidamente uma batalha.

### Situações especiais

- Em batalhas contra chefes, reforços funcionam sob o mesmo sistema, mas podem ser provocados por habilidades do chefe ou eventos de batalha. Ainda se aplicam limites próprios configurados para o encontro.
- Mobs neutros só entram se estiverem efetivamente hostis ou cumprirem regra especial.
- Aliados **jogadores** não são reforços inimigos; adesão ao combate cooperativo é sistema diferente, ainda não definido.
- Caso o mundo carregue/descarregue chunks, o monstro morra, despareça ou seja capturado por outro encontro, cancelar sua reserva/pêndencia com segurança.

## 6. Exemplo ilustrativo

Um zumbi detecta o jogador. Perto dele há outro zumbi, um esqueleto, um creeper e um esqueleto atrás de uma parede.

- **Iniciador:** zumbi (ameaça 1).
- **Grupo inicial selecionado:** zumbi (1) + outro zumbi (1) + esqueleto (2) = **4 pontos** e 3 participantes.
- **Creeper:** pode ser excluído por custo (adicionaria 3 pontos e ultrapassaria o orçamento inicial sugerido de 6).
- **Esqueleto atrás da parede:** excluído por falta de acessibilidade/percepção.

Durante a batalha, um monstro válido se aproxima e o sistema anuncia **um reforço**. Se couber no limite acumulado e de participantes, ele entra após o aviso de duas ações. Poderá haver uma segunda onda **somente se** surgir outro candidato válido e houver orçamento e slots.

Nada disso obriga uma batalha comum a ter reforços se não existirem candidatos próximos.

## 7. Relação com combate por turnos e interface

- A câmera é **preferência local persistida**: última câmera usada, com fallback lateral elevada (RFC-002). O reforço não força mudança de câmera.
- O topo da HUD apresenta iniciativa futura e marcador de reforço; um aviso informa tipo/quantidade quando permitido.
- Alterar elenco por reforço obriga **recalcular a previsão de turnos**, mas não resetar a iniciativa dos participantes atuais.
- A nova criatura entra em posição visual disponível em ambas as visões (lateral elevada e atrás do personagem), sem sobrepor menus/HP.
- O motor controla chegada durante a ordem lógica; alertas temporais **não dependem da taxa de quadros nem do tempo real do jogador**.

## 8. Persistência, multiplayer e antiexploit

- `BattleSessionManager` mantém reserva exclusiva de jogador e mob e trata encerramento idempotente.
- Uma entidade do mundo corresponde a **um** participante; não clonar mobs nem criar cópias no mundo para a animação.
- O monstro reservado não deve agir simultaneamente na IA vanilla enquanto pertence à sessão (a contenção física será projetada/testada na fase real).
- Um servidor dedicado permanece responsável pelos cálculos; pacote cliente com entrada de reforço é apenas informativo.
- Morte física e recompensa/XP ocorrem exatamente uma vez por entidade participante, **após resolver o resultado**, com proteção contra o evento vanilla duplicar XP RPG.
- Estados de desconexão, unload de chunks e crash precisam cancelar/recuperar reservas com segurança. A política de reconexão ainda não foi definida.
- Impor limite de trabalho por sessão e por tick de servidor; evitar varredura de todas as entidades do mundo a cada passo.

## 9. Plano de implementação

**Marco A — Motor simulado (sem mobs reais):**
- [ ] Testar seleção do grupo inicial com uma coleção de candidatos fictícios.
- [ ] Testar critérios de custo, formação determinística e exclusão de paredes/camadas simuladas.
- [ ] Suportar `PendingReinforcement` no motor e anúncios de chegada.
- [ ] Testar limite cumulativo, janela de aviso, intervalo entre ondas e ausência de reforços.
- [ ] Simular batalhas que terminam com ou sem pendências, sem travar.

**Marco B — Integração com o mundo:**
- [ ] Capturar aggro dos monstros na mesma região da sessão.
- [ ] Reservar entidades de maneira exclusiva e manter validação de distância/percepção.
- [ ] Validar IA, dano externo, chunks descarregados, múltiplos jogadores e desempenho em servidor.
- [ ] Associar recompensas e persistência após a conclusão da batalha.

**Marco C — Balanceamento:**
- [ ] Ajustar limites por dificuldade, região/masmorra e composição do grupo.
- [ ] Avaliar regras específicas para chefes, criaturas de outros mods e facções.

## 10. Critérios de aceitação

- [ ] Um inimigo próximo e agressivo inicia uma sessão única.
- [ ] Só mobs elegíveis e alcançáveis são selecionados para o grupo inicial.
- [ ] Formação e escolha de reforços respeitam limites de slots, ameaças e ondas.
- [ ] Reforços podem ocorrer em qualquer categoria de batalha, mas não surgem se não houver candidato.
- [ ] A chegada é anunciada com antecedência e atualiza a linha de iniciativa sem forçar mudança de câmera.
- [ ] Jogador/mob não aparece em duas batalhas ao mesmo tempo.
- [ ] Sem reforços pendentes, a vitória encerra a batalha imediatamente.
- [ ] Reforço cancelado (morte, unload, inválido) não bloqueia o encerramento.
- [ ] XP e drops não são concedidos mais de uma vez.
- [ ] Outras sessões e jogadores continuam funcionando sem congelamento do servidor.

## 11. Pontos pendentes de decisão

- Números definitivos de raio, ameaça, quantidade e intervalos.
- Se monstros sem alvo direto no jogador, mas da mesma patrulha/facção, poderão integrar uma batalha.
- Alcance e regras de percepção por água, terreno irregular, paredes, cavernas e altura.
- Composição de encontros por biomas, masmorras e progressão do mundo.
- Se encontros com vários jogadores terão orçamento de ameaça maior e como entram aliados.
- Regras de fuga/derrota e seus efeitos sobre monstros físicos e período de graça.

> Apenas **a política de reforços dinâmicos com limites** e a **formação por proximidade, comportamento e ameaça** ficam aprovadas. Os números acima são valores iniciais para experimentação, não balanceamento final. Nenhum código de agrupamento ou reforço foi implementado ainda.
