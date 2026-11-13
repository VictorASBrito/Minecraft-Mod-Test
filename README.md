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

O cliente utiliza uma **entidade de câmera virtual, não adicionada ao mundo**, para apresentar o mapa em duas perspectivas de verdade:
- **Elevada/oblíqua:** enquadra jogador, zumbi e terreno em uma vista lateral a partir de uma posição alta;
- **Por trás:** enquadra o jogador de costas, com o zumbi à frente.

A câmera inicial é a última usada no cliente. A preferência fica em `config/minecraft_mod_test-battle-view.properties` e é restaurada após reiniciar o jogo. O antigo `BattleScreen` foi substituído por uma **HUD renderizada diretamente sobre o mundo**.

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
