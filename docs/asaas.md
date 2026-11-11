# Cobrança online com o Asaas

A cobrança online é **opcional**. Sem a chave do Asaas o sistema funciona igual (pagamento manual, fiado, caixa) e o
botão "Gerar cobrança" não aparece. A integração fica atrás da interface `GatewayPagamento`, para a maquininha
(InfinitePay, PagBank ou Mercado Pago) entrar depois sem mexer na OS nem no caixa.

## Como funciona

1. Na OS, o atendente escolhe **Gerar cobrança** (Pix, boleto, cartão ou "cliente escolhe"). O sistema cria a cobrança no
   Asaas e mostra o **link de pagamento**, que o atendente copia e envia como quiser.
2. A cobrança fica **pendente** e reserva parte do saldo da OS (para não cobrar duas vezes o mesmo valor).
3. Quando o cliente paga, o Asaas chama o **webhook** `POST /api/webhooks/asaas`. O sistema registra o pagamento da OS
   (origem Asaas), que entra no caixa do dia e no saldo da OS.
4. O dinheiro **não** entra no caixa só por gerar a cobrança.

## Configuração (variáveis de ambiente, nunca no git)

| Variável | Para quê |
|---|---|
| `ASAAS_API_KEY` | Chave de API. Sem ela, a cobrança online fica desligada. |
| `ASAAS_URL` | Padrão: `https://api-sandbox.asaas.com/v3` (**sandbox**). Produção: `https://api.asaas.com/v3`. |
| `ASAAS_WEBHOOK_TOKEN` | Token que o Asaas envia no cabeçalho `asaas-access-token`. Sem ele, o webhook recusa tudo (401). |

- Chave de sandbox começa com `$aact_hmlg_`; a de produção, com `$aact_prod_`. Misturar ambientes dá 401 (o sistema avisa no log).
- A chave contém `$`: no painel do Dokploy, coloque o valor **entre aspas simples** se o campo interpretar `$`.
- O token do webhook é um segredo **diferente** da chave de API (a documentação do Asaas proíbe usar a chave como token).

### Passo a passo (sandbox primeiro)

1. Crie uma conta no **Sandbox** do Asaas e uma chave de API nesse ambiente.
2. No Dokploy, no backend: `ASAAS_API_KEY`, `ASAAS_WEBHOOK_TOKEN` (uma senha longa e aleatória) e, se quiser, `ASAAS_URL`.
3. No painel do Asaas (Integrações, Webhooks), crie um webhook:
   - URL: `https://<domínio-da-oficina>/api/webhooks/asaas`
   - Token de autenticação: o mesmo `ASAAS_WEBHOOK_TOKEN`
   - Eventos: **PAYMENT_CONFIRMED**, **PAYMENT_RECEIVED** e **PAYMENT_REFUNDED** (os outros são ignorados).
   - Fila **sem** interrupção habilitada (o webhook responde 200 a tudo que não é erro de verdade).
4. Gere uma cobrança numa OS de teste e pague pelo sandbox (Pix de teste do Asaas). A OS deve mostrar o pagamento com origem "Asaas".
5. Só depois, troque para produção: `ASAAS_URL=https://api.asaas.com/v3`, a chave `$aact_prod_...` e um webhook novo em produção.

## O que foi conferido na documentação oficial (docs.asaas.com, em 2026-11-11)

Não foi possível testar contra o Asaas de verdade nesta construção (faltava uma chave de sandbox). Os campos abaixo foram
lidos da documentação e estão cobertos por testes com respostas simuladas (`AsaasGatewayTest`). **Antes de vender, rodar o passo 4 acima.**

| Item | Valor |
|---|---|
| URL base | Sandbox `https://api-sandbox.asaas.com/v3`, produção `https://api.asaas.com/v3` |
| Autenticação | cabeçalho `access_token`; `User-Agent` obrigatório para contas criadas desde 13/06/2024; `Content-Type: application/json` |
| Criar cliente | `POST /customers`, obrigatórios `name` e `cpfCnpj` (por isso a cobrança pede o CPF/CNPJ do cliente) |
| Criar cobrança | `POST /payments`, obrigatórios `customer`, `billingType` (`PIX`, `BOLETO`, `CREDIT_CARD`, `UNDEFINED` = o cliente escolhe), `value`, `dueDate`; usamos também `description` e `externalReference` |
| Resposta da cobrança | `id` (`pay_...`), `invoiceUrl` (link de pagamento), `status` |
| Cancelar cobrança | `DELETE /payments/{id}` (**não é estorno**: não devolve dinheiro já pago) |
| Consultar | `GET /payments/{id}` |
| Webhook | `POST` com `{id, event, payment:{id,...}}`; token no cabeçalho `asaas-access-token`; entrega "ao menos uma vez" (pode repetir); a fila pausa após 15 falhas seguidas |

## Regras do sistema

- Só gera cobrança para OS com **cliente cadastrado**, com **CPF ou CNPJ** (informado na hora e guardado no cliente se faltar).
- Valor padrão = saldo ainda não pago, fiado nem cobrado; nunca acima disso. Vencimento padrão = hoje + 3 dias.
- O cliente é criado no Asaas na primeira cobrança e reaproveitado nas seguintes.
- **Idempotência:** cada cobrança vira no máximo um pagamento (índice único no id do Asaas). Reenvio do evento, ou `CONFIRMED` seguido de `RECEIVED` (cartão), não duplica.
- Só eventos de cobranças criadas por este sistema são considerados; o resto responde 200 e é ignorado.
- O valor registrado é o que o sistema cobrou (juros e multa de boleto vencido não entram).
- A forma do pagamento vem do tipo realmente pago (`billingType` do evento): Pix, boleto, cartão (crédito ou débito); transferência e depósito contam como Pix.
- `PAYMENT_REFUNDED` marca o pagamento como estornado e a cobrança como estornada. Pagamento do Asaas **não** se estorna à mão aqui.
- Não cancela a OS com cobrança pendente: cancele a cobrança antes.

## Fora do escopo (ver `docs/PENDENCIAS.md`)

Estorno parcial, chargeback, cobrança parcelada, assinaturas, split, nota fiscal e conciliação de taxas do Asaas.
