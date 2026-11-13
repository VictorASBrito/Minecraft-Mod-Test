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

Esses pacotes **ainda não foram criados**; são diretrizes, não funcionalidades disponíveis.

## Primeiro teste

O comando `/rpgstatus` é registrado no evento de comandos do NeoForge e responde com uma mensagem simples. Ele é deliberadamente pequeno para validar o carregamento inicial antes de acoplar sistemas grandes.

## Pendências de design

- Progressão individual vs. global.
- Sistema de atributos e níveis.
- Classes fixas, livres ou híbridas.
- Quests próprias ou integração com FTB Quests.
- Como persistir dados e sincronizá-los no multiplayer.
- Organização das configurações para servidores.
