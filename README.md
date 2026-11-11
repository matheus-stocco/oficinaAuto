# oficinaAuto

Sistema de gestão para oficina mecânica (mão de obra), uma aplicação por oficina.
O plano completo, módulo a módulo, está em [`Arquivo Mestre.md`](Arquivo%20Mestre.md);
o andamento em [`docs/ROTEIRO-MODULOS.md`](docs/ROTEIRO-MODULOS.md).

## Rodar localmente

Precisa de Java 17+, Node 22 e um PostgreSQL.

```bash
cp .env.example .env            # ajuste as variáveis e exporte-as no shell
cd backend && ./mvnw spring-boot:run
cd frontend && npm install && npm run dev   # http://localhost:5173 (proxy /api -> :8080)
```

## Testes

```bash
cd backend && ./mvnw test -Dtest='com.oficina.<modulo>.*Test'
cd frontend && npx vitest run src/<modulo>
```

## Deploy

Automático pelo Dokploy (Railpack), sem Docker. Veja [`docs/dokploy.md`](docs/dokploy.md).
