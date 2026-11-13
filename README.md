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
