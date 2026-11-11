package com.oficina.financeiro;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.oficina.financeiro.gateway.GatewayException;
import com.oficina.financeiro.gateway.GatewayPagamento;
import com.oficina.funcionarios.FuncionarioForm;
import com.oficina.funcionarios.FuncionarioService;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Gateway de mentira no lugar do Asaas: o que se confere aqui é o nosso lado (regras, webhook, idempotência). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.asaas.webhook-token=token-de-teste")
@Import(CobrancaIntegracaoTest.GatewayDeMentira.class)
class CobrancaIntegracaoTest {
    static class FakeGateway implements GatewayPagamento {
        final List<NovaCobranca> pedidos = new CopyOnWriteArrayList<>();
        final List<String> cancelados = new CopyOnWriteArrayList<>();
        volatile GatewayException falha;
        int clientesCriados;
        int contador;

        void zerar() { pedidos.clear(); cancelados.clear(); falha = null; }

        @Override public boolean ativo() { return true; }

        @Override
        public CobrancaCriada criarCobranca(NovaCobranca n) {
            if (falha != null) throw falha;
            pedidos.add(n);
            String cliente = n.clienteIdExterno() != null ? n.clienteIdExterno() : "cus_fake_" + (++clientesCriados);
            return new CobrancaCriada("pay_fake_" + (++contador), cliente, "https://sandbox.asaas.com/i/fake" + contador, "PENDING");
        }

        @Override public void cancelarCobranca(String id) { if (falha != null) throw falha; cancelados.add(id); }

        @Override public ConsultaCobranca consultar(String id) { return new ConsultaCobranca(id, "PENDING", null, null, null); }
    }

    @TestConfiguration
    static class GatewayDeMentira {
        @Bean @Primary FakeGateway gatewayDeMentira() { return new FakeGateway(); }
    }

    @Autowired MockMvc mvc;
    @Autowired FuncionarioService funcionarios;
    @Autowired JdbcTemplate jdbc;
    @Autowired FakeGateway fake;
    Cookie cookie;
    final String hoje = LocalDate.now().toString();
    final String ontem = LocalDate.now().minusDays(1).toString();
    final String amanha = LocalDate.now().plusDays(1).toString();

    @BeforeEach
    void entrar() throws Exception {
        limpar();
        fake.zerar();
        funcionarios.criar(new FuncionarioForm("Ana", null, null, null, "ana", "senha-boa-123"));
        cookie = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"ana\",\"senha\":\"senha-boa-123\"}")).andReturn().getResponse().getCookie("oficina_token");
    }

    @AfterEach
    void limpar() {
        for (String t : new String[]{"cobranca", "receber", "pagamento", "despesa", "os_item", "ordem_servico", "auditoria", "veiculo", "cliente", "servico", "funcionario"}) {
            jdbc.execute("delete from " + t);
        }
    }

    private long id(String json) { return Long.parseLong(json.replaceAll("^\\{\"id\":(\\d+).*", "$1")); }

    private ResultActions json(MockHttpServletRequestBuilder b, String corpo) throws Exception {
        return mvc.perform(b.cookie(cookie).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private long cliente(String nome, String documento) throws Exception {
        return id(json(post("/api/clientes"), "{\"nome\":\"" + nome + "\",\"telefone\":\"11988887777\"" + (documento == null ? "" : ",\"documento\":\"" + documento + "\"") + "}")
                .andReturn().getResponse().getContentAsString());
    }

    private long os(Long cliente, String preco) throws Exception {
        return id(json(post("/api/ordens"), "{" + (cliente == null ? "" : "\"clienteId\":" + cliente + ",") + "\"itens\":[{\"descricao\":\"S\",\"quantidade\":1,\"precoUnit\":" + preco + "}]}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private ResultActions cobrar(long os, String corpo) throws Exception { return json(post("/api/ordens/" + os + "/cobrancas"), corpo); }

    private String asaasIdDaCobranca(long os) {
        return jdbc.queryForObject("select asaas_id from cobranca where os_id = ? order by id desc limit 1", String.class, os);
    }

    private ResultActions webhook(String token, String corpo) throws Exception {
        var b = post("/api/webhooks/asaas").contentType(MediaType.APPLICATION_JSON).content(corpo);
        return mvc.perform(token == null ? b : b.header("asaas-access-token", token)); // sem cookie: o Asaas não tem login
    }

    private String evento(String evento, String payId, String billing, String data) {
        return "{\"id\":\"evt_" + System.nanoTime() + "\",\"event\":\"" + evento + "\",\"payment\":{\"object\":\"payment\",\"id\":\"" + payId + "\""
                + (billing == null ? "" : ",\"billingType\":\"" + billing + "\"") + (data == null ? "" : ",\"paymentDate\":\"" + data + "\"") + "}}";
    }

    private int contar(String tabela) { return jdbc.queryForObject("select count(*) from " + tabela, Integer.class); }

    // ---------- criar cobrança ----------

    @Test
    void exigeLoginParaCobrarECancelar() throws Exception {
        mvc.perform(get("/api/ordens/1/cobrancas")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/ordens/1/cobrancas").contentType(MediaType.APPLICATION_JSON).content("{\"tipo\":\"PIX\"}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/cobrancas/1/cancelar")).andExpect(status().isUnauthorized());
    }

    @Test
    void geraCobrancaPixDoSaldoTodoComLinkEReservaOSaldo() throws Exception {
        long os = os(cliente("Maria Souza", "123.456.789-09"), "100");
        mvc.perform(get("/api/ordens/" + os + "/cobrancas").cookie(cookie)).andExpect(jsonPath("$.online").value(true))
                .andExpect(jsonPath("$.temCliente").value(true)).andExpect(jsonPath("$.clienteTemDocumento").value(true)).andExpect(jsonPath("$.disponivel").value(100.0));
        cobrar(os, "{\"tipo\":\"PIX\"}").andExpect(status().isOk()).andExpect(jsonPath("$.cobrancas[0].tipo").value("PIX"))
                .andExpect(jsonPath("$.cobrancas[0].valor").value(100.0)).andExpect(jsonPath("$.cobrancas[0].status").value("PENDENTE"))
                .andExpect(jsonPath("$.cobrancas[0].link").value(org.hamcrest.Matchers.startsWith("https://sandbox.asaas.com/i/")))
                .andExpect(jsonPath("$.cobrancas[0].vencimento").value(LocalDate.now().plusDays(3).toString())).andExpect(jsonPath("$.disponivel").value(0.0));
        var pedido = fake.pedidos.get(0);
        assertEquals("Maria Souza", pedido.clienteNome());
        assertEquals("12345678909", pedido.clienteDocumento()); // só dígitos
        assertEquals("os-" + os, pedido.referencia());
        // o dinheiro NÃO entra no caixa só por gerar a cobrança
        mvc.perform(get("/api/ordens/" + os + "/financeiro").cookie(cookie)).andExpect(jsonPath("$.pago").value(0.0));
        cobrar(os, "{\"tipo\":\"PIX\"}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.erro").value(org.hamcrest.Matchers.containsString("Não há saldo a cobrar")));
    }

    @Test
    void cobrancaParcialAcimaDoDisponivelEVencimentoPassadoSaoRecusados() throws Exception {
        long os = os(cliente("Maria", "12345678909"), "100");
        cobrar(os, "{\"tipo\":\"PIX\",\"valor\":100.01}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.erro").value(org.hamcrest.Matchers.containsString("saldo disponível")));
        cobrar(os, "{\"tipo\":\"PIX\",\"vencimento\":\"" + ontem + "\"}").andExpect(status().isBadRequest());
        cobrar(os, "{\"tipo\":\"PIX\",\"valor\":0}").andExpect(status().isBadRequest());
        cobrar(os, "{\"tipo\":\"PIX\",\"valor\":10.123}").andExpect(status().isBadRequest());
        cobrar(os, "{\"tipo\":\"BOLETINHO\"}").andExpect(status().isBadRequest());
        cobrar(os, "{}").andExpect(status().isBadRequest());
        cobrar(os, "{\"tipo\":\"BOLETO\",\"valor\":40,\"vencimento\":\"" + amanha + "\"}").andExpect(status().isOk()).andExpect(jsonPath("$.disponivel").value(60.0));
        cobrar(os, "{\"tipo\":\"CREDIT_CARD\",\"valor\":60.01}").andExpect(status().isBadRequest());
        cobrar(os, "{\"tipo\":\"UNDEFINED\",\"valor\":60}").andExpect(status().isOk()).andExpect(jsonPath("$.disponivel").value(0.0));
        assertEquals(2, fake.pedidos.size());
    }

    @Test
    void pagamentoEFiadoJaFeitosReduzemOQuePodeSerCobrado() throws Exception {
        long os = os(cliente("Maria", "12345678909"), "100");
        json(post("/api/ordens/" + os + "/pagamentos"), "{\"valor\":30,\"forma\":\"DINHEIRO\"}").andExpect(status().isOk());
        json(post("/api/ordens/" + os + "/fiado"), "{\"valor\":20,\"vencimento\":\"" + amanha + "\"}").andExpect(status().isOk());
        mvc.perform(get("/api/ordens/" + os + "/cobrancas").cookie(cookie)).andExpect(jsonPath("$.disponivel").value(50.0));
        cobrar(os, "{\"tipo\":\"PIX\",\"valor\":50.01}").andExpect(status().isBadRequest());
        cobrar(os, "{\"tipo\":\"PIX\"}").andExpect(status().isOk()).andExpect(jsonPath("$.cobrancas[0].valor").value(50.0));
    }

    @Test
    void osSemClienteOuCanceladaOuSemValorNaoGeraCobranca() throws Exception {
        long semCliente = os(null, "100");
        cobrar(semCliente, "{\"tipo\":\"PIX\"}").andExpect(status().isConflict()).andExpect(jsonPath("$.erro").value(org.hamcrest.Matchers.containsString("escolha um cliente")));
        mvc.perform(get("/api/ordens/" + semCliente + "/cobrancas").cookie(cookie)).andExpect(jsonPath("$.temCliente").value(false));
        long cancelada = os(cliente("Ana", "12345678909"), "100");
        json(patch("/api/ordens/" + cancelada + "/status"), "{\"status\":\"CANCELADA\"}").andExpect(status().isOk());
        cobrar(cancelada, "{\"tipo\":\"PIX\"}").andExpect(status().isConflict());
        long zerada = id(json(post("/api/ordens"), "{\"clienteId\":" + cliente("Zero", "12345678909") + "}").andReturn().getResponse().getContentAsString());
        cobrar(zerada, "{\"tipo\":\"PIX\"}").andExpect(status().isBadRequest());
        cobrar(999999, "{\"tipo\":\"PIX\"}").andExpect(status().isNotFound());
        assertTrue(fake.pedidos.isEmpty());
    }

    @Test
    void clienteSemDocumentoPrecisaInformarOCpfEOSistemaGuarda() throws Exception {
        long cli = cliente("Maria", null);
        long os = os(cli, "100");
        mvc.perform(get("/api/ordens/" + os + "/cobrancas").cookie(cookie)).andExpect(jsonPath("$.clienteTemDocumento").value(false));
        cobrar(os, "{\"tipo\":\"PIX\"}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.erro").value(org.hamcrest.Matchers.containsString("CPF ou CNPJ")));
        cobrar(os, "{\"tipo\":\"PIX\",\"cpfCnpj\":\"123\"}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.erro").value(org.hamcrest.Matchers.containsString("11 ou 14")));
        assertTrue(fake.pedidos.isEmpty());
        cobrar(os, "{\"tipo\":\"PIX\",\"cpfCnpj\":\"123.456.789-09\"}").andExpect(status().isOk());
        mvc.perform(get("/api/clientes/" + cli).cookie(cookie)).andExpect(jsonPath("$.documento").value("12345678909"));
        mvc.perform(get("/api/ordens/" + os + "/cobrancas").cookie(cookie)).andExpect(jsonPath("$.clienteTemDocumento").value(true));
    }

    @Test
    void clienteAnonimizadoNaoPodeSerCobrado() throws Exception {
        long cli = cliente("Maria", "12345678909");
        long os = os(cli, "100");
        mvc.perform(post("/api/clientes/" + cli + "/anonimizar").cookie(cookie)).andExpect(status().isOk());
        cobrar(os, "{\"tipo\":\"PIX\"}").andExpect(status().isConflict());
    }

    @Test
    void oClienteDoAsaasEReaproveitadoNasCobrancasSeguintes() throws Exception {
        long cli = cliente("Maria", "12345678909");
        cobrar(os(cli, "100"), "{\"tipo\":\"PIX\"}").andExpect(status().isOk());
        cobrar(os(cli, "50"), "{\"tipo\":\"PIX\"}").andExpect(status().isOk());
        assertNull(fake.pedidos.get(0).clienteIdExterno());
        assertEquals("cus_fake_1", fake.pedidos.get(1).clienteIdExterno());
        assertEquals(1, fake.clientesCriados);
        assertEquals("cus_fake_1", jdbc.queryForObject("select asaas_id from cliente where id = ?", String.class, cli));
    }

    @Test
    void quandoOAsaasRecusaMostraOMotivoENaoGuardaNada() throws Exception {
        long os = os(cliente("Maria", "12345678909"), "100");
        fake.falha = new GatewayException(HttpStatus.BAD_REQUEST, "O CPF/CNPJ informado é inválido.");
        cobrar(os, "{\"tipo\":\"PIX\"}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.erro").value("O CPF/CNPJ informado é inválido."));
        fake.falha = new GatewayException(HttpStatus.BAD_GATEWAY, "Não foi possível falar com o Asaas agora. Tente de novo em instantes.");
        cobrar(os, "{\"tipo\":\"PIX\"}").andExpect(status().isBadGateway());
        assertEquals(0, contar("cobranca"));
        mvc.perform(get("/api/ordens/" + os + "/cobrancas").cookie(cookie)).andExpect(jsonPath("$.disponivel").value(100.0));
    }

    // ---------- cancelar ----------

    @Test
    void cancelarCobrancaLiberaOSaldoEChamaOGateway() throws Exception {
        long os = os(cliente("Maria", "12345678909"), "100");
        cobrar(os, "{\"tipo\":\"PIX\"}").andExpect(status().isOk());
        long c = jdbc.queryForObject("select id from cobranca where os_id = ?", Long.class, os);
        mvc.perform(post("/api/cobrancas/" + c + "/cancelar").cookie(cookie)).andExpect(status().isOk())
                .andExpect(jsonPath("$.cobrancas[0].status").value("CANCELADA")).andExpect(jsonPath("$.disponivel").value(100.0));
        assertEquals(List.of(asaasIdDaCobranca(os)), fake.cancelados);
        mvc.perform(post("/api/cobrancas/" + c + "/cancelar").cookie(cookie)).andExpect(status().isConflict());
        mvc.perform(post("/api/cobrancas/99999999/cancelar").cookie(cookie)).andExpect(status().isNotFound());
    }

    @Test
    void seOGatewayNaoConseguirCancelarACobrancaContinuaPendente() throws Exception {
        long os = os(cliente("Maria", "12345678909"), "100");
        cobrar(os, "{\"tipo\":\"PIX\"}");
        long c = jdbc.queryForObject("select id from cobranca where os_id = ?", Long.class, os);
        fake.falha = new GatewayException(HttpStatus.BAD_REQUEST, "Esta cobrança não pode ser removida.");
        mvc.perform(post("/api/cobrancas/" + c + "/cancelar").cookie(cookie)).andExpect(status().isBadRequest());
        assertEquals("PENDENTE", jdbc.queryForObject("select status from cobranca where id = ?", String.class, c));
    }

    @Test
    void naoCancelaAOsComCobrancaPendenteMasCancelaDepoisDeCancelarACobranca() throws Exception {
        long os = os(cliente("Maria", "12345678909"), "100");
        cobrar(os, "{\"tipo\":\"PIX\"}");
        json(patch("/api/ordens/" + os + "/status"), "{\"status\":\"CANCELADA\"}").andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value(org.hamcrest.Matchers.containsString("cobrança online pendente")));
        long c = jdbc.queryForObject("select id from cobranca where os_id = ?", Long.class, os);
        mvc.perform(post("/api/cobrancas/" + c + "/cancelar").cookie(cookie)).andExpect(status().isOk());
        json(patch("/api/ordens/" + os + "/status"), "{\"status\":\"CANCELADA\"}").andExpect(status().isOk());
    }

    // ---------- webhook ----------

    @Test
    void webhookSemTokenOuComTokenErradoDa401() throws Exception {
        String corpo = evento("PAYMENT_RECEIVED", "pay_x", "PIX", null);
        webhook(null, corpo).andExpect(status().isUnauthorized());
        webhook("errado", corpo).andExpect(status().isUnauthorized());
        webhook("token-de-teste ", corpo).andExpect(status().isUnauthorized());
        webhook("", corpo).andExpect(status().isUnauthorized());
        assertEquals(0, contar("pagamento"));
    }

    @Test
    void webhookIgnoraEventoSemNadaAFazerMasAvisaCorpoInvalido() throws Exception {
        webhook("token-de-teste", "{\"event\":\"PAYMENT_CREATED\",\"payment\":{\"id\":\"pay_1\"}}").andExpect(status().isOk()).andExpect(jsonPath("$.received").value(true));
        webhook("token-de-teste", "{\"event\":\"PAYMENT_OVERDUE\",\"payment\":{\"id\":\"pay_1\"}}").andExpect(status().isOk());
        webhook("token-de-teste", "{\"event\":\"INVOICE_CREATED\"}").andExpect(status().isOk());
        webhook("token-de-teste", "{\"payment\":{\"id\":\"pay_1\"}}").andExpect(status().isBadRequest());
        webhook("token-de-teste", "{\"event\":\"PAYMENT_RECEIVED\",\"payment\":{}}").andExpect(status().isBadRequest());
        webhook("token-de-teste", "não é json").andExpect(status().isBadRequest());
        assertEquals(0, contar("pagamento"));
    }

    @Test
    void cobrancaQueNaoEDesteSistemaEIgnoradaComSucesso() throws Exception {
        webhook("token-de-teste", evento("PAYMENT_RECEIVED", "pay_de_outro_lugar", "PIX", null)).andExpect(status().isOk());
        assertEquals(0, contar("pagamento"));
    }

    @Test
    void pagamentoRecebidoVirouPagamentoNoCaixaDaOsComOrigemAsaas() throws Exception {
        long os = os(cliente("Maria", "12345678909"), "100");
        cobrar(os, "{\"tipo\":\"PIX\",\"valor\":60}").andExpect(status().isOk());
        String pay = asaasIdDaCobranca(os);
        webhook("token-de-teste", evento("PAYMENT_RECEIVED", pay, "PIX", hoje)).andExpect(status().isOk());

        mvc.perform(get("/api/ordens/" + os + "/financeiro").cookie(cookie)).andExpect(jsonPath("$.pago").value(60.0)).andExpect(jsonPath("$.saldo").value(40.0))
                .andExpect(jsonPath("$.pagamentos[0].origem").value("ASAAS")).andExpect(jsonPath("$.pagamentos[0].forma").value("PIX"))
                .andExpect(jsonPath("$.pagamentos[0].data").value(hoje));
        mvc.perform(get("/api/ordens/" + os + "/cobrancas").cookie(cookie)).andExpect(jsonPath("$.cobrancas[0].status").value("PAGA"));
        mvc.perform(get("/api/financeiro/caixa").cookie(cookie)).andExpect(jsonPath("$.porForma.PIX").value(60.0)).andExpect(jsonPath("$.entradas").value(60.0));
        assertEquals(1, jdbc.queryForObject("select count(*) from auditoria where acao = 'PAGAMENTO_ASAAS'", Integer.class));
    }

    @Test
    void omesmoEventoDuasVezesERecebidoEConfirmadoDepoisRegistramUmPagamentoSo() throws Exception {
        long os = os(cliente("Maria", "12345678909"), "100");
        cobrar(os, "{\"tipo\":\"CREDIT_CARD\"}").andExpect(status().isOk());
        String pay = asaasIdDaCobranca(os);
        String confirmado = evento("PAYMENT_CONFIRMED", pay, "CREDIT_CARD", hoje);
        webhook("token-de-teste", confirmado).andExpect(status().isOk());
        webhook("token-de-teste", confirmado).andExpect(status().isOk());                                    // reenvio
        webhook("token-de-teste", evento("PAYMENT_RECEIVED", pay, "CREDIT_CARD", hoje)).andExpect(status().isOk()); // liquidação, dias depois
        webhook("token-de-teste", evento("PAYMENT_RECEIVED", pay, "CREDIT_CARD", hoje)).andExpect(status().isOk());
        assertEquals(1, contar("pagamento"));
        mvc.perform(get("/api/financeiro/caixa").cookie(cookie)).andExpect(jsonPath("$.porForma.CARTAO").value(100.0)).andExpect(jsonPath("$.entradas").value(100.0));
    }

    @Test
    void oBancoNaoAceitaDoisPagamentosComOMesmoIdDoAsaas() throws Exception {
        long os = os(cliente("Maria", "12345678909"), "100");
        jdbc.update("insert into pagamento (os_id, valor, forma, data, origem, id_externo) values (?, 10, 'PIX', current_date, 'ASAAS', 'pay_dup')", os);
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("insert into pagamento (os_id, valor, forma, data, origem, id_externo) values (?, 10, 'PIX', current_date, 'ASAAS', 'pay_dup')", os));
    }

    @Test
    void formaDoPagamentoVemDoTipoRealPagoEADataDoAsaasValeSeNaoForFutura() throws Exception {
        String[][] casos = {{"BOLETO", "BOLETO"}, {"CREDIT_CARD", "CARTAO"}, {"DEBIT_CARD", "CARTAO"}, {"PIX", "PIX"}, {"TRANSFER", "PIX"}};
        for (String[] caso : casos) {
            long os = os(cliente("Cli " + caso[0], "12345678909"), "10");
            cobrar(os, "{\"tipo\":\"UNDEFINED\"}").andExpect(status().isOk());
            webhook("token-de-teste", evento("PAYMENT_RECEIVED", asaasIdDaCobranca(os), caso[0], ontem)).andExpect(status().isOk());
            mvc.perform(get("/api/ordens/" + os + "/financeiro").cookie(cookie)).andExpect(jsonPath("$.pagamentos[0].forma").value(caso[1]))
                    .andExpect(jsonPath("$.pagamentos[0].data").value(ontem));
        }
        long futura = os(cliente("Futuro", "12345678909"), "10");
        cobrar(futura, "{\"tipo\":\"PIX\"}");
        webhook("token-de-teste", evento("PAYMENT_RECEIVED", asaasIdDaCobranca(futura), "PIX", amanha)).andExpect(status().isOk());
        mvc.perform(get("/api/ordens/" + futura + "/financeiro").cookie(cookie)).andExpect(jsonPath("$.pagamentos[0].data").value(hoje));
        long semData = os(cliente("Sem data", "12345678909"), "10");
        cobrar(semData, "{\"tipo\":\"PIX\"}");
        webhook("token-de-teste", evento("PAYMENT_RECEIVED", asaasIdDaCobranca(semData), null, null)).andExpect(status().isOk());
        mvc.perform(get("/api/ordens/" + semData + "/financeiro").cookie(cookie)).andExpect(jsonPath("$.pagamentos[0].data").value(hoje));
    }

    @Test
    void estornoNoAsaasMarcaOPagamentoComoEstornadoEUmaSoVez() throws Exception {
        long os = os(cliente("Maria", "12345678909"), "100");
        cobrar(os, "{\"tipo\":\"PIX\"}");
        String pay = asaasIdDaCobranca(os);
        webhook("token-de-teste", evento("PAYMENT_RECEIVED", pay, "PIX", hoje));
        webhook("token-de-teste", evento("PAYMENT_REFUNDED", pay, "PIX", null)).andExpect(status().isOk());
        webhook("token-de-teste", evento("PAYMENT_REFUNDED", pay, "PIX", null)).andExpect(status().isOk());
        mvc.perform(get("/api/ordens/" + os + "/financeiro").cookie(cookie)).andExpect(jsonPath("$.pago").value(0.0)).andExpect(jsonPath("$.pagamentos[0].estornado").value(true));
        mvc.perform(get("/api/ordens/" + os + "/cobrancas").cookie(cookie)).andExpect(jsonPath("$.cobrancas[0].status").value("ESTORNADA"));
        mvc.perform(get("/api/financeiro/caixa").cookie(cookie)).andExpect(jsonPath("$.entradas").value(0.0));
        assertEquals(1, jdbc.queryForObject("select count(*) from auditoria where acao = 'PAGAMENTO_ASAAS_ESTORNADO'", Integer.class));
        webhook("token-de-teste", evento("PAYMENT_REFUNDED", "pay_desconhecido", "PIX", null)).andExpect(status().isOk());
    }

    @Test
    void pagamentoDoAsaasNaoPodeSerEstornadoAMao() throws Exception {
        long os = os(cliente("Maria", "12345678909"), "100");
        cobrar(os, "{\"tipo\":\"PIX\"}");
        webhook("token-de-teste", evento("PAYMENT_RECEIVED", asaasIdDaCobranca(os), "PIX", hoje));
        long pag = jdbc.queryForObject("select id from pagamento where os_id = ?", Long.class, os);
        mvc.perform(post("/api/pagamentos/" + pag + "/estornar").cookie(cookie)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value(org.hamcrest.Matchers.containsString("Asaas")));
    }

    @Test
    void comOPagamentoDoAsaasAOsPodeSerEntregue() throws Exception {
        long os = os(cliente("Maria", "12345678909"), "100");
        for (String s : new String[]{"EM_EXECUCAO", "PRONTA"}) json(patch("/api/ordens/" + os + "/status"), "{\"status\":\"" + s + "\"}").andExpect(status().isOk());
        json(patch("/api/ordens/" + os + "/status"), "{\"status\":\"ENTREGUE\"}").andExpect(status().isConflict());
        cobrar(os, "{\"tipo\":\"PIX\"}").andExpect(status().isOk());
        json(patch("/api/ordens/" + os + "/status"), "{\"status\":\"ENTREGUE\"}").andExpect(status().isConflict()); // cobrado, ainda não pago
        webhook("token-de-teste", evento("PAYMENT_RECEIVED", asaasIdDaCobranca(os), "PIX", hoje));
        json(patch("/api/ordens/" + os + "/status"), "{\"status\":\"ENTREGUE\"}").andExpect(status().isOk());
    }
}
