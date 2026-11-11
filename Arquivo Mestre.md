# ARQUIVO MESTRE — Sistema de Oficina Automotiva (construção do zero)

> Cole este arquivo inteiro como primeira mensagem de uma sessão nova do Claude Code
> aberta na raiz do repositório `oficinaAuto`. Execute **um módulo por vez**, na ordem. Só avance
> quando o critério de aceite do módulo atual passar **no ambiente online** (seção 2).

---

## 1. Papel e objetivo

Você é um desenvolvedor sênior full-stack. Você **não conhece este projeto**: siga este
documento passo a passo, módulo por módulo, e não invente funcionalidade que não esteja
aqui. Construa do zero o sistema de gestão de uma oficina mecânica de forma **simples, muito
bonita, minimalista, leve e fácil de manter**.

Modelo de venda (decisão fechada, não repergunte):

- **1 aplicação por oficina**, em **1 VPS dedicada** por oficina (single-tenant).
- **Sem multi-tenant, sem planos, sem cobrança embutida.** Não existe `oficina_id` no banco.
- A oficina é de **mão de obra**: **não existe controle de estoque** em nenhuma parte.

Quem usa: **uma única pessoa, o atendente, pelo computador ou pelo celular**. Os
mecânicos **não usam o sistema**; são apenas funcionários cadastrados para constar nas OS
e nos relatórios.

Princípios (valem para toda decisão):

1. **Simplicidade de uso:** poucas telas, poucos toques, textos claros em português.
2. **Simplicidade de código:** o caminho mais curto que resolve. Sem camada, interface,
   biblioteca ou configuração que este documento não peça.
3. **Visual:** minimalista e profissional. **Proibido emoji.** Só ícones monocromáticos
   (`lucide-react`), sempre com o mesmo traço e tamanho.
4. **Leve:** nenhuma lista sem paginação; nenhum bundle desnecessário.
5. **Responsivo de verdade:** toda tela e todo botão funcionam em **qualquer tamanho de
   tela** (celular, tablet e computador), dentro das boas práticas de usabilidade.
6. **Testado:** nenhum módulo é concluído sem testes unitários passando.

---

## 2. Ambiente de desenvolvimento = produção de teste (Dokploy)

Todo o trabalho roda **direto na VPS** (Hostinger), gerenciada pelo **Dokploy**. O build
e a publicação são **automáticos**, feitos pelo **Railpack** do Dokploy. Cada `git push`
publica o sistema **online na hora**, e o desenvolvedor humano testa cada módulo ali.

**Proibido Docker no projeto:** nenhum `Dockerfile`, `docker-compose.yml` ou
`.dockerignore`. O projeto é só código-fonte e o Railpack detecta e constrói sozinho.

Consequências, que são regras:

1. **Nunca faça push de código quebrado.** Rode os testes e suba a aplicação antes de cada
   commit. Um commit ruim derruba o ambiente de teste.
2. **Migrations só para frente.** Nunca edite uma migration Flyway já commitada; crie uma
   nova. O banco da VPS persiste entre deploys (volume do Postgres).
3. **Segredos ficam no painel do Dokploy** (variáveis de ambiente), nunca no git.
   Versione só `.env.example`.
4. **Healthcheck:** exponha `GET /api/saude` (sem login, devolve só `{"status":"ok"}`) e
   configure-o no Dokploy, para um deploy com falha não substituir o que está no ar.
5. **Estrutura de deploy (monorepo, 3 recursos no Dokploy):**
   - **Banco:** serviço PostgreSQL do próprio Dokploy (com volume persistente).
   - **Backend:** aplicação com diretório raiz `backend/`. O Railpack detecta o Maven.
     Lê a porta de `PORT` (`server.port=${PORT:8080}`).
   - **Frontend:** aplicação com diretório raiz `frontend/`. O Railpack detecta o Vite e
     publica o build estático.
   - **Um domínio só:** o frontend atende `/` e o backend atende o caminho `/api` (sem
     remover o prefixo), configurado nos Domínios do Dokploy. Assim front e API ficam na
     mesma origem, o que o cookie `SameSite=Strict` exige. HTTPS por conta do Dokploy.
   - Versões de Java e Node são fixadas por arquivos do projeto (ex.: `.java-version`,
     `.node-version`) e confirmadas no primeiro deploy.
6. Uploads (logo, fundo) ficam num **volume montado no backend pelo Dokploy**
   (`/data/uploads`, caminho vindo de variável de ambiente), nunca dentro do código
   publicado, para sobreviver a deploys.
7. Antes de um módulo que **altera tabelas existentes**, avise o humano para fazer um
   backup do banco pelo Dokploy.

---

## 3. Ponto de partida (repositório novo)

Este repositório (`oficinaAuto`) começa **praticamente vazio** (só um README). **Não há
código para reaproveitar:** crie `backend/` e `frontend/` no Módulo 0.

Um protótipo antigo existiu em outro repositório e serviu só de aviso. **Não repita os
erros dele:**

1. `node_modules`, `target/`, `bin/` e arquivos de editor commitados no git. Crie o
   `.gitignore` **antes do primeiro commit de código**.
2. Senha do banco e outros segredos fixos em arquivo de configuração.
3. CORS fixo em `localhost`, duplicado em dois lugares.
4. `spring.jpa.hibernate.ddl-auto=update`: proibido; use migrations.
5. Sistema sem login.
6. Números fixos no dashboard, no lugar de dados reais.
7. Item de menu duplicado ("Ordens de Serviço" aparecia duas vezes).

---

## 4. Decisões fechadas (não repergunte)

| Tema | Decisão |
|---|---|
| Estoque | **Não existe.** Sem catálogo de peças, custo, margem, baixa ou alerta. |
| Pessoas | `funcionario` (com acesso opcional = usuário) e `cliente` são tabelas separadas. Não existe tabela `usuario`. |
| Perfis | **Não existem perfis.** Quem tem login faz tudo. Mecânico = funcionário sem login. |
| Cliente | **Totalmente opcional.** Só nome e telefone são necessários para cadastrar. A OS pode existir sem cliente e até sem veículo. |
| Relatório por cliente | **Não existe.** Existe o **Histórico de OS** (consulta por placa, nome, telefone, período). |
| Orçamento | **Módulo separado** (tabela, menu, telas). Ver Módulo 6. |
| Valores da OS | Serviços + Mão de obra + Peças (repasse) + Taxa de compra − Desconto. Ver Módulo 5. |
| Financeiro online | **Asaas**, atrás da interface `GatewayPagamento`. Maquininha (InfinitePay, PagBank ou Mercado Pago) é decisão futura. |
| Relatórios | Google Charts + tabela paginada + PDF de cada um. |
| Marca e modelo do veículo | Campos digitados até o Módulo 12. No **Módulo 12** (penúltimo) o autocomplete passa a vir de um **catálogo no próprio banco**, populado a partir do repositório `gpupo/brazilian-cars`. **Sem API externa de FIPE.** |
| CEP | **Fora desta reconstrução.** Decidido ao terminar tudo. Não implemente e não deixe ganchos. |
| Plataforma | **Responsivo em qualquer tela**, acessível pelo navegador do celular e do computador. **Sem PWA** (sem instalação nem modo offline). |

Menu lateral (fixo, 4 grupos que expandem), mais o rodapé:

| Grupo | Submenus |
|---|---|
| **Cadastros** | Clientes, Veículos, Funcionários, Serviços |
| **Movimentações** | Ordens de Serviço, Orçamentos, Agenda, Financeiro |
| **Relatórios** | os relatórios do Módulo 9 |
| **Ajuda** | Manual de uso, Atalhos e dicas, Sobre (versão e suporte) |

- O logo da oficina no topo do menu leva ao **Início "Hoje"** (dashboard).
- **Rodapé do menu:** à esquerda, avatar e nome do usuário logado, com seta que abre um
  menu (**Ajuda, Trocar senha, Alternar modo escuro/claro, Sair da conta**); à direita, a
  **engrenagem de Configurações**.

Fora de escopo (não implementar; anotar em `docs/ROADMAP.md`): estoque, fornecedores,
nota fiscal, multi-oficina, app nativo, perfis de permissão, fotos da OS, comissão,
agenda com alerta/conflito/recorrência, CRM, API externa de FIPE, CEP.

---

## 5. Modelo de dados (resumo; crie por migrations, nomes em português)

Campos comuns de contato (nome, telefone, e-mail, endereço) vivem numa **única classe
reutilizada** (`@Embeddable`) por funcionário e cliente. Não crie tabela `pessoa` com
herança.

- `funcionario`: nome, telefone, cpf (opcional), ativo, **login** (nulo = sem acesso),
  **senha_hash** (nulo = sem acesso).
- `cliente`: nome, telefone, e-mail, documento, endereço, observações (tudo opcional,
  exceto nome e telefone), `anonimizado_em`.
- `veiculo`: placa (normalizada, única, opcional), modelo, ano, cor, cliente_id (opcional).
- `servico`: nome, preço padrão, ativo.
- `ordem_servico`: número sequencial, cliente_id (opc.), veiculo_id (opc.), km_entrada,
  defeito_relatado, observacoes, funcionario_id (mecânico, opc.), previsao_entrega,
  status, e o **bloco de valores** abaixo.
- `os_item`: os_id, servico_id (opc., nulo = avulso), descricao, quantidade, preco_unit.
- `orcamento` e `orcamento_item`: mesmos campos de valores e itens da OS, com cliente
  avulso (`cliente_nome`, `cliente_telefone`) ou cliente_id; `gerado_em`.
- `agendamento`: data, hora (opc.), cliente_id ou nome/telefone avulsos, veiculo_id ou
  placa/modelo avulsos, funcionario_id (opc.), observacoes, status
  (`AGENDADO`, `REALIZADO`, `CANCELADO`, `NAO_COMPARECEU`), `agendamento_servico`.
- `pagamento`: os_id, valor, forma (`DINHEIRO`, `PIX`, `CARTAO`), data, origem
  (`MANUAL` ou `ASAAS`), id_externo (único, para idempotência).
- `receber` (fiado): os_id, valor, vencimento, pago_em.
- `despesa`: descricao, valor, data, categoria.
- `empresa` (1 linha): nome, cnpj, telefone, endereço, logo, fundo (cor ou imagem),
  `orcamento_rascunho_horas` (padrão 24).
- `auditoria`: quem, quando, o quê (mudança de status da OS, reabertura, anonimização).

**Bloco de valores (OS e Orçamento), calculado sempre no backend, em `BigDecimal`:**

- `mao_de_obra` (valor) e `mao_de_obra_descricao` (texto opcional).
- `pecas_valor` (repasse) e `pecas_descricao` (texto: "o que foi utilizado").
- `taxa_compra` (valor **ou** percentual sobre `pecas_valor`; opcional).
- `desconto` (valor **ou** percentual sobre o total bruto; opcional).
- `total = Σ(itens) + mao_de_obra + pecas_valor + taxa_compra − desconto`
- `receita_oficina = Σ(itens) + mao_de_obra + taxa_compra − desconto`
- `repasse_pecas = pecas_valor` (**nunca** entra na receita da oficina).
- Regra: `desconto` não pode passar de `Σ(itens) + mao_de_obra + taxa_compra`.

Implemente o cálculo **uma vez** (`CalculoValores`) e use em OS, Orçamento, PDF e
relatórios. Não duplique a fórmula.

---

## 6. Regras técnicas

- **Stack definida:** Spring Boot 3, Java 17, PostgreSQL, React + Vite + react-router +
  `lucide-react`. Não troque de framework.
- **Banco:** Flyway (`backend/src/main/resources/db/migration`), `ddl-auto=validate`.
  Dinheiro em `NUMERIC(12,2)`/`BigDecimal`, nunca `double`.
- **Paginação (obrigatória):** toda rota de lista usa `Pageable` (20 por página, máximo
  100, ordenação definida). Nenhuma rota devolve a tabela inteira. Colunas de busca e
  filtro têm índice. O dashboard usa consultas agregadas, nunca carrega listas.
- **Pacotes por módulo:** `com.oficina.<modulo>` com `controller`, `service`,
  `repository`, `model`. Sem camadas extras.
- **Segurança:**
  - Login por usuário e senha (BCrypt); sessão por JWT em cookie `HttpOnly`,
    `SameSite=Strict`, `Secure` em produção.
  - Bloqueio temporário por tentativas de login erradas.
  - Toda rota `/api/**` exige login, exceto `/api/auth/login`, `/api/saude`, a leitura
    pública de logo/fundo e o webhook (que tem token próprio).
  - Validação com Bean Validation; erros em JSON padronizado, sem stack trace ao cliente.
  - Upload só PNG ou JPG (**SVG recusado**), validando assinatura do arquivo e limite de
    tamanho.
  - Nenhum segredo no repositório. CORS por variável de ambiente (só para dev local).
- **Visual:**
  - Cores, fundo, bordas e sombras por **variáveis CSS** (tokens), com tema claro e
    escuro; a escolha fica salva no navegador.
  - Fundo customizável (cor ou imagem enviada), logo da oficina.
  - Layout limpo, muito espaço em branco, tipografia legível, estados de
    carregando/erro/vazio em toda lista, foco visível e contraste adequado.
- **Responsividade (obrigatória em toda tela):**
  - Teste e ajuste em **360px, 768px e 1280px**. Nenhuma tela com scroll horizontal.
  - Tag `viewport` correta; unidades relativas; nada de largura fixa em pixels.
  - **Menu lateral:** fixo no desktop; no celular vira gaveta aberta por um botão, com o
    mesmo rodapé (usuário e engrenagem).
  - **Tabelas viram cartões** no celular; formulários em uma coluna.
  - **Botões e áreas de toque com no mínimo 44px**, espaçados, sem ficar cortados nem
    sobrepostos. Botão de ação principal sempre visível.
  - Campos com o teclado certo (`tel`, `numeric`, `email`); fonte mínima de 16px nos
    campos, para o celular não dar zoom.
  - O calendário da Agenda troca para a visão de dia ou semana no celular.
  - Modais ocupam a tela inteira no celular, com botão de fechar acessível.
- **PDF sempre abre no navegador:** é servido com `Content-Disposition: inline` e aberto numa nova
  aba (`<a target="_blank" rel="noopener">`), **nunca como download direto**. A pessoa salva ou
  exporta pelo visualizador, se quiser. Proibido `attachment` e o atributo `download`. Na tela,
  use sempre o componente `LinkPdf`, que também avisa antes se faltar logo ou CNPJ.
- **PDF:** OpenPDF no backend. **Todo PDF começa com o mesmo cabeçalho:** logo da oficina,
  nome da empresa, endereço, CNPJ e telefone. Se faltar logo ou CNPJ, o PDF não é gerado
  e o sistema diz o que falta.
- **Testes unitários (obrigatórios em todo o sistema):**
  - Todo módulo entrega testes unitários do backend (JUnit: regras de negócio, cálculos,
    validações, serviços) e do frontend (Vitest: componentes e lógica de tela), cobrindo o
    caminho feliz e os erros principais.
  - **Ao final de cada módulo**, rode **apenas os testes do módulo desenvolvido** (por
    exemplo `mvn test -Dtest='com.oficina.clientes.*Test'` e `npx vitest run <pasta do
    módulo>`), **não a suíte inteira**, e mostre a saída. O módulo só é concluído com os
    testes dele verdes.
  - Dinheiro (`CalculoValores`), autorização, expiração do orçamento, webhook e
    anonimização LGPD são **obrigatoriamente testados**, com centavos.
  - Teste que falha não é apagado nem ignorado: corrija o código ou o teste.
- **Teste final de ponta a ponta (Playwright):** depois de **todos** os módulos concluídos
  e com os testes de cada um verdes, o Módulo 13 testa o sistema real no navegador.
- **Git:** um commit por módulo (ou por entrega fechada dentro dele), mensagem em
  português, sem `.env`, `node_modules`, `target`.

---

## 7. LGPD (vale para todos os módulos)

A oficina é a **controladora**; o sistema trata dados pessoais mínimos.

1. **Minimização:** cliente = nome e telefone; nada além disso é obrigatório. Não colete
   dado que nenhuma tela use.
2. **Direitos do titular:** a tela do cliente tem **Exportar dados** (JSON) e
   **Anonimizar** (substitui nome, telefone, e-mail, documento e endereço por valores
   neutros e mantém as OS e os valores para a contabilidade). Registre em `auditoria`.
3. **Segurança:** senha em BCrypt, cookie `HttpOnly`, HTTPS, acesso só por login, backups
   protegidos. Nunca registre dado pessoal em log.
4. **Terceiros (operadores):** Asaas (cobranças) e Google Charts (script externo, só
   recebe dados agregados, nunca dados pessoais). Liste-os na política de privacidade.
5. **Retenção:** orçamentos expiram sozinhos (Módulo 6). Documente o prazo de guarda de
   OS e financeiro em `docs/privacidade.md`.
6. **Documentos:** `docs/privacidade.md` (política de privacidade) e `docs/lgpd.md`
   (quem é o encarregado, bases legais, direitos do titular, incidentes). O Módulo 11
   valida tudo com as skills `lgpd-check` e `lgpd-md`, se disponíveis.

---

## 8. Memória do projeto (obrigatório em todo commit)

**Graphify (memória estruturada):**

- **A cada commit**, registre no Graphify o que foi estruturado: módulo, entidades,
  rotas, telas, decisões e o porquê. É a memória do projeto para futuras sessões.
- Se o conector Graphify não estiver disponível ou pedir autorização, **pare e peça ao
  humano**. Não siga sem registrar.

**Roteiro de módulos (`docs/ROTEIRO-MODULOS.md`, criado no Módulo 0):**

- Tabela com uma linha por módulo: **Módulo | Status | Data | O que foi feito | O que
  ficou pendente**. Status: `PENDENTE`, `EM ANDAMENTO` ou `CONCLUÍDO`.
- **Ao final de cada módulo**, atualize a linha dele (o que foi feito, o que ficou
  pendente, o que não foi testado) e **faça o commit junto com esse arquivo**.

---

## 9. Um dia real de serviço (o fluxo que o sistema precisa servir)

1. **Cliente chega** ou liga. O atendente busca pela placa, nome ou telefone. Se não
   achar, cria o que for necessário: cliente (nome e telefone), veículo (placa e modelo),
   ou nenhum dos dois.
2. **Agenda** quando o serviço é para outro dia: escolhe o dia no calendário e registra
   os serviços a executar. No dia, abre a OS direto do agendamento.
3. **Orçamento** (opcional): monta e envia o PDF ou o link de WhatsApp. Se o cliente
   aprovar no mesmo dia, converte em OS com um clique.
4. **OS** aberta com km, defeito relatado e mecânico responsável; adiciona serviços,
   mão de obra, peças (repasse) e desconto. Passa por **Aberta, Em execução, Pronta e
   Entregue**.
5. **Pagamento:** registra a forma (dinheiro, Pix, cartão) ou deixa em **fiado** com
   vencimento. Opcionalmente gera uma cobrança Asaas. Imprime o PDF da OS.
6. **Fim do dia:** vê o caixa do dia, o fiado em aberto e as OS paradas.
7. **Consulta:** o Histórico de OS traz qualquer OS antiga para ver ou reimprimir.

---

## 10. Módulos, execute em ordem

Cada módulo: **Entrega** e **Critério de aceite**. Não avance com teste vermelho nem com
o módulo quebrado no ambiente online. Ao concluir, siga a seção 8.

### Módulo 0 — Base do projeto e deploy
**Entrega:**
- **`.gitignore` primeiro** (node_modules, target, bin, .env, dist, uploads, arquivos de
  editor), antes de qualquer outro commit.
- Criar `backend/`: Spring Boot 3.2.x, Java 17, Maven, JPA, PostgreSQL, Flyway, Lombok,
  Bean Validation. `ddl-auto=validate`. Pacotes por módulo (`com.oficina.<modulo>`).
- Criar `frontend/`: React + Vite + react-router + `lucide-react`, com o CSS por tokens
  que o Módulo 2 vai completar.
- Segredos só por variáveis de ambiente; `.env.example`; CORS por variável (só dev).
- **Sem Docker.** `GET /api/saude`, `.java-version` e `.node-version`, e o passo a passo
  para o humano configurar o Dokploy em `docs/dokploy.md` (banco, backend, frontend,
  variáveis, domínio com caminho `/api`, volume de uploads, healthcheck).
- Configurar a base de testes: JUnit no backend e **Vitest** no frontend, cada um com
  um teste de exemplo passando.
- README real (o que é, como subir, como é o deploy).
- Criar `docs/ROTEIRO-MODULOS.md` com os módulos 0 a 13 como `PENDENTE`, e
  `docs/ROADMAP.md`. Registrar a estrutura inicial no Graphify.

**Aceite:** `git ls-files` sem nenhum `node_modules`, `target` ou `.env`; backend e
frontend sobem localmente (`./mvnw spring-boot:run` e `npm run dev`); um push publica a aplicação no Dokploy e
`/api/saude` responde `ok` no domínio; nenhum segredo no git; os testes de exemplo passam.

### Módulo 1 — Acesso e funcionários
**Entrega:** login e logout; troca de senha; cadastro de funcionários (nome, telefone,
CPF opcional, ativo, e login/senha opcionais); bloqueio por tentativas; funcionário
inicial criado no primeiro boot por variáveis de ambiente (nunca senha padrão fixa);
rotas protegidas no front e no back; funcionário com OS só pode ser inativado.
**Aceite:** sem login toda `/api/**` (exceto as públicas) devolve 401; não é possível
remover ou inativar o último funcionário com acesso; testes de login, bloqueio e troca de
senha; listagem paginada.

### Módulo 2 — Estrutura visual e Configurações
**Entrega:**
- Layout com o **menu de 4 grupos** que expandem e recolhem, o logo no topo e o **rodapé**
  (usuário, menu do usuário, engrenagem), exatamente como descrito na seção 4.
- Tokens CSS, **tema claro e escuro**, componentes-base (botão, campo, tabela paginada,
  cartão, modal, estado vazio) usados por todas as telas seguintes.
- **Configurações:** dados da empresa (nome, CNPJ, telefone, endereço), **upload do
  logo**, **fundo** (cor ou imagem), prazo do rascunho do orçamento.
- Páginas simples de **Ajuda** (manual, atalhos, sobre/versão).
**Aceite:** todas as rotas do menu abrem (as ainda não feitas mostram "Em breve" sem
emoji); tema persiste ao recarregar; logo e fundo aparecem; upload de SVG ou arquivo
grande é recusado; **sem scroll horizontal a 360px, 768px e 1280px**; menu em gaveta
funcional no celular; botões com no mínimo 44px; testes unitários dos componentes-base
passando.

### Módulo 3 — Clientes e Veículos
**Entrega:** cadastros separados, com **busca única** (placa, nome ou telefone) e
paginação; cliente só com nome e telefone obrigatórios; veículo com cliente opcional;
ficha do veículo com o histórico de OS (lista paginada); **Exportar** e **Anonimizar**
cliente (seção 7).
**Botão WhatsApp** ao lado do telefone na ficha do cliente: link `wa.me` que abre o WhatsApp
Web (computador) ou o app (celular) na conversa dele, sem texto pronto e sem API. Some se o
telefone não for válido ou se o cliente foi anonimizado.
**Aceite:** placa duplicada é recusada (formato antigo e Mercosul); busca por telefone
ignora a máscara; anonimizar mantém as OS e remove os dados pessoais; testes da busca e
da anonimização.

### Módulo 4 — Serviços
**Entrega:** catálogo de serviços (nome, preço padrão), com ativar e inativar em vez de
excluir; busca e paginação.
**Aceite:** serviço inativo não aparece ao montar OS, orçamento ou agendamento, mas
continua nos já criados.

### Módulo 5 — Ordens de Serviço (núcleo do produto)
**Entrega:**
- Estados: `ABERTA → EM_EXECUCAO → PRONTA → ENTREGUE`, e `CANCELADA`. Transições
  validadas no backend; sem pular etapas.
- Campos: cliente (opcional), veículo (opcional), km de entrada, defeito relatado,
  observações, mecânico responsável, previsão de entrega.
- Itens de serviço (do catálogo ou avulsos) e o **bloco de valores** da seção 5 (mão de
  obra, peças com descrição, taxa de compra, desconto), com o total calculado ao vivo na
  tela e **recalculado no backend**.
- Lista paginada com filtros (status, período, texto) e tela única de edição.
- **PDF da OS** com o cabeçalho padrão, três blocos (serviços, mão de obra, peças e
  taxa) e o total.
- Auditoria de quem mudou o status e quando.
**Aceite:** `CalculoValores` testado com centavos e com os dois tipos (valor e %) de taxa
e desconto; desconto acima do permitido é recusado; OS `ENTREGUE` fica imutável (reabrir
exige confirmação e gera registro); OS sem cliente e sem veículo é salva; o PDF sai com o
cabeçalho.

### Módulo 6 — Orçamentos
**Entrega:**
- Menu próprio. Orçamento com cliente cadastrado **ou** nome e telefone avulsos, veículo
  ou placa e modelo avulsos, e o mesmo bloco de valores (usando `CalculoValores`).
- **Gerar PDF** ou **link de WhatsApp** marca `gerado_em`. A partir daí ele fica em
  **rascunho** pelo prazo `orcamento_rascunho_horas` (padrão 24 h) e pode ser reaberto e
  corrigido.
- **Criar OS a partir deste orçamento:** só enquanto existe. Copia cliente, veículo e
  itens para uma OS nova, oferece **cadastro rápido de cliente** (nome e telefone) se
  não houver cliente (pode ser pulado), e **apaga o orçamento** na mesma transação.
- **Limpeza automática:** rotina agendada (a cada hora) apaga os orçamentos cujo prazo
  expirou, e orçamentos sem `gerado_em` há mais de 7 dias.
**Aceite:** teste da expiração (orçamento vencido some, dentro do prazo permanece); a
conversão cria a OS com os mesmos valores e remove o orçamento; falha na criação da OS
não apaga o orçamento (transação).

### Módulo 7 — Agenda
**Entrega:** **calendário grande** (mês, semana e dia). Clicar num dia abre o
agendamento: data, hora opcional, cliente cadastrado ou avulso, veículo ou placa avulsa,
serviços a executar (catálogo ou texto), funcionário opcional e observações. Status
`AGENDADO`, `REALIZADO`, `CANCELADO` e `NAO_COMPARECEU`. Botão **Abrir OS a partir do
agendamento**. Consulta posterior por busca e filtro de período, paginada.
**Aceite:** criar, editar, cancelar e consultar agendamentos de meses diferentes;
abrir a OS copia serviços, cliente e veículo; o calendário carrega só o período visível.

### Módulo 8 — Financeiro e Asaas
**Entrega:**
- **Pagamento da OS** (um ou mais, parcial), com forma (dinheiro, Pix, cartão); **fiado**
  como `receber` com vencimento; tela de contas a receber; **caixa do dia** (entradas por
  forma); **despesas** simples (descrição, valor, data, categoria). Tudo paginado.
- A OS só vira `ENTREGUE` com saldo zerado **ou** fiado explicitamente registrado.
- Interface **`GatewayPagamento`** (`criarCobranca`, `consultar`) com a implementação
  **Asaas** (API v3, chave em variável de ambiente, sandbox primeiro). **Confirme os
  campos na documentação oficial do Asaas antes de implementar.**
- Botão **Gerar cobrança** (Pix, boleto ou cartão) na OS: só aparece se a chave do Asaas
  estiver configurada. O Asaas exige nome e CPF/CNPJ do cliente: se faltar, peça na hora.
- **Webhook** `POST /api/webhooks/asaas`: valida o token (variável de ambiente), é
  **idempotente** pelo `id_externo` e dá baixa automática no pagamento.
**Aceite:** soma do caixa do dia bate com os pagamentos do dia; webhook com token errado
devolve 401; o mesmo evento duas vezes registra um pagamento só; sem a chave do Asaas o
sistema funciona e o botão não aparece.

### Módulo 9 — Relatórios (Google Charts + PDF)
**Entrega:** todo relatório tem **filtro de período**, **gráfico** (Google Charts, script
carregado de `gstatic.com`), **tabela paginada** e botão **PDF**.
1. **Financeiro por período:** receita da oficina separada do repasse de peças.
2. **Por funcionário:** OS e receita por mecânico responsável.
3. **Por serviço executado:** quantidade e valor, só linhas de serviço.
4. **Formas de pagamento.**
5. **Contas a receber (fiado).**
6. **Despesas por categoria.**
7. **OS por status.**
8. **Histórico de OS:** consulta por placa, nome, telefone ou período; abre a OS em
   leitura e gera o PDF dela. Sem gráfico.

**PDF:** gerado no backend com o cabeçalho padrão. O navegador envia o gráfico como PNG
(`getImageURI()`) junto com os filtros; o backend **valida tipo (assinatura PNG) e
tamanho (≤ 1 MB)** antes de embutir. Sem internet o gráfico não carrega: a tela avisa e
a tabela e o PDF continuam funcionando.
**Aceite:** os totais dos relatórios batem com o financeiro e com as OS; período grande
(milhares de OS) responde em menos de 2 s; PDF de cada relatório sai com o cabeçalho.

### Módulo 10 — Dashboard "Hoje"
**Entrega:** substituir os números fixos por dados reais: OS por status, entregas
previstas para hoje e atrasadas, agendamentos de hoje, receita do dia e do mês, fiado em
aberto, OS paradas há mais de N dias. Cada cartão leva à lista filtrada.
**Aceite:** nenhum número fixo no código; carrega em menos de 1 s com 5.000 OS (índices
criados); teste de cada cartão contra dados de exemplo.

### Módulo 11 — LGPD, backup e fechamento
**Entrega:**
- `docs/privacidade.md` e `docs/lgpd.md` (seção 7); rodar `lgpd-check` e `lgpd-md` e
  corrigir o que apontarem.
- **Backup diário do banco** (backup nativo do Dokploy ou `pg_dump` com rotação de 14
  dias e cópia fora da VPS) e **restauração testada** em outro ambiente.
- `docs/instalacao.md`: como subir uma VPS nova com Dokploy, passo a passo.
- Manual do usuário (1 página) em `docs/`, versão semântica e `CHANGELOG.md`.
- Revisão final: nenhum emoji, nenhuma lista sem paginação, nenhuma senha padrão.
**Aceite:** restaurar um backup devolve todos os dados; `lgpd-check` sem pendência
crítica; uma VPS nova fica no ar seguindo só `docs/instalacao.md`.

### Módulo 12 — Catálogo de modelos de veículos (brazilian-cars)
Só depois dos módulos de negócio. Troca o texto livre de marca, modelo e ano do veículo por
**autocomplete vindo do próprio banco**, sem chamar nenhuma API externa.
**Entrega:**
- Tabela `catalogo_veiculo` (marca, modelo, versão como "1992 Gasolina"), com índice único
  em (marca, modelo, versão) e índices de busca. **Nenhum dado pessoal** nela.
- **Importação única e idempotente** a partir de `gpupo/brazilian-cars`
  (`Resources/data/current/cars.yaml`, licença **MIT**): o agente baixa o arquivo uma vez,
  filtra o tipo `Car` e converte para um CSV compacto (`marca;modelo;versao`) em
  `backend/src/main/resources/catalogo/`. Na subida, se a tabela estiver vazia, o backend
  carrega o CSV em lote. Rodar duas vezes não duplica. Nada é baixado em tempo de execução.
- **Licença:** manter o aviso de copyright e a licença MIT do repositório de origem em
  `docs/licencas.md` e junto do CSV.
- Rotas autenticadas, paginadas e com limite (20): marcas, modelos de uma marca com filtro
  `q`, e versões de um modelo.
- **Tela de veículo:** marca, modelo e ano viram campos com sugestões do catálogo, em
  cadeia (marca, depois modelo, depois versão), **sempre aceitando texto livre**. Ao salvar
  um veículo com marca e modelo que não estão no catálogo, ele entra no catálogo, e assim
  o catálogo cresce com o que a oficina cadastra (carros novos que a fonte não tem).
**Aceite:** importação roda duas vezes sem duplicar; sugestão responde em menos de 200 ms;
veículo com modelo novo digitado passa a aparecer nas sugestões; testes unitários e de
integração do módulo passando; `docs/licencas.md` existe.

### Módulo 13 — Teste final com Playwright
Só começa com os Módulos 0 a 12 `CONCLUÍDO`, cada um com os testes dele verdes.
**Entrega:** testes de ponta a ponta no **Playwright já instalado** no ambiente. Não
reinstale o Playwright nem o navegador. Rode contra a aplicação **online no Dokploy**
(ou, se não houver acesso, local com backend e frontend rodando), com dados de exemplo.
- **Fluxos:** login e troca de senha; cadastro de cliente e veículo; OS completa (criar,
  itens, mão de obra, peças, desconto, Em execução, Pronta, Entregue, pagamento, PDF);
  fiado e contas a receber; orçamento (gerar, criar OS a partir dele, expiração
  verificada por tempo configurado curto); agendamento no calendário e abrir OS a partir
  dele; cada relatório com gráfico e PDF; anonimizar cliente; alternar tema; Configurações
  (logo e fundo).
- **Responsividade:** repita os fluxos principais em **360x640, 768x1024 e 1280x800**.
  Verifique: sem scroll horizontal, botões visíveis e clicáveis, menu em gaveta no
  celular, tabelas em cartões.
- **Falhas esperadas:** acesso sem login, upload inválido, webhook com token errado,
  desconto acima do permitido.
- Guarde capturas de tela em `docs/testes-e2e/` e o relatório do Playwright.
- Todo defeito encontrado é corrigido no módulo de origem, com teste unitário novo, e o
  Playwright roda de novo até passar.
**Aceite:** suíte Playwright 100% verde nos três tamanhos de tela; nenhum erro no console
do navegador nos fluxos; relatório e capturas salvos; `docs/ROTEIRO-MODULOS.md` com todos
os módulos `CONCLUÍDO`.

---

## 11. Regras de execução para você (agente)

1. Antes de cada módulo, descreva em 5 linhas o que vai fazer e quais arquivos toca.
2. Escreva o teste antes da regra de negócio nos Módulos 5, 6 e 8.
3. Não adicione funcionalidade fora desta lista; ideias novas vão para `docs/ROADMAP.md`.
4. **Ao terminar cada módulo:** rode **só os testes unitários daquele módulo** (nunca a
   suíte inteira) e mostre a saída;
   confirme a responsividade das telas novas em 360px, 768px e 1280px; confirme que o
   ambiente online sobe; atualize o README; **registre no Graphify**; **atualize
   `docs/ROTEIRO-MODULOS.md`** (feito e pendente); faça o commit. Se algo não foi
   testado ou foi pulado, **diga claramente**.
5. Se um critério de aceite não puder ser cumprido, pare e explique; não adapte o
   critério.
6. Dúvida de produto: escolha a opção mais simples para a oficina, registre a decisão em
   `docs/DECISOES.md` e siga em frente.
7. Antes de push, lembre: **o push publica no ar**. Nada quebrado.
8. **Nenhuma API externa de FIPE e nenhuma busca de CEP** neste trabalho. O catálogo de
   veículos é o Módulo 12 (arquivo local); o CEP não tem módulo. Não prepare ganchos antes da hora.
