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
- [x] Ficha do veículo com histórico de OS: feito no Módulo 5 (últimas 20 OS, com link).
- [ ] Cadastro rápido de cliente e veículo dentro da tela da OS: não existe. Hoje, na OS, cliente e veículo precisam estar cadastrados antes (a OS aceita ficar sem eles). No orçamento, o cadastro na hora existe, ao criar a OS (Módulo 6).
- [ ] Busca de cliente e de veículo usa `like '%texto%'` sem índice próprio (suficiente para milhares de registros); avaliar `pg_trgm` se ficar lenta.
- [ ] Endereço do cliente é um único campo de texto; separar em CEP, cidade e UF só quando o CEP for integrado.
- [ ] Não existe exclusão de cliente, só anonimização (LGPD, mantém a contabilidade). Conferir com o dono se precisa de exclusão física.
- [ ] Anonimizar não cobre dados pessoais digitados em campos livres de OS futuras (observações): tratar no Módulo 5.

## Módulo 4 — Serviços
- [ ] A regra "serviço inativo não aparece ao montar OS, orçamento ou agenda" está pronta na API (`ativos=true`), mas só vale de fato quando esses módulos (5, 6 e 7) usarem o filtro e guardarem nome e preço na própria linha (para o histórico não mudar quando o preço padrão mudar).
- [ ] Não há importação de serviços por planilha (estava no prompt antigo, cortada por simplicidade).
- [ ] Preço padrão sem histórico de alteração: mudar o preço não é auditado.

## Módulo 5 — Ordens de Serviço
- [x] A regra "OS só vira ENTREGUE com saldo zerado ou fiado registrado": feita no Módulo 8 (parte A).
- [ ] A prévia dos totais na tela repete a fórmula do backend (`calculo.js` e `CalculoValores`), com os mesmos casos de teste nos dois lados. Se a fórmula mudar, mudar nos dois e rodar os dois testes.
- [ ] Texto livre da OS (defeito relatado, observações, descrições) pode conter dado pessoal e a anonimização do cliente não o limpa. Avisar o atendente na tela ou limpar na anonimização.
- [ ] Fotos e checklist de entrada: cortados de propósito (só o atendente usa o sistema, pelo computador). Está no ROADMAP.
- [ ] Cancelamento sem motivo registrado; reabertura de OS entregue sem perfil especial (existe um só nível de acesso).
- [ ] O número da OS é o id do banco: pode haver lacunas se um cadastro falhar no meio.
- [ ] A lista de mecânicos da OS carrega até 100 funcionários.
- [ ] Busca de OS por texto usa `like '%texto%'` sem índice próprio (bom para milhares de OS); revisar se o relatório de histórico ficar lento.
- [ ] PDF testado por extração de texto, por conversão em imagem (pdftoppm) e no Chromium; não foi aberto em outros leitores. Fonte Helvetica padrão (sem caracteres fora do Latin-1).
- [x] Orçamento sem WhatsApp: confirmado pelo dono (só PDF, que ele salva e compartilha). O botão do WhatsApp na ficha do cliente existe e foi pedido.

## Módulo 6 — Orçamentos
- [ ] A limpeza agendada só roda com a aplicação ligada (a cada hora, 1 minuto depois de subir). Um orçamento vencido continua listado e utilizável até a próxima rodada (no máximo ~1 h de atraso).
- [ ] O prazo vale a partir da primeira abertura do PDF e muda se o prazo nas Configurações mudar (é calculado na limpeza com o valor atual).
- [ ] Abrir o PDF é um `GET` que grava `gerado_em` na primeira vez. Aceitável aqui (um só usuário); um navegador que "pré-carregue" o link poderia começar o prazo sem querer.
- [ ] Orçamento avulso guarda nome e telefone de quem não é cliente até ser apagado (no máximo o prazo, ou 7 dias se nunca teve PDF); entra na política de privacidade (Módulo 11).
- [ ] A tela de orçamento atualiza o prazo ao voltar da aba do PDF (evento de foco do navegador); se o navegador não disparar o evento, basta recarregar a página.
- [ ] Não há número de validade impresso no PDF do orçamento nem texto legal (condições, garantia); definir com o dono antes de vender.

## Módulo 7 — Agenda
- [ ] Decisão minha, reversível: abrir a OS marca o agendamento como Realizado automaticamente (o cliente apareceu). Se o dono preferir marcar à mão, é só tirar essa linha em `AgendamentoService.criarOs`.
- [ ] Sem aviso de conflito de horário, sem duração, sem recorrência e sem lembrete ao cliente (fora de escopo, está no ROADMAP). Nada impede agendar no passado.
- [ ] Não existe exclusão de agendamento, só cancelar (a consulta posterior depende do histórico). Confirmar com o dono.
- [ ] Nome e telefone de clientes avulsos ficam guardados nos agendamentos para sempre (a consulta posterior os usa) e a anonimização de clientes cadastrados não os alcança. Definir prazo de guarda ou uma ferramenta de limpeza na política de privacidade (Módulo 11).
- [ ] A grade do mês mostra até 3 agendamentos por dia e esconde o resto em "+N mais" (leva à visão do dia). Vale conferir com o dono se 3 basta.
- [ ] A semana começa no domingo (fixo). Data e hora são locais, sem fuso: servidor e navegador precisam estar no mesmo fuso (o caso de uma oficina por VPS).
- [ ] No tablet em pé (701 a 900 px) o mês aparece compacto, com nomes cortados; o celular (até 700 px) usa dia ou semana.
- [ ] Os testes do calendário no frontend congelam a data (relógio falso); se o fuso da máquina de testes mudar, conferir.

## Módulo 8 — Financeiro (parte A feita; parte B, Asaas, pendente)
- [ ] Parte B inteira: `GatewayPagamento`, implementação do Asaas, botão "Gerar cobrança" (só com chave configurada) e webhook com token e idempotência. Precisa de uma chave de sandbox do Asaas para testar de verdade; sem ela, só testes com respostas simuladas.
- [ ] Editar o valor da OS depois de receber é permitido (a OS mostra o saldo negativo ou o que falta); a entrega fica bloqueada até acertar. Não há bloqueio de edição de itens quando há pagamento.
- [ ] Estorno é do registro do sistema: devolver o dinheiro ao cliente (Pix, cartão) é feito fora e não é conferido aqui.
- [ ] Fiado não tem juros, multa nem parcelamento: um fiado é um valor com um vencimento. Parcelas = vários fiados.
- [ ] A data do pagamento não pode ser futura, mas pode ser qualquer dia passado (lançar o que entrou ontem). O caixa é por dia do recebimento, não por hora; não há "fechamento de caixa" que impeça mexer em dias antigos.
- [ ] Despesas não têm anexo, fornecedor nem recorrência; categorias são texto livre (sugeridas pelas já usadas).
- [ ] Não há relatório financeiro ainda (Módulo 9) nem cartões no Dashboard (Módulo 10).
- [ ] Taxa de maquininha/cartão não é descontada: o caixa mostra o valor cobrado, não o líquido.
- [ ] No tablet e no celular, o resumo do caixa quebra em duas colunas; a lista de recebimentos do dia não mostra a hora (só o dia).
- [ ] Se o volume de uploads não estiver montado, o sistema trata como "sem logo" (e o PDF avisa o que falta); no Dokploy, conferir o volume `/data/uploads` (Módulo 0).

## Módulo 12 — Catálogo de modelos de veículos (planejado)
- [ ] Fonte: `gpupo/brazilian-cars` (`Resources/data/current/cars.yaml`, ~5,7 mil linhas de modelos; marca, modelo e versões como "1992 Gasolina"; licença MIT, manter o aviso). O repositório foi atualizado pela última vez em 2024-05, então carros mais novos não estão nele: por isso a tela aceita texto livre e o catálogo aprende com o que a oficina cadastra.
- [ ] Os dados vêm originalmente da tabela FIPE. A licença MIT é do repositório, não garante o direito sobre a origem: confirmar o uso comercial antes de vender.
- [ ] A API apifipe.com.br foi descartada (conta sem assinatura, resposta 402); a chave dela nunca entrou no repositório.
