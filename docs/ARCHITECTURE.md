# Arquitetura inicial

## Objetivo

Criar um mod de RPG e fantasia para Minecraft 1.21.1 que seja modular, com espaço para sistemas próprios de evolução, combate, magia e exploração.

## Decisões confirmadas

- **Minecraft 1.21.1**, **NeoForge** e **Java 21**.
- **ModDevGradle** com estrutura baseada no MDK oficial.
- Lógica de progressão importante deve ser validada no lado do **servidor**, evitando que o cliente determine estados permanentes.
- Mods de mochila, mapas, teletransporte e otimização são **opcionais** e não devem ser requisitos de compilação para o núcleo do RPG.

## Organização de código planejada

Conforme implementarmos sistemas reais, organizar pacotes por responsabilidade:

- `progression/` — estado e regras de evolução.
- `classes/` — arquétipos, especializações ou profissões, se aprovados.
- `magic/` — habilidades e magias, se aprovadas.
- `combat/` — mecânicas relacionadas a combate.
- `network/` — sincronização cliente-servidor, quando necessária.
- `client/` — interface e renderização no lado cliente.

O pacote `progression/` já foi criado com a curva de experiência, armazenamento persistente e eventos de combate. Os demais pacotes continuam como diretrizes futuras.

## Primeiro teste

O comando `/rpgstatus` é registrado no evento de comandos do NeoForge e responde com uma mensagem simples. Ele é deliberadamente pequeno para validar o carregamento inicial antes de acoplar sistemas grandes.

## Pendências de design

- Progressão individual vs. global.
- Sistema de atributos e níveis.
- Classes fixas, livres ou híbridas.
- Quests próprias ou integração com FTB Quests.
- Como persistir dados e sincronizá-los no multiplayer.
- Organização das configurações para servidores.

## Módulo inicial: progressão individual

- `ExperienceCurve`: converte XP total em nível e progresso sem dependência do Minecraft.
- `ProgressionAttachments`: armazena XP total em *data attachment* do jogador, com `Codec.INT`, persistência e `copyOnDeath()`.
- `ProgressionService`: autoridade do servidor para consultar e conceder XP.
- `ProgressionEvents`: recompensa provisória de 20 XP ao derrotar monstros hostis.
- `RpgProgressionCommand`: consulta pessoal e concessão de XP protegida por permissão de operador.
- `ExperienceCurveTest`: testes unitários da matemática de experiência.

O XP de RPG **não substitui nem altera** níveis ou orbes de experiência vanilla. Não há sincronização geral cliente-servidor do dado de XP: a interface atual usa mensagens enviadas pelo servidor. Quando houver HUD, será preciso criar um canal de sincronização próprio.

Regra provisória: nível inicial 1, máximo 50, custo inicial 100 XP e incremento de 25 XP por nível. XP persistente é armazenado como valor total cumulativo, com teto de 34.300 XP. O sistema poderá ser substituído por regras de progressão de RPG decididas posteriormente.
