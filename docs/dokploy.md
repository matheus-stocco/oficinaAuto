# Configuração no Dokploy (Railpack, sem Docker)

Três recursos no mesmo projeto do Dokploy, todos ligados a este repositório (branch `main`,
auto-deploy ligado):

1. **PostgreSQL** (serviço de banco do Dokploy). Anote host interno, banco, usuário e senha.
2. **backend** (Application, build type **Railpack**)
   - Diretório raiz (Build Path): `backend`
   - Variáveis: `DB_URL=jdbc:postgresql://<host-interno>:5432/<banco>`, `DB_USER`,
     `DB_PASSWORD`, `UPLOADS_DIR=/data/uploads`
   - Volume (Mounts): volume persistente montado em `/data/uploads`
   - Healthcheck: `GET /api/saude`
3. **frontend** (Application, build type **Railpack**)
   - Diretório raiz (Build Path): `frontend`

**Domínio único:** no frontend, domínio da oficina no caminho `/`; no backend, o **mesmo
domínio** no caminho `/api` (sem remover o prefixo), com HTTPS ligado.

Pontos a confirmar no primeiro deploy (dependem da versão do Railpack):
- o Railpack usa Java 17 (arquivo `backend/.java-version`) e Node 22 (`frontend/.node-version`);
- o front publicado serve `index.html` em qualquer rota (necessário para o react-router).
Se algum falhar, ajuste e registre em `docs/DECISOES.md`.
