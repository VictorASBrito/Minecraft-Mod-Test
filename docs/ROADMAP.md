# Roadmap (rascunho)

## Fase 0 — Fundação técnica
- [x] Fixar Minecraft 1.21.1, NeoForge e Java 21.
- [x] Estruturar projeto Gradle e metadados do mod.
- [x] Criar comando mínimo `/rpgstatus`.
- [x] Confirmar build de fundação no GitHub Actions e teste local relatado pelo autor.
- [ ] Confirmar execução no cliente e num servidor dedicado.

## Fase 1 — Projeto do RPG
- [ ] Escolher fantasia/ambientação e identidade do mod.
- [ ] Definir progressão individual e progressão global.
- [ ] Definir se haverá classes, atributos e árvore de habilidades.
- [ ] Selecionar o primeiro sistema mínimo jogável.

## Fase 2 — Vertical slice
- [x] Implementar XP por derrota de monstros hostis e níveis provisórios.
- [ ] Validar XP e persistência no cliente, após o novo build.
- [x] Implementar persistência via AttachmentType e cópia ao morrer.
- [ ] Testar persistência e multiplayer no jogo.
- [x] Criar feedback por comandos e mensagens do servidor.
- [ ] Criar HUD (quando definirmos o design).
- [x] Escrever testes de unidade da curva de XP.
- [ ] Verificar critérios de aceitação manualmente no cliente.

## Fase 3 — Compatibilidade
- [ ] Criar instância com Sophisticated Backpacks e dependências.
- [ ] Adicionar Waystones, Xaero's e mods de qualidade de vida.
- [ ] Testar mods de otimização sem exigir sua instalação.
- [ ] Documentar versões validadas e conflitos conhecidos.

Nenhum item de fase posterior representa funcionalidade concluída.
