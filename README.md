# Minecraft Mod Test

Protótipo de **mod de RPG e fantasia** para **Minecraft Java 1.21.1**, usando **NeoForge** e **Java 21**.

> O nome e o `mod_id` são provisórios. O projeto está na fase de fundação técnica; classes, magia, progressão e exploração serão especificadas antes da implementação.

## Stack definida

| Item | Escolha |
| --- | --- |
| Minecraft | 1.21.1 |
| Loader | NeoForge 21.1.265 |
| Java | JDK 21 |
| Build | ModDevGradle 2.0.148 / Gradle 9.2.1 |
| Mod ID provisório | `minecraft_mod_test` |

Os arquivos de Gradle foram adaptados do [MDK oficial de 1.21.1](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle), fixado no commit `e93d7f09d4eec12c76d1d7be9bf4eaad0246e9e0`.

## Como executar no Windows

1. Instale o **JDK 21** e o **Git**. Verifique `java -version` e `git --version`.
2. Clone o repositório e abra um terminal PowerShell na pasta do projeto.
3. Execute:

```powershell
.\gradlew.bat runClient
```

Na primeira execução, o script baixa automaticamente o **Gradle Wrapper JAR do MDK oficial fixado**, verifica seu hash Git e depois inicializa o Gradle. O Gradle baixará as dependências necessárias.

Para gerar o JAR do mod:

```powershell
.\gradlew.bat build
```

O arquivo será gerado em `build/libs/`.

## Teste de funcionamento

1. Inicie o cliente de desenvolvimento com `runClient`.
2. Entre em um mundo.
3. No chat, execute `/rpgstatus`.
4. Verifique se a mensagem de confirmação do protótipo aparece.

O comando apenas demonstra que o mod carregou e que eventos do servidor estão funcionando. **Ainda não existe um sistema de evolução de personagem**.

## Dependências auxiliares

Sophisticated Backpacks, Waystones, Xaero's Minimap/World Map, JEI, Jade e mods de otimização **não são dependências obrigatórias** do nosso mod. Serão testados numa instância separada conforme o desenvolvimento evoluir.

## Organização

- `src/main/java/` — Java do mod.
- `src/main/resources/` — textos e recursos.
- `src/main/templates/` — metadados gerados no build.
- `docs/ARCHITECTURE.md` — escolhas arquiteturais.
- `docs/ROADMAP.md` — etapas planejadas.
- `.github/workflows/build.yml` — validação automatizada de compilação.

## Status

- [x] Base NeoForge 1.21.1 / Java 21
- [x] Comando de diagnóstico `/rpgstatus`
- [ ] Compilação e teste no cliente confirmados
- [ ] Definição das regras de progressão
- [ ] Primeiro sistema jogável de RPG

> A compilação ainda precisa ser confirmada pelo GitHub Actions ou por um teste local.

## Batalha no próprio mundo — câmera 3D real (MVP 2)

O comando `/battle test` agora **não abre uma tela de batalha separada**: utiliza a renderização 3D do próprio Minecraft. O servidor posiciona **um zumbi real, imobilizado e protegido** próximo do jogador e mantém ambos nos locais iniciais enquanto o combate está ativo. A batalha usa atributos/HP **simulados** pelo `BattleEngine`, e não causa dano nem oferece XP/drops vanilla.

O cliente mantém **o jogador como entidade da câmera**, usa terceira pessoa e aplica posição e direção no final de `Camera.setup` via Mixin. A arquitetura aproveita a abordagem pública do Cobblemon Cinematics, sem exigir o Cobblemon como dependência. As duas perspectivas disponíveis são:
- **Elevada/oblíqua:** enquadra jogador, zumbi e terreno em uma vista lateral a partir de uma posição alta;
- **Por trás:** enquadra o jogador de costas, com o zumbi à frente.

A câmera não utiliza mais `FIRST_PERSON`: o avatar completo deve aparecer no mundo. O enquadramento também testa colisão com blocos. A câmera inicial é a última usada no cliente. A preferência fica em `config/minecraft_mod_test-battle-view.properties` e é restaurada após reiniciar o jogo. O antigo `BattleScreen` foi substituído por uma **HUD renderizada diretamente sobre o mundo**.

### Controles de teste (configuráveis em Opções > Controles)

| Tecla | Função |
|---|---|
| `J` | Atacar |
| `K` | Defender |
| `L` | Fugir / sair da batalha |
| `V` | Alternar entre as duas câmeras |

### Teste

1. Use um **mundo de teste** e procure um local plano, com chão e dois blocos livres na frente.
2. Inicie o mod com `./gradlew runClient` (Windows: `.\\gradlew.bat runClient`).
3. Entre no mundo e execute `/battle test`. O servidor tentará colocar um zumbi real a aproximadamente quatro blocos do jogador.
4. Veja se a câmera mostra **o próprio mapa**, os blocos, seu avatar e o zumbi.
5. Pressione `V` para alternar as posições de câmera. Use `J` e `K` para resolver os turnos.
6. Vença a batalha ou pressione `L` para sair. A câmera original deve ser restaurada e o zumbi de teste removido.
7. Inicie outra batalha e verifique a preferência de câmera.

`/battle resume` reenvia um snapshot se houver batalha ativa; `/battle cancel` cancela no servidor.

**Importante:** nesta etapa o zumbi é uma entidade real **usada como representação visual**, mas os pontos de vida/danos ainda são os da simulação. Ainda não estamos iniciando encontros automaticamente por aggro, nem aplicando dano físico real, agrupamento, reforços ou XP. Isso virá depois de validar câmera, HUD, contenção de entidades e servidor dedicado.

Ao iniciar uma batalha, somente o jogador envolvido recebe proteção temporária de dano e imobilização; essas propriedades são restauradas ao encerrar o teste. Outros jogadores e o mundo continuam funcionando. Para evitar efeitos inesperados em mundos importantes, não use essa implementação experimental fora de um ambiente de teste.

Consulte também [o plano de testes do combate in-world](docs/MVP-BATTLE.md).

### Correção da câmera após o primeiro teste visual

As capturas mostraram o braço de primeira pessoa e os participantes fora do quadro. A câmera virtual `ArmorStand` foi substituída por transformações aplicadas diretamente a `Camera.setup`, mantendo o jogador como origem do render e forçando `THIRD_PERSON_BACK` somente durante a batalha.

A mira vanilla é ocultada enquanto a sessão estiver ativa. A perspectiva original do jogador é restaurada ao encerrar.

Referência técnica: [Cobblemon Cinematics](https://github.com/P1neapplell0/Cobblemon-Cinematics). Não há dependência de Cobblemon.

**Necessário reteste no Minecraft:** o build verifica Java e testes automatizados, mas não garante o enquadramento visual real.

### Bloqueio de controles e foco no alvo (correção experimental)

Durante uma sessão ativa, a câmera pode continuar em seu modo escolhido, mas **WASD, pulo, agachar, sprint, ataques vanilla, uso de itens, troca de hotbar, mineração e abertura de contêineres são bloqueados**. O cliente zera os impulsos antes do movimento, e o servidor cancela ataques e interações para evitar que pacotes tardios quebrem o mundo.

Ao pressionar **J**, se o servidor aceitar o ataque no turno correto, o personagem se orienta para o zumbi e executa uma animação de ataque, sem forçar a troca da câmera. O foco é temporário. Quando o combate termina, os controles normais devem voltar.

**Validação pendente:** teste em jogo real (incluindo criativo, sobrevivência, multiplayer e abertura de baús). Use um mundo descartável até confirmarmos que os bloqueios não interferem em outros mods.

## Interface de combate atualizada (HUD compacta)

A interface deixa o centro do mundo visível e é idêntica nas câmeras lateral elevada e atrás do jogador.

- **Canto superior esquerdo:** retratos do jogador e dos inimigos, em sequência de iniciativa, com a primeira ação destacada e uma seta apontando o próximo participante.
- **Parte inferior esquerda:** retrato da skin do jogador e sua vida *simulada de RPG* (não confundir com os corações vanilla). A condição **DEFENDENDO** aparece quando aplicável.
- **Parte inferior:** cinco ações com ícones: **Atacar**, **Habilidade**, **Defender**, **Item** e **Fugir**. Apenas Atacar [J], Defender [K] e Fugir [L] funcionam nesta fase; as demais ficam visivelmente desabilitadas.
- **Sem informação de HP de inimigos:** removida tanto da HUD quanto dos pacotes de estado do cliente. O motor de combate do servidor continua controlando a vida do monstro.
- **Sem título da câmera ou batalha no topo:** apenas a timeline de rostos; tecla [V] continua alternando a câmera.
- Hotbar, mira, corações vanilla, fome, armadura e barra XP vanilla são ocultados durante a HUD de batalha e voltam depois.
- O layout se adapta a janelas menores: status e ações ficam em duas linhas quando a largura não é suficiente.

Os retratos atuais usam a skin do próprio jogador e a textura oficial do zumbi. A previsão atual ainda é 1×1, portanto **não há retratos reais de outros jogadores e criaturas no pacote de rede**; isso será feito quando implementarmos o sistema de grupos.

Próxima etapa planejada: **tela de resultados** com XP do RPG, itens/drop e progressão. O zumbi de teste ainda não concede recompensas, então a HUD apenas exibe uma mensagem de vitória provisória.

### Como experimentar esta versão

```powershell
git fetch origin
git switch feature/combat-hud-redesign
git pull --ff-only origin feature/combat-hud-redesign
.\\gradlew.bat runClient
```

Use `/battle test` e confira os dois modos com [V]; [J] ataca, [K] defende e [L] termina o teste.

## Resultados de batalha e recompensas (MVP 3)

Após vitória, derrota ou fuga, o mod **restaura a câmera e os controles normais** e abre uma tela própria de resultados com experiência RPG, nível e itens recebidos. A barra de XP avança gradualmente e informa quando um novo nível foi alcançado.

### Como testar

**Teste normal (sem recompensas):** `/battle test` — continua funcionando como antes e apresenta a tela de resultado com **0 XP e nenhum item**.

**Teste com recompensas REAIS (exclusivo para operadores nível 2+):** `/battle test-rewards` — apenas em caso de vitória, entrega **20 XP de RPG** e **1 Carne Podre** ao inventário (ou deixa o item no chão caso esteja cheio). O comando é restrito para impedir que jogadores comuns gerem itens e níveis infinitamente.

1. Use um mundo de testes, em local plano, com cheats habilitados (ou permissão OP em servidor).
2. Execute `/rpg profile`: o nível deve começar em 1 com 0/100 XP, caso ainda não tenha XP salvo.
3. Execute `/battle test-rewards`, vença com **J** e veja a tela de resultados.
4. Confirme **+20 XP**, novo total, nível e ícone do item obtido; clique em **Continuar**.
5. Execute `/rpg profile` novamente e confira o XP persistido.
6. Repita cinco vitórias para testar a subida do nível 1 para 2. Feche e reabra o mundo e confira novamente.
7. Teste **fuga (L)** e **derrota**: não devem conceder recompensas.
8. Teste `/battle test` normal: resultado sem XP/itens e nenhuma alteração no inventário.

### Regras de segurança

- Recompensas são calculadas **somente no servidor** quando a sessão termina; não existe pacote cliente solicitando XP ou itens.
- O registro da sessão é removido antes de conceder recompensas, impedindo processamento duplicado por comando/pacote antigo.
- O XP RPG é independente da experiência de encantamento vanilla, salvo como `AttachmentType` persistente, com cópia após morte.
- A tela mostra exclusivamente o comprovante enviado pelo servidor; não inventa itens ou XP.
- A vida exata do zumbi continua oculta do cliente.

**Nota de integração:** este PR incorpora as classes de progressão do PR #2, mas NÃO incorpora a recompensa por evento vanilla de morte de mob. Quando os encontros reais forem implementados, conceder XP somente no fechamento da sessão para evitar duplicação.

## Comandos de combate com mouse ou teclado

Durante um encontro, agora aparece um **cursor de mouse livre** sobre a mesma HUD compacta. Isso significa que a câmera permanece na perspectiva lateral ou por trás, mas não pode mais ser girada com o mouse enquanto o jogador escolhe suas ações.

- **Clique esquerdo:** selecione os cartões Atacar, Defender e Fugir na parte inferior (realce dourado ao passar o mouse).
- **Teclado:** J = Atacar, K = Defender, L = Fugir e V = alternar câmera; os atalhos continuam personalizáveis em Opções > Controles.
- Os cartões Habilidade e Item continuam desabilitados, sem executar ações.
- A camada de captura é uma `Screen` transparente, **não uma segunda arena nem uma tela escurecida**; o mundo, o avatar e os inimigos continuam renderizados.
- O bloqueio no servidor para ataques e interações vanilla é mantido; cliques da interface não podem minerar blocos nem abrir baús.
- Ao encerrar a batalha, a tela de resultados assume o lugar da interface e a câmera normal é restaurada. Ao fechar o resultado, o cursor volta ao comportamento vanilla.
- Esc não fecha a sessão sem confirmação do servidor; use Fugir (L ou clique).

### Como testar

```powershell
git fetch origin
git switch feature/battle-mouse-actions
git pull --ff-only origin feature/battle-mouse-actions
.\\gradlew.bat runClient
```

Use `/battle test` e, se quiser testar recompensas e possuir permissão de operador, `/battle test-rewards`.

## Seleção de alvos e batalha 1x2 (MVP 5)

Esta etapa introduz **múltiplos inimigos no motor** com iniciativa individual, mas mantém o combate no próprio mapa 3D e os ataques físicos/HP vanilla separados das regras RPG.

- **`/battle test`** continua iniciando o teste 1x1 conhecido.
- **`/battle test-group`** inicia um teste 1 jogador versus **dois zumbis** no mapa (precisa de espaço aberto para os dois).
- Dois **cartões de alvo compactos** aparecem no alto à direita, mostrando nome, ícone e seleção, **sem revelar HP inimigo**.
- Clique no cartão de um zumbi ou pressione **Tab** para alternar o alvo. Ao clicar **Atacar** ou pressionar **J**, apenas o zumbi selecionado recebe o dano RPG.
- Cada zumbi possui velocidade e turno próprios; a sequência de retratos da iniciativa mostra **Zumbi 1** e **Zumbi 2** separadamente.
- Zumbis derrotados desaparecem do mundo, seus cartões ficam inativos e eles deixam de agir. A vitória só chega quando todos estiverem derrotados.
- O servidor valida `battleId`, `turnId`, `targetIndex` e se o alvo está vivo. Pacotes repetidos ou alvos mortos não gastam turnos nem causam dano.
- As câmeras continuam as mesmas, agora mirando o centro do grupo; ao atacar, o avatar olha para o alvo específico.
- O teste de grupo **não concede XP ou loot**; `/battle test-rewards` continua exclusivo para operadores na batalha 1x1, para preservar a segurança da progressão.

### Teste
```powershell
git fetch origin
git switch feature/battle-multi-target
git pull --ff-only origin feature/battle-multi-target
.\gradlew.bat runClient
```

Em local plano de um mundo de testes, execute `/battle test-group`, escolha o **Zumbi 2** pelo mouse e ataque até derrotá-lo, observando a mudança da ordem de iniciativa. Em seguida, derrote o **Zumbi 1** e confirme que a tela de resultados abre sem recompensas. Repita usando Tab e J.

**Pendente:** clicar diretamente no modelo 3D do monstro, animações avançadas, encontros por aggro, reforços dinâmicos, multiplayer cooperativo e proteção especial contra obstrução da câmera em construções fechadas.

## MVP 7 — Encontros por detecção e reforços dinâmicos (experimental)

Este primeiro marco automático suporta **somente zumbis vanilla**. Enquanto você explora em Sobrevivência ou Aventura, o servidor reconhece um zumbi que está perseguindo você de verdade, dentro de 6 blocos e com linha de visão, após 20 ticks de aggro estável. Não basta um monstro distante existir no mapa.

**Grupos iniciais:** até 3 zumbis que já perseguem o mesmo jogador e são visíveis num raio de 12 blocos. Não são criadas cópias: são as entidades do mundo que participam da sessão.

**Reforços:** em qualquer batalha, até 2 zumbis reais adicionais podem entrar, em no máximo 2 ondas. É necessário haver um monstro elegível perseguindo o jogador a até 16 blocos. O servidor reserva o mob e a HUD avisa a chegada por duas ações do motor; o grupo não excede 3 monstros vivos simultâneos nem 5 históricos. Sem candidato, não surge reforço artificialmente.

**Pós-batalha:** 300 ticks (15s) de proteção contra novos encontros RPG, sem invulnerabilidade fora da batalha. Em fuga/derrota, mobs sobreviventes recuperam seu estado vanilla. Após crash, as marcações de reserva permitem restaurar as flags ao recarregar chunks.

**Recompensa natural provisória:** vitória paga 20 XP de RPG e 1 carne podre por zumbi derrotado, até 5 mobs; derrota/fuga não paga. Entidades derrotadas são descartadas sem loot vanilla para evitar recompensas duplicadas. Este modelo não preserva as loot tables/equipamentos especiais do Minecraft, que serão integrados posteriormente.

**Controle administrativo (OP):** `/battle auto off` desativa novos encontros automáticos e varreduras de reforços. `/battle auto on` reativa. O ajuste é temporário e volta para ligado após reiniciar o servidor. Os comandos manuais `/battle test`, `/battle test-group` e `/battle test-rewards` permanecem disponíveis.

### Roteiro de teste

1. Entre num mundo descartável em Sobrevivência. Use `/time set night` com cheats para evitar zumbis queimando ao sol.
2. Aproxime-se de um zumbi que realmente esteja perseguindo você; espere cerca de um segundo com ele a menos de seis blocos.
3. Teste dois ou três zumbis perseguindo o jogador simultaneamente para validar o grupo inicial.
4. Deixe outro zumbi hostil por perto (até 16 blocos, fora do grupo inicial). Se houver espaço, ele poderá ser anunciado como reforço.
5. Clique em alvos distintos, teste Tab, J, K, L e V. Verifique que a vida dos zumbis continua oculta.
6. Vença e valide a experiência e os itens no resultado. Depois fuja de outro confronto e confira que mobs sobreviventes voltam à IA vanilla.
7. Confira se não se inicia imediatamente outra batalha RPG nos 15 segundos seguintes.
8. Use `/battle auto off`, aproxime-se de um zumbi hostil e confirme que a batalha automática não começa; reative depois.

**Limitações conhecidas:** somente zumbis vanilla; linha de visão não equivale a navegação/caminho real por cavernas; IA de monstros equipados, chunks, multiplayer e falhas abruptas devem ser testados antes de usar este código num mundo importante. Os parâmetros são conservadores, fixados em `EncounterRules`, e ainda não são o balanceamento definitivo.
