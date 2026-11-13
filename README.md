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

O comando verifica se o mod carregou. Também existe agora uma **primeira implementação experimental de evolução de personagem**, separada do XP de encantamento do Minecraft.

## Progressão individual (protótipo)

- Todos começam no **nível 1**.
- Matar um **monstro hostil** concede **20 XP de RPG** ao jogador responsável pela morte.
- O próximo nível custa **100 XP** no início e cresce **25 XP** por nível.
- **Nível máximo: 50**; ao atingi-lo, não são concedidos mais pontos de XP RPG.
- A experiência é **persistida por jogador** e preservada ao morrer.
- São valores provisórios para validar a implementação, não o balanceamento final do RPG.

### Comandos

| Comando | Quem pode usar | O que faz |
| --- | --- | --- |
| `/rpg` | Qualquer jogador | Mostra nível e progresso |
| `/rpg profile` | Qualquer jogador | Mesmo resultado, forma explícita |
| `/rpg xp add <jogador> <quantidade>` | OP nível 2+ | Concede XP RPG para testes |
| `/rpgstatus` | Qualquer jogador | Confirma que o mod carregou |

### Teste manual recomendado

1. Inicie um mundo com o mod e execute `/rpg profile` (nível 1 e 0/100 XP).
2. Mate **5 zumbis**: o XP deve chegar a 100 e o nível a 2.
3. Execute `/rpg xp add SeuNick 125` com trapaças habilitadas ou OP: deve alcançar o nível 3.
4. Saia e entre no mundo novamente; confirme que o nível permanece.
5. Morra e reapareça; confirme que o nível permanece.
6. Em multiplayer, confirme que outro jogador tem progressão independente.

**Ainda não implementamos atributos, classes, habilidades, magia, HUD nem XP por quests.**

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
- [x] Primeira compilação validada no GitHub Actions e no cliente (confirmado pelo autor)
- [ ] Validação da nova implementação de progressão
- [ ] Definição do balanceamento definitivo da progressão
- [x] Protótipo inicial de XP e níveis implementado (aguardando teste no jogo)

> O novo código de progressão só deve ser considerado validado após o build desta branch e o teste no jogo.
