# Roteiro de módulos

| Módulo | Status | Data | O que foi feito | O que ficou pendente |
|---|---|---|---|---|
| 0 Base do projeto e deploy | CONCLUÍDO (local) | 2026-11-11 | Backend Spring Boot 3.2 (Flyway, `/api/saude`, CORS por env, teste JUnit); frontend Vite/React (Vitest, teste de exemplo); `.env.example`; README; `docs/dokploy.md`; commits no GitHub | Deploy no Dokploy e registro no Graphify adiados por decisão do dono; fazer depois |
| 1 Acesso e funcionários | CONCLUÍDO (local) | 2026-11-11 | Login/logout por cookie JWT HttpOnly SameSite=Strict; troca de senha; funcionários com acesso opcional (CRUD paginado, inativar); bloqueio após 5 erros (15 min); proteção do último acesso; primeiro funcionário por `ADMIN_LOGIN`/`ADMIN_SENHA`; telas de login, rotas protegidas, funcionários e trocar senha. Testes: backend 22, frontend 6; teste manual com `curl` contra o banco local | Layout definitivo vem no Módulo 2; deploy e Graphify adiados |
| 2 Estrutura visual e Configurações | PENDENTE | | | |
| 3 Clientes e Veículos | PENDENTE | | | |
| 4 Serviços | PENDENTE | | | |
| 5 Ordens de Serviço | PENDENTE | | | |
| 6 Orçamentos | PENDENTE | | | |
| 7 Agenda | PENDENTE | | | |
| 8 Financeiro e Asaas | PENDENTE | | | |
| 9 Relatórios | PENDENTE | | | |
| 10 Dashboard "Hoje" | PENDENTE | | | |
| 11 LGPD, backup e fechamento | PENDENTE | | | |
| 12 Teste final com Playwright | PENDENTE | | | |
