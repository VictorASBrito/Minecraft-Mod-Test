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
