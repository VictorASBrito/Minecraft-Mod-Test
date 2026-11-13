# RFC-002 — Tela de combate e encontros por detecção

**Status:** Direção visual e gatilho de encontro aprovados; detalhes técnicos ainda em estudo  
**Projeto:** Minecraft Mod Test — Minecraft 1.21.1 / NeoForge / Java 21  
**Relacionada:** [RFC-001 — Motor de combate](./RFC-001-COMBAT-TURNOS.md)  
**Referências fornecidas pelo autor:** duas imagens enviadas na conversa de 13/11/2026 (ainda não adicionadas ao repositório).

> Este documento registra requisitos de experiência e critérios futuros. **Não há código de combate ou de câmera implementado nesta RFC.**

## 1. Decisões confirmadas

1. O jogador continua explorando o Minecraft normalmente, mas entra em um modo de batalha próprio ao encontrar inimigos.
2. A tela de batalha oferecerá **duas perspectivas alternáveis**, ambas com modelos/ambientes tridimensionais no estilo Minecraft:
   - **Visão lateral elevada/oblíqua**, com câmera mostrando o grupo e oponentes em lados opostos da arena e elementos de cenário ao fundo.
   - **Visão atrás do personagem ativo**, enquadrando costas/ombro do personagem e os inimigos à frente; comandos de combate dispostos ao redor do personagem.
3. A escolha da câmera é **apenas visual**. Ela não altera turnos, iniciativa, dano, posição lógica, alcance ou seleção válida de alvos.
4. **Encontros começam pela detecção de inimigos hostis**: o monstro precisa identificar o jogador como alvo e atender às condições adicionais de aproximação/segurança. O jogo não deve iniciar uma batalha porque qualquer monstro distante simplesmente existe no mundo.
5. O sistema de batalha continua com **iniciativa dinâmica baseada em velocidade e custo de ações** conforme a RFC-001.
6. O personagem não tem classe obrigatória, possui limites de afinidade e **poderá redefinir suas especializações mediante custo ou desafio**; é proibido trocar durante batalha.

## 2. Modo A — Visão lateral elevada / oblíqua

**Referência:** segunda imagem enviada (grupo de personagens em primeiro plano, monstros à frente, arena vista de cima e de lado).

- A câmera enquadra **toda a área relevante da batalha**, com leve elevação, perspectiva lateral/diagonal e profundidade 3D.
- Aliados ocupam um lado/primeiro plano; inimigos ocupam a área oposta. A distribuição precisa pode depender do número de participantes e do cenário.
- Personagens e monstros aparecem com modelos 3D, posições facilmente distinguíveis e barras de HP/recursos legíveis.
- A HUD apresenta: **linha de próximos turnos**, personagem ativo, HP/PM ou recursos aplicáveis, estado dos participantes, painel de ações e indicação de alvo.
- Em turnos inimigos, a câmera pode manter o enquadramento geral; zooms curtos e efeitos são opcionais, nunca devem esconder a próxima decisão.
- A visualização deve suportar diferentes tamanhos de equipe, inimigos grandes, obstáculos visuais e mudanças de resolução.

## 3. Modo B — Atrás do personagem ativo com menu ao redor

**Referência:** primeira imagem enviada (personagem visto pelas costas, inimigos no campo e botões SKILLS, ITEMS, GUARD, ESCAPE etc. flutuando ao redor).

- Câmera em terceira pessoa **atrás do personagem cujo turno está ativo** (centralizada ou levemente sobre o ombro), olhando para os adversários.
- **Menu contextual ao redor do personagem**, com ações inicialmente previstas: **Atacar, Habilidades, Itens, Defender, Fugir**. Ações especiais surgirão apenas quando o sistema correspondente existir.
- O menu deve ser legível e selecionar uma ação por mouse/teclado sem exigir movimentação da câmera. Controle para gamepad é desejável, mas não é requisito do primeiro protótipo.
- Seleção de alvo realça o inimigo correspondente e mostra HP/informações disponíveis; alvos não válidos não ficam selecionáveis.
- A linha de iniciativa e os dados essenciais de combate continuam acessíveis, mesmo com a HUD mais centrada no personagem.
- Durante uma ação, o menu pode recolher para mostrar animações, retornando quando a decisão do jogador é necessária.
- Em multiplayer, cada cliente acompanha seu personagem ou o participante ativo, sem expor controles de um turno de outro jogador.

## 4. Troca de perspectiva

**Requisito confirmado:** o jogador pode alternar as duas visões **durante a batalha**.

**Proposta de implementação (ajustável):**
- Controle de câmera discreto na HUD, com atalho configurável (por exemplo, `V`); evitar conflitar com atalhos do Minecraft ou de mods.
- Preferência **local ao cliente**, lembrada entre batalhas. A câmera de um jogador não força os demais a mudar a sua.
- Transição suave curta quando possível, com opção para desativar animação de câmera por acessibilidade.
- Alternar modos não encerra a batalha, não troca alvo, não consome turno, não modifica a iniciativa nem envia ação de combate ao servidor.
- Se a câmera escolhida não conseguir enquadrar adequadamente todos os participantes, aplicar correção automática de distância/enquadramento sem alterar a lógica de batalha.

**Pendência:** confirmar qual dos dois modos será o padrão inicial, a tecla padrão e as preferências de acessibilidade.

## 5. Encontros por detecção de inimigos

### 5.1 Requisitos de início

Um encontro no mundo real **só será elegível** quando:
1. O monstro for considerado hostil para nosso sistema e estiver vivo, disponível e fora de outra batalha.
2. O servidor identificar um jogador como alvo de hostilidade (não apenas uma entidade casualmente próxima).
3. O jogador estiver elegível, sem batalha em andamento e fora de regras de bloqueio/cooldown.
4. O monstro estiver próximo o suficiente, e a detecção cumprir as condições de percepção e estabilidade definidas no balanceamento (ex.: visão, tempo mínimo de perseguição e/ou distância).
5. O servidor conseguir criar e reservar a sessão de batalha de maneira atômica.

**Parâmetros a definir:** raio de entrada, tempo mínimo de aggro, linha de visão, imunidades temporárias, regras diurnas/noturnas, tratamento de monstros de outros mods.

O jogador **não precisa atacar ou receber dano primeiro** para começar o encontro.

### 5.2 Proteção contra encontros excessivos

- Não criar múltiplas sessões simultâneas para o mesmo jogador ou o mesmo mob.
- Reservar um único encontro quando vários monstros detectarem o mesmo jogador ao mesmo tempo.
- Definir como agrupar outros monstros próximos antes de abrir a cena de batalha.
- Evitar cadeia imediata de batalhas na volta ao mundo após vitória, fuga, derrota ou desconexão: considerar período de graça configurável.
- Não capturar mobs neutros por simples proximidade; sua hostilidade depende de regras explícitas.
- Manter o restante do mundo Minecraft e outros jogadores funcionando.

### 5.3 Multiplayer (proposta, não aprovada)

- Servidor decide a composição de um grupo e quem recebe convite/entra automaticamente.
- Jogadores próximos não devem ser capturados numa batalha sem regras claras.
- A sessão não deve interromper outros encontros ou a exploração de terceiros.

## 6. Dados e responsabilidades técnicas

| Camada | Responsabilidade |
| --- | --- |
| `EncounterDetector` | Observa alvo/aggro e requisitos de distância, visibilidade, cooldown e elegibilidade no servidor |
| `BattleSessionManager` | Reserva participantes e impede sessões duplicadas |
| `BattleEngine` | Calcula turnos dinâmicos, ações e resultados independentemente da câmera |
| `BattleScreen` | Exibe HUD comum, alvo selecionado e modo de câmera atual |
| `ElevatedBattleView` | Enquadramento lateral elevado, área completa de batalha |
| `BehindPlayerBattleView` | Câmera atrás do personagem ativo e ações ao redor |
| `BattleCameraController` | Alterna visões e preferências locais; não resolve regras do RPG |

Os nomes acima são **propostas arquiteturais**, não classes existentes.

**Separação visual/lógica:** sincronizar snapshots de turno, entidades/participantes e ações com o cliente. O servidor calcula o estado autoritativo; a câmera é local e pode seguir animando independentemente dos ticks da batalha.

## 7. Etapas de implementação

### MVP 1 — Simulação segura
- `/battle test` cria uma batalha simulada sem congelar, teleportar ou tomar controle de mobs do mundo.
- Mostrar o campo e um menu funcional com ações **Atacar** e **Defender**.
- Implementar a primeira perspectiva **elevada**; testar interface e motor.
- Implementar a perspectiva **atrás do personagem ativo** e a alternância local; as duas precisam estar disponíveis antes de concluir o marco visual.
- Validar turno por velocidade, escolha de alvo, vitória, derrota e retorno ao mundo.
- Criar testes automatizados do motor e testes manuais de troca de câmera.

### MVP 2 — Detecção real
- Conectar aggro de monstro à avaliação de encontro no servidor, com proteção contra duplicatas.
- Iniciar e encerrar uma sessão com mobs reais sob política de contenção/restauração segura.
- Testar cooldown, múltiplos monstros, terrenos perigosos, jogadores próximos e desconexão.

### MVP 3 — Grupos, habilidades, acabamento visual
- Suporte cooperativo, mais inimigos, efeitos animados, variações de cenário e novas habilidades.
- Integração com árvores de especialização e recompensas de XP ao final, sem duplicar eventos vanilla.

## 8. Critérios de aceitação da tela

- [ ] Os **dois modos** mostram aliados, inimigos e alvo sem perda de informações essenciais.
- [ ] É possível alternar a visão durante batalha sem consumir turno ou alterar estado do servidor.
- [ ] Visão elevada mantém o combate legível com vários participantes.
- [ ] Visão atrás do personagem apresenta opções ao seu redor, selecionáveis sem movimentar o avatar.
- [ ] Inimigo ativo/turno ativo/HP e próximos turnos são discerníveis em ambas as visões.
- [ ] Resoluções diferentes e controles de câmera não impedem selecionar ações.
- [ ] Animações e câmera não bloqueiam permanentemente o jogador nem deixam a HUD presa após o combate.
- [ ] Duas pessoas numa batalha podem ter perspectivas diferentes nos respectivos clientes.

## 9. Critérios de aceitação da detecção

- [ ] Monstro hostil com alvo válido e condições de aproximação completas inicia encontro.
- [ ] Monstro distante, sem aggro ou fora das condições **não** inicia encontro.
- [ ] Um mesmo jogador não recebe dois encontros simultâneos.
- [ ] Vários monstros detectando juntos não provocam várias telas concorrentes.
- [ ] O mundo e jogadores não envolvidos continuam funcionando.
- [ ] Vitória, derrota, fuga e desconexão permitem limpeza da sessão com segurança.

## 10. Ainda precisamos decidir

- **Qual visão será padrão** quando uma batalha começar.
- Raio/tempo de detecção e regras exatas para mobs neutros, chefes e entidades de mods.
- Seleção e quantidade de inimigos por encontro.
- Como aliados próximos entram no combate (convite, proximidade ou grupo).
- Política de contenção física dos mobs no mundo.
- Regras de fuga, derrota, reconexão e timeout.
- Custo/material/desafio para redefinição de afinidades e o que permanece aprendido.
- Decidir quando e como serão usadas animações 3D específicas.

Este documento não afirma que as telas ou encontros já estão implementados.
