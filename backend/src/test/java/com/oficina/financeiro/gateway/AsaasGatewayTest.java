package com.oficina.financeiro.gateway;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.oficina.financeiro.gateway.GatewayPagamento.CobrancaCriada;
import com.oficina.financeiro.gateway.GatewayPagamento.NovaCobranca;
import com.oficina.financeiro.gateway.GatewayPagamento.TipoCobranca;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Campos e cabeçalhos conferidos na documentação oficial do Asaas (2026-11-11). Sem chamar o Asaas de verdade. */
class AsaasGatewayTest {
    static final String BASE = "https://api-sandbox.asaas.com/v3";
    static final String CHAVE = "$aact_hmlg_chave-secreta-de-teste";

    private record Montagem(AsaasGateway gateway, MockRestServiceServer servidor) {}

    private Montagem montar(boolean ativo) {
        RestClient.Builder b = RestClient.builder().baseUrl(BASE).defaultHeader("access_token", CHAVE)
                .defaultHeader("User-Agent", "oficinaAuto").defaultHeader("Content-Type", "application/json");
        MockRestServiceServer s = MockRestServiceServer.bindTo(b).build();
        return new Montagem(new AsaasGateway(b.build(), ativo), s);
    }

    private NovaCobranca pedido(String clienteId) {
        return new NovaCobranca(clienteId, "Maria Souza", "12345678909", "maria@x.com", TipoCobranca.PIX,
                new BigDecimal("100.50"), LocalDate.of(2026, 11, 20), "Ordem de serviço 7", "os-7");
    }

    @Test
    void criaOClienteEDepoisACobrancaComOsCamposECabecalhosDoAsaas() {
        var m = montar(true);
        m.servidor().expect(requestTo(BASE + "/customers")).andExpect(method(HttpMethod.POST))
                .andExpect(header("access_token", CHAVE)).andExpect(header("User-Agent", "oficinaAuto"))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.name").value("Maria Souza")).andExpect(jsonPath("$.cpfCnpj").value("12345678909"))
                .andExpect(jsonPath("$.email").value("maria@x.com"))
                .andRespond(withSuccess("{\"object\":\"customer\",\"id\":\"cus_001\"}", MediaType.APPLICATION_JSON));
        m.servidor().expect(requestTo(BASE + "/payments")).andExpect(method(HttpMethod.POST))
                .andExpect(header("access_token", CHAVE))
                .andExpect(jsonPath("$.customer").value("cus_001")).andExpect(jsonPath("$.billingType").value("PIX"))
                .andExpect(jsonPath("$.value").value(100.5)).andExpect(jsonPath("$.dueDate").value("2026-11-20"))
                .andExpect(jsonPath("$.description").value("Ordem de serviço 7")).andExpect(jsonPath("$.externalReference").value("os-7"))
                .andRespond(withSuccess("{\"object\":\"payment\",\"id\":\"pay_001\",\"invoiceUrl\":\"https://sandbox.asaas.com/i/001\",\"status\":\"PENDING\"}",
                        MediaType.APPLICATION_JSON));

        CobrancaCriada c = m.gateway().criarCobranca(pedido(null));

        assertEquals("pay_001", c.idCobranca());
        assertEquals("cus_001", c.clienteIdExterno());
        assertEquals("https://sandbox.asaas.com/i/001", c.link());
        assertEquals("PENDING", c.status());
        m.servidor().verify();
    }

    @Test
    void clienteJaCriadoNaoECriadoDeNovo() {
        var m = montar(true);
        m.servidor().expect(requestTo(BASE + "/payments")).andExpect(jsonPath("$.customer").value("cus_existente"))
                .andRespond(withSuccess("{\"id\":\"pay_002\",\"status\":\"PENDING\"}", MediaType.APPLICATION_JSON));
        CobrancaCriada c = m.gateway().criarCobranca(pedido("cus_existente"));
        assertEquals("cus_existente", c.clienteIdExterno());
        assertNull(c.link());
        m.servidor().verify(); // só 1 chamada: nada de /customers
    }

    @Test
    void mandaOTipoDeCobrancaCertoParaBoletoCartaoEEscolhaDoCliente() {
        for (TipoCobranca tipo : TipoCobranca.values()) {
            var m = montar(true);
            m.servidor().expect(requestTo(BASE + "/payments")).andExpect(jsonPath("$.billingType").value(tipo.name()))
                    .andRespond(withSuccess("{\"id\":\"pay_x\",\"status\":\"PENDING\"}", MediaType.APPLICATION_JSON));
            m.gateway().criarCobranca(new NovaCobranca("cus_1", "A", "12345678909", null, tipo, BigDecimal.TEN, LocalDate.of(2026, 11, 20), null, null));
            m.servidor().verify();
        }
    }

    @Test
    void naoEnviaEmailNemDescricaoQuandoNaoHa() {
        var m = montar(true);
        m.servidor().expect(requestTo(BASE + "/customers")).andExpect(jsonPath("$.email").doesNotExist())
                .andRespond(withSuccess("{\"id\":\"cus_9\"}", MediaType.APPLICATION_JSON));
        m.servidor().expect(requestTo(BASE + "/payments")).andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.externalReference").doesNotExist())
                .andRespond(withSuccess("{\"id\":\"pay_9\",\"status\":\"PENDING\"}", MediaType.APPLICATION_JSON));
        m.gateway().criarCobranca(new NovaCobranca(null, "A", "12345678909", null, TipoCobranca.PIX, BigDecimal.TEN, LocalDate.of(2026, 11, 20), null, null));
        m.servidor().verify();
    }

    @Test
    void mostraMotivoDoAsaasQuandoRecusaOsDados() {
        var m = montar(true);
        m.servidor().expect(requestTo(BASE + "/customers")).andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                .body("{\"errors\":[{\"code\":\"invalid_object\",\"description\":\"O CPF/CNPJ informado é inválido.\"}]}"));
        GatewayException e = assertThrows(GatewayException.class, () -> m.gateway().criarCobranca(pedido(null)));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        assertEquals("O CPF/CNPJ informado é inválido.", e.getMessage());
    }

    @Test
    void erroDeChaveNaoVazaAChaveEPedeConferirAConfiguracao() {
        var m = montar(true);
        m.servidor().expect(requestTo(BASE + "/customers")).andRespond(withStatus(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON)
                .body("{\"errors\":[{\"code\":\"invalid_access_token\",\"description\":\"A chave de API fornecida é inválida\"}]}"));
        GatewayException e = assertThrows(GatewayException.class, () -> m.gateway().criarCobranca(pedido(null)));
        assertEquals(HttpStatus.BAD_GATEWAY, e.getStatus());
        assertTrue(e.getMessage().contains("chave de API"));
        assertFalse(e.getMessage().contains(CHAVE));
    }

    @Test
    void falhaDoServidorDoAsaasViraMensagemGenericaSemDetalhes() {
        var m = montar(true);
        m.servidor().expect(requestTo(BASE + "/customers")).andRespond(withServerError().body("stack trace interno " + CHAVE));
        GatewayException e = assertThrows(GatewayException.class, () -> m.gateway().criarCobranca(pedido(null)));
        assertEquals(HttpStatus.BAD_GATEWAY, e.getStatus());
        assertFalse(e.getMessage().contains("stack") || e.getMessage().contains(CHAVE));
    }

    @Test
    void semChaveNaoChamaNada() {
        var m = montar(false);
        assertFalse(m.gateway().ativo());
        GatewayException e = assertThrows(GatewayException.class, () -> m.gateway().criarCobranca(pedido(null)));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        m.servidor().verify(); // nenhuma requisição foi feita
    }

    @Test
    void cancelaACobrancaComDelete() {
        var m = montar(true);
        m.servidor().expect(requestTo(BASE + "/payments/pay_001")).andExpect(method(HttpMethod.DELETE)).andExpect(header("access_token", CHAVE))
                .andRespond(withSuccess("{\"deleted\":true,\"id\":\"pay_001\"}", MediaType.APPLICATION_JSON));
        m.gateway().cancelarCobranca("pay_001");
        m.servidor().verify();
    }

    @Test
    void consultaACobranca() {
        var m = montar(true);
        m.servidor().expect(requestTo(BASE + "/payments/pay_001")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":\"pay_001\",\"status\":\"RECEIVED\",\"value\":100.5,\"paymentDate\":\"2026-11-10\",\"billingType\":\"PIX\"}", MediaType.APPLICATION_JSON));
        var c = m.gateway().consultar("pay_001");
        assertEquals("RECEIVED", c.status());
        assertEquals(new BigDecimal("100.5"), c.valor());
        assertEquals(LocalDate.of(2026, 11, 10), c.dataPagamento());
        assertEquals("PIX", c.forma());
    }

    @Test
    void consultaSemDataDePagamentoDevolveNulo() {
        var m = montar(true);
        m.servidor().expect(requestTo(BASE + "/payments/pay_2")).andRespond(withSuccess("{\"id\":\"pay_2\",\"status\":\"PENDING\",\"value\":10}", MediaType.APPLICATION_JSON));
        assertNull(m.gateway().consultar("pay_2").dataPagamento());
    }

    @Test
    void avisoDeAmbienteTrocadoNaoQuebraAInicializacao() {
        assertDoesNotThrow(() -> GatewayConfig.avisarSeAmbienteNaoCombina(BASE, "$aact_prod_x"));
        assertDoesNotThrow(() -> GatewayConfig.avisarSeAmbienteNaoCombina("https://api.asaas.com/v3", "$aact_hmlg_x"));
        assertDoesNotThrow(() -> GatewayConfig.avisarSeAmbienteNaoCombina(BASE, "$aact_hmlg_x"));
    }
}
