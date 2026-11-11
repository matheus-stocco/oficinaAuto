# Pendências por módulo

Atualizar ao final de cada módulo (regra 4 do Arquivo Mestre). Marcar `[x]` quando resolvida.

## Gerais (valem para o projeto todo)
- [ ] Deploy no Dokploy (Railpack): configurar conforme `docs/dokploy.md` e confirmar `/api/saude` no domínio.
- [ ] Graphify: autorizar o conector e registrar a estrutura dos módulos já feitos (0, 1, 2, 3...).
- [ ] CEP (autopreenchimento de endereço): decidido para o fim do projeto; ainda sem fonte definida.
- [ ] Catálogo de modelos: ver Módulo 12.
- [ ] Confirmar no primeiro deploy: versões de Java e Node do Railpack e fallback de rotas do frontend (react-router).

## Módulo 0 — Base do projeto
- [ ] Deploy no Dokploy e registro no Graphify (ver Gerais).

## Módulo 1 — Acesso e funcionários
- [ ] Testes de integração exigem banco de teste dedicado e vazio (documentado no README); sem CI ainda.

## Módulo 2 — Estrutura visual e Configurações
- [ ] Testar na tela o envio de logo e de imagem de fundo (hoje só testado no backend e nos formulários).
- [ ] Componentes-base (botão, campo, tabela, modal) ainda são classes CSS; extrair quando repetir (iniciado no Módulo 3 com `Paginacao` e `Busca`).

## Módulo 3 — Clientes e Veículos
- [ ] Ficha do veículo com histórico de OS (lista paginada): depende das OS; entra no Módulo 5.
- [ ] Cadastro rápido de cliente + veículo na mesma tela, a partir da OS e do orçamento: entra nos Módulos 5 e 6.
- [ ] Busca de cliente e de veículo usa `like '%texto%'` sem índice próprio (suficiente para milhares de registros); avaliar `pg_trgm` se ficar lenta.
- [ ] Endereço do cliente é um único campo de texto; separar em CEP, cidade e UF só quando o CEP for integrado.
- [ ] Não existe exclusão de cliente, só anonimização (LGPD, mantém a contabilidade). Conferir com o dono se precisa de exclusão física.
- [ ] Anonimizar não cobre dados pessoais digitados em campos livres de OS futuras (observações): tratar no Módulo 5.

## Módulo 12 — Catálogo de modelos de veículos (planejado)
- [ ] Fonte: `gpupo/brazilian-cars` (`Resources/data/current/cars.yaml`, ~5,7 mil linhas de modelos; marca, modelo e versões como "1992 Gasolina"; licença MIT, manter o aviso). O repositório foi atualizado pela última vez em 2024-05, então carros mais novos não estão nele: por isso a tela aceita texto livre e o catálogo aprende com o que a oficina cadastra.
- [ ] Os dados vêm originalmente da tabela FIPE. A licença MIT é do repositório, não garante o direito sobre a origem: confirmar o uso comercial antes de vender.
- [ ] A API apifipe.com.br foi descartada (conta sem assinatura, resposta 402); a chave dela nunca entrou no repositório.
