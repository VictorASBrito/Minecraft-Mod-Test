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

## Batalha de teste por turnos (MVP)

O motor de batalha experimental possui **iniciativa dinâmica por velocidade**.
O jogo abre uma tela de combate com botões funcionais; o servidor resolve o resultado.
**Ainda não são encontros com monstros reais.**

### Como experimentar

1. Com **Minecraft 1.21.1, NeoForge 21.1.265 e Java 21**, inicie o ambiente com `./gradlew runClient` (Windows: `.\\gradlew.bat runClient`).
2. Entre em um mundo, em um lugar seguro. O mod **não interrompe os ticks do mundo** e a janela experimental não deixa o avatar fisicamente invulnerável.
3. Execute `/battle test` no chat (comando disponível para jogador normal).
4. Uma tela com **Você vs Zumbi (simulado)** aparecerá.
5. Use **Atacar** (30 de dano, custo de iniciativa 1000) ou **Defender** (reduz o próximo dano recebido de 18 para 9, custo 700).
6. O servidor usa velocidade 120 para jogador, 70 para zumbi, 120 HP vs 90 HP.
7. Ao vencer, perder ou selecionar **Sair do teste**, use **Fechar**.
8. `/battle resume` reabre uma sessão ativa, e `/battle cancel` termina uma sessão ativa.

O cálculo de iniciativa é **lógico**, não por tempo real: uma escolha pode esperar o tempo necessário, enquanto o restante do servidor continua funcionando.

### Duas apresentações (protótipos 2D)

- **Visão elevada:** ambos os personagens em lados opostos do campo.
- **Visão por trás:** personagem maior em primeiro plano, zumbi ao fundo.

Na tela, use o botão de visão no canto superior direito. O último layout é salvo em `config/minecraft_mod_test-battle-view.properties` para as batalhas futuras, inclusive após reiniciar o jogo.

**Atenção:** nesta versão as duas perspectivas são **esquemas gráficos 2D**, não câmeras 3D que renderizam as entidades reais. Isso será desenvolvido numa fase posterior.

### Segurança e limitações

- Não altera HP, inventário, IA, posição ou XP de entidades do mundo.
- Não concede recompensas nem XP pelo zumbi simulado.
- Cada jogador só pode ter uma sessão de teste por vez, em memória, encerrada ao desconectar ou parar o servidor.
- Pacotes de ação incluem ID da sessão e ID do turno; comandos duplicados ou fora de turno são ignorados no servidor.
- **Ainda não implementados:** detecção/aggro, agrupamento de mobs, reforços, escolha de alvos, multiplayer cooperativo, modelos 3D e a integração com o sistema de progressão do PR #2.

Confira também [documentação e roteiro de testes do motor](docs/MVP-BATTLE.md).
