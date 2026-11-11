package com.oficina.financeiro;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.oficina.funcionarios.FuncionarioForm;
import com.oficina.funcionarios.FuncionarioService;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Sem @Transactional de propósito: o dinheiro precisa de transações reais (rollback, estorno, caixa). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FinanceiroIntegracaoTest {
    @Autowired MockMvc mvc;
    @Autowired FuncionarioService funcionarios;
    @Autowired JdbcTemplate jdbc;
    Cookie cookie;
    final String hoje = LocalDate.now().toString();
    final String ontem = LocalDate.now().minusDays(1).toString();
    final String amanha = LocalDate.now().plusDays(1).toString();

    @BeforeEach
    void entrar() throws Exception {
        limpar();
        funcionarios.criar(new FuncionarioForm("Ana", null, null, null, "ana", "senha-boa-123"));
        cookie = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"ana\",\"senha\":\"senha-boa-123\"}")).andReturn().getResponse().getCookie("oficina_token");
    }

    @AfterEach
    void limpar() {
        for (String t : new String[]{"receber", "pagamento", "despesa", "agendamento_servico", "agendamento", "os_item", "ordem_servico",
                "auditoria", "veiculo", "cliente", "servico", "funcionario"}) {
            jdbc.execute("delete from " + t);
        }
    }

    private long id(String json) { return Long.parseLong(json.replaceAll("^\\{\"id\":(\\d+).*", "$1")); }

    private long os(String preco) throws Exception {
        return id(mvc.perform(post("/api/ordens").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content("{\"itens\":[{\"descricao\":\"Serviço\",\"quantidade\":1,\"precoUnit\":" + preco + "}]}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private ResultActions json(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder b, String corpo) throws Exception {
        return mvc.perform(b.cookie(cookie).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private ResultActions pagar(long os, String valor, String forma, String data) throws Exception {
        return json(post("/api/ordens/" + os + "/pagamentos"),
                "{\"valor\":" + valor + ",\"forma\":\"" + forma + "\"" + (data == null ? "" : ",\"data\":\"" + data + "\"") + "}");
    }

    private ResultActions fiar(long os, String valor, String venc) throws Exception {
        return json(post("/api/ordens/" + os + "/fiado"), "{\"valor\":" + valor + ",\"vencimento\":\"" + venc + "\"}");
    }

    private ResultActions mudar(long os, String status) throws Exception {
        return json(patch("/api/ordens/" + os + "/status"), "{\"status\":\"" + status + "\"}");
    }

    private void ate(long os, String status) throws Exception {
        String[] fluxo = {"EM_EXECUCAO", "PRONTA", "ENTREGUE"};
        for (String s : fluxo) {
            mudar(os, s).andExpect(status().isOk());
            if (s.equals(status)) return;
        }
    }

    private long idDoPagamento(long os, int posicao) throws Exception {
        String r = mvc.perform(get("/api/ordens/" + os + "/financeiro").cookie(cookie)).andReturn().getResponse().getContentAsString();
        return Long.parseLong(com.jayway.jsonpath.JsonPath.read(r, "$.pagamentos[" + posicao + "].id").toString());
    }

    private long idDoFiado(long os) throws Exception {
        String r = mvc.perform(get("/api/ordens/" + os + "/financeiro").cookie(cookie)).andReturn().getResponse().getContentAsString();
        return Long.parseLong(com.jayway.jsonpath.JsonPath.read(r, "$.fiados[0].id").toString());
    }

    private int contar(String tabela) { return jdbc.queryForObject("select count(*) from " + tabela, Integer.class); }

    @Test
    void exigeLogin() throws Exception {
        for (String url : new String[]{"/api/financeiro/caixa", "/api/pagamentos", "/api/receber", "/api/receber/resumo", "/api/despesas", "/api/ordens/1/financeiro"}) {
            mvc.perform(get(url)).andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/pagamentos/1/estornar")).andExpect(status().isUnauthorized());
    }

    // ---------- pagamentos ----------

    @Test
    void pagamentoParcialAtualizaOSaldoEMostraOResumo() throws Exception {
        long os = os("100");
        mvc.perform(get("/api/ordens/" + os + "/financeiro").cookie(cookie)).andExpect(jsonPath("$.total").value(100.0))
                .andExpect(jsonPath("$.saldo").value(100.0)).andExpect(jsonPath("$.podeEntregar").value(false));
        pagar(os, "40.50", "DINHEIRO", null).andExpect(status().isOk()).andExpect(jsonPath("$.pago").value(40.5))
                .andExpect(jsonPath("$.saldo").value(59.5)).andExpect(jsonPath("$.pagamentos[0].forma").value("DINHEIRO"))
                .andExpect(jsonPath("$.pagamentos[0].data").value(hoje)).andExpect(jsonPath("$.pagamentos[0].origem").value("MANUAL"));
        pagar(os, "59.50", "PIX", null).andExpect(jsonPath("$.saldo").value(0.0)).andExpect(jsonPath("$.podeEntregar").value(true))
                .andExpect(jsonPath("$.pagamentos.length()").value(2));
    }

    @Test
    void recusaValorInvalidoAcimaDoSaldoDataFuturaOuOsSemSaldo() throws Exception {
        long os = os("100");
        pagar(os, "0", "PIX", null).andExpect(status().isBadRequest());
        pagar(os, "-5", "PIX", null).andExpect(status().isBadRequest());
        pagar(os, "10.123", "PIX", null).andExpect(status().isBadRequest());
        pagar(os, "10", "CHEQUE", null).andExpect(status().isBadRequest());
        pagar(os, "100.01", "PIX", null).andExpect(status().isBadRequest()).andExpect(jsonPath("$.erro").value(org.hamcrest.Matchers.containsString("saldo")));
        pagar(os, "10", "PIX", amanha).andExpect(status().isBadRequest());
        json(post("/api/ordens/" + os + "/pagamentos"), "{\"valor\":10}").andExpect(status().isBadRequest());
        pagar(os, "100", "PIX", ontem).andExpect(status().isOk());
        pagar(os, "1", "PIX", null).andExpect(status().isBadRequest()); // já quitada
        pagar(999999, "1", "PIX", null).andExpect(status().isNotFound());
        assertEquals(1, contar("pagamento"));
    }

    @Test
    void naoRecebeEmOsCancelada() throws Exception {
        long os = os("100");
        mudar(os, "CANCELADA").andExpect(status().isOk());
        pagar(os, "10", "PIX", null).andExpect(status().isConflict());
        fiar(os, "10", hoje).andExpect(status().isConflict());
    }

    // ---------- fiado ----------

    @Test
    void fiadoContaNoSaldoMasNaoNoPagoERecusaVencimentoPassadoEValorAlto() throws Exception {
        long os = os("100");
        pagar(os, "30", "DINHEIRO", null).andExpect(status().isOk());
        fiar(os, "70.01", hoje).andExpect(status().isBadRequest());
        fiar(os, "20", ontem).andExpect(status().isBadRequest());
        fiar(os, "70", amanha).andExpect(status().isOk()).andExpect(jsonPath("$.pago").value(30.0)).andExpect(jsonPath("$.fiadoAberto").value(70.0))
                .andExpect(jsonPath("$.saldo").value(0.0)).andExpect(jsonPath("$.fiados[0].status").value("ABERTO"))
                .andExpect(jsonPath("$.fiados[0].vencimento").value(amanha));
        pagar(os, "1", "PIX", null).andExpect(status().isBadRequest()); // saldo zero: tudo pago ou fiado
    }

    // ---------- regra de entrega ----------

    @Test
    void naoEntregaSemReceberEEntregaComPagamentoMaisFiado() throws Exception {
        long os = os("100");
        ate(os, "PRONTA");
        mudar(os, "ENTREGUE").andExpect(status().isConflict()).andExpect(jsonPath("$.erro").value(org.hamcrest.Matchers.containsString("Falta receber R$ 100,00")));
        pagar(os, "40", "DINHEIRO", null).andExpect(status().isOk());
        mudar(os, "ENTREGUE").andExpect(status().isConflict()).andExpect(jsonPath("$.erro").value(org.hamcrest.Matchers.containsString("R$ 60,00")));
        fiar(os, "60", amanha).andExpect(status().isOk());
        mudar(os, "ENTREGUE").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ENTREGUE"));
    }

    @Test
    void osSemValorEntregaSemPagamento() throws Exception {
        long os = id(mvc.perform(post("/api/ordens").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn().getResponse().getContentAsString());
        ate(os, "ENTREGUE");
    }

    @Test
    void osEditadaParaBaixoDepoisDePagarNaoEntregaAteAjustar() throws Exception {
        long os = os("100");
        pagar(os, "100", "PIX", null).andExpect(status().isOk());
        ate(os, "PRONTA");
        json(put("/api/ordens/" + os), "{\"itens\":[{\"descricao\":\"Serviço\",\"quantidade\":1,\"precoUnit\":50}]}").andExpect(status().isOk());
        mudar(os, "ENTREGUE").andExpect(status().isConflict()).andExpect(jsonPath("$.erro").value(org.hamcrest.Matchers.containsString("passam do total")));
        mvc.perform(get("/api/ordens/" + os + "/financeiro").cookie(cookie)).andExpect(jsonPath("$.saldo").value(-50.0)).andExpect(jsonPath("$.podeEntregar").value(false));
        long pag = idDoPagamento(os, 0);
        mvc.perform(post("/api/pagamentos/" + pag + "/estornar").cookie(cookie)).andExpect(status().isOk());
        pagar(os, "50", "PIX", null).andExpect(status().isOk());
        mudar(os, "ENTREGUE").andExpect(status().isOk());
    }

    @Test
    void osEditadaParaCimaDepoisDePagarExigeReceberADiferenca() throws Exception {
        long os = os("100");
        pagar(os, "100", "PIX", null).andExpect(status().isOk());
        ate(os, "PRONTA");
        json(put("/api/ordens/" + os), "{\"itens\":[{\"descricao\":\"Serviço\",\"quantidade\":1,\"precoUnit\":130}]}").andExpect(status().isOk());
        mudar(os, "ENTREGUE").andExpect(status().isConflict()).andExpect(jsonPath("$.erro").value(org.hamcrest.Matchers.containsString("R$ 30,00")));
    }

    @Test
    void reabrirOsEntregueNaoExigeNadaDoFinanceiro() throws Exception {
        long os = os("100");
        pagar(os, "100", "PIX", null);
        ate(os, "ENTREGUE");
        json(patch("/api/ordens/" + os + "/status"), "{\"status\":\"PRONTA\",\"confirmar\":true}").andExpect(status().isOk());
    }

    // ---------- estorno ----------

    @Test
    void estornarDevolveOSaldoSaiDoCaixaEFicaNoHistorico() throws Exception {
        long os = os("100");
        pagar(os, "100", "PIX", null).andExpect(status().isOk());
        long pag = idDoPagamento(os, 0);
        mvc.perform(post("/api/pagamentos/" + pag + "/estornar").cookie(cookie)).andExpect(status().isOk())
                .andExpect(jsonPath("$.pago").value(0.0)).andExpect(jsonPath("$.saldo").value(100.0))
                .andExpect(jsonPath("$.pagamentos[0].estornado").value(true));
        mvc.perform(get("/api/financeiro/caixa").cookie(cookie)).andExpect(jsonPath("$.entradas").value(0.0));
        mvc.perform(post("/api/pagamentos/" + pag + "/estornar").cookie(cookie)).andExpect(status().isConflict());
        mvc.perform(post("/api/pagamentos/99999999/estornar").cookie(cookie)).andExpect(status().isNotFound());
        assertEquals(1, jdbc.queryForObject("select count(*) from auditoria where acao = 'PAGAMENTO_ESTORNADO'", Integer.class));
    }

    @Test
    void estornarOPagamentoDeUmaBaixaReabreOFiado() throws Exception {
        long os = os("100");
        fiar(os, "100", amanha).andExpect(status().isOk());
        long fiado = idDoFiado(os);
        json(post("/api/receber/" + fiado + "/baixa"), "{\"forma\":\"DINHEIRO\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.fiados[0].status").value("PAGO")).andExpect(jsonPath("$.pago").value(100.0)).andExpect(jsonPath("$.fiadoAberto").value(0.0));
        long pag = idDoPagamento(os, 0);
        mvc.perform(post("/api/pagamentos/" + pag + "/estornar").cookie(cookie)).andExpect(status().isOk())
                .andExpect(jsonPath("$.fiados[0].status").value("ABERTO")).andExpect(jsonPath("$.fiadoAberto").value(100.0))
                .andExpect(jsonPath("$.pago").value(0.0)).andExpect(jsonPath("$.saldo").value(0.0));
    }

    // ---------- cancelamento ----------

    @Test
    void naoCancelaOsComPagamentoRecebidoMasCancelaDepoisDoEstorno() throws Exception {
        long os = os("100");
        pagar(os, "40", "PIX", null).andExpect(status().isOk());
        mudar(os, "CANCELADA").andExpect(status().isConflict()).andExpect(jsonPath("$.erro").value(org.hamcrest.Matchers.containsString("estorne")));
        mvc.perform(post("/api/pagamentos/" + idDoPagamento(os, 0) + "/estornar").cookie(cookie)).andExpect(status().isOk());
        mudar(os, "CANCELADA").andExpect(status().isOk());
    }

    @Test
    void cancelarAOsCancelaOFiadoEmAberto() throws Exception {
        long os = os("100");
        fiar(os, "100", amanha).andExpect(status().isOk());
        mudar(os, "CANCELADA").andExpect(status().isOk());
        mvc.perform(get("/api/receber?status=ABERTO").cookie(cookie)).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/receber?status=CANCELADO").cookie(cookie)).andExpect(jsonPath("$.totalElements").value(1));
    }

    // ---------- contas a receber ----------

    @Test
    void baixaDoFiadoGeraPagamentoNaDataInformadaEQuitaOFiado() throws Exception {
        long os = os("100");
        fiar(os, "100", amanha).andExpect(status().isOk());
        long fiado = idDoFiado(os);
        json(post("/api/receber/" + fiado + "/baixa"), "{\"forma\":\"PIX\",\"data\":\"" + ontem + "\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.fiados[0].pagoEm").value(ontem)).andExpect(jsonPath("$.pagamentos[0].data").value(ontem))
                .andExpect(jsonPath("$.pagamentos[0].valor").value(100.0));
        json(post("/api/receber/" + fiado + "/baixa"), "{\"forma\":\"PIX\"}").andExpect(status().isConflict());
        json(post("/api/receber/" + fiado + "/baixa"), "{}").andExpect(status().isBadRequest());
        mvc.perform(post("/api/receber/" + fiado + "/cancelar").cookie(cookie)).andExpect(status().isConflict());
        mvc.perform(get("/api/financeiro/caixa?data=" + ontem).cookie(cookie)).andExpect(jsonPath("$.porForma.PIX").value(100.0));
    }

    @Test
    void cancelarOFiadoLiberaOSaldoMasNaoCancelaUmJaCancelado() throws Exception {
        long os = os("100");
        fiar(os, "100", amanha);
        long fiado = idDoFiado(os);
        mvc.perform(post("/api/receber/" + fiado + "/cancelar").cookie(cookie)).andExpect(status().isOk())
                .andExpect(jsonPath("$.fiadoAberto").value(0.0)).andExpect(jsonPath("$.saldo").value(100.0)).andExpect(jsonPath("$.fiados[0].status").value("CANCELADO"));
        mvc.perform(post("/api/receber/" + fiado + "/cancelar").cookie(cookie)).andExpect(status().isConflict());
        json(post("/api/receber/" + fiado + "/baixa"), "{\"forma\":\"PIX\"}").andExpect(status().isConflict());
        mvc.perform(post("/api/receber/99999999/cancelar").cookie(cookie)).andExpect(status().isNotFound());
    }

    @Test
    void listaContasAReceberComFiltroBuscaAtrasoResumoEPaginacao() throws Exception {
        long cli = id(mvc.perform(post("/api/clientes").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Maria Souza\",\"telefone\":\"11988887777\"}")).andReturn().getResponse().getContentAsString());
        long os1 = id(mvc.perform(post("/api/ordens").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content("{\"clienteId\":" + cli + ",\"itens\":[{\"descricao\":\"S\",\"quantidade\":1,\"precoUnit\":100}]}")).andReturn().getResponse().getContentAsString());
        long os2 = os("50");
        fiar(os1, "100", amanha).andExpect(status().isOk());
        fiar(os2, "50", hoje).andExpect(status().isOk());
        jdbc.update("update receber set vencimento = current_date - 5 where os_id = ?", os2); // vencido há 5 dias (a API não aceita data passada)

        mvc.perform(get("/api/receber/resumo").cookie(cookie)).andExpect(jsonPath("$.totalAberto").value(150.0)).andExpect(jsonPath("$.totalVencido").value(50.0));
        mvc.perform(get("/api/receber?status=ABERTO").cookie(cookie)).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].osId").value(os2)) // mais antigo primeiro
                .andExpect(jsonPath("$.content[0].diasAtraso").value(5)).andExpect(jsonPath("$.content[1].diasAtraso").value(0))
                .andExpect(jsonPath("$.content[1].clienteNome").value("Maria Souza"));
        mvc.perform(get("/api/receber?q=souza").cookie(cookie)).andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/receber?q=" + os2).cookie(cookie)).andExpect(jsonPath("$.content[0].osId").value(os2));
        mvc.perform(get("/api/receber?size=1&page=1").cookie(cookie)).andExpect(jsonPath("$.content.length()").value(1)).andExpect(jsonPath("$.totalPages").value(2));
        json(post("/api/receber/" + idDoFiado(os1) + "/baixa"), "{\"forma\":\"PIX\"}").andExpect(status().isOk());
        mvc.perform(get("/api/receber/resumo").cookie(cookie)).andExpect(jsonPath("$.totalAberto").value(50.0));
        mvc.perform(get("/api/receber?status=PAGO").cookie(cookie)).andExpect(jsonPath("$.totalElements").value(1));
    }

    // ---------- caixa do dia ----------

    @Test
    void caixaSomaPorFormaDoDiaSubtraiDespesasEIgnoraEstornoEOutrosDias() throws Exception {
        long os = os("500");
        pagar(os, "40", "DINHEIRO", null).andExpect(status().isOk());
        pagar(os, "60", "PIX", null).andExpect(status().isOk());
        pagar(os, "25", "DINHEIRO", ontem).andExpect(status().isOk());
        pagar(os, "10", "CARTAO", null).andExpect(status().isOk());
        mvc.perform(post("/api/pagamentos/" + idDoPagamento(os, 3) + "/estornar").cookie(cookie)).andExpect(status().isOk()); // o cartão
        json(post("/api/despesas"), "{\"descricao\":\"Luz\",\"valor\":30,\"data\":\"" + hoje + "\",\"categoria\":\"Contas\"}").andExpect(status().isOk());
        json(post("/api/despesas"), "{\"descricao\":\"Antiga\",\"valor\":999,\"data\":\"" + ontem + "\"}").andExpect(status().isOk());

        mvc.perform(get("/api/financeiro/caixa").cookie(cookie)).andExpect(jsonPath("$.data").value(hoje))
                .andExpect(jsonPath("$.porForma.DINHEIRO").value(40.0)).andExpect(jsonPath("$.porForma.PIX").value(60.0)).andExpect(jsonPath("$.porForma.CARTAO").value(0.0))
                .andExpect(jsonPath("$.entradas").value(100.0)).andExpect(jsonPath("$.despesas").value(30.0)).andExpect(jsonPath("$.saldo").value(70.0));
        mvc.perform(get("/api/financeiro/caixa?data=" + ontem).cookie(cookie)).andExpect(jsonPath("$.porForma.DINHEIRO").value(25.0))
                .andExpect(jsonPath("$.entradas").value(25.0)).andExpect(jsonPath("$.despesas").value(999.0)).andExpect(jsonPath("$.saldo").value(-974.0));
        mvc.perform(get("/api/financeiro/caixa?data=2020-01-01").cookie(cookie)).andExpect(jsonPath("$.entradas").value(0.0));
    }

    @Test
    void asEntradasDoCaixaBatemComAListaDePagamentosDoDia() throws Exception {
        long os = os("1000");
        pagar(os, "10.10", "DINHEIRO", null); pagar(os, "20.20", "PIX", null); pagar(os, "30.30", "CARTAO", null); pagar(os, "5", "PIX", ontem);
        var corpo = mvc.perform(get("/api/pagamentos?de=" + hoje + "&ate=" + hoje + "&size=100").cookie(cookie)).andExpect(jsonPath("$.totalElements").value(3))
                .andReturn().getResponse().getContentAsString();
        java.util.List<Number> valores = com.jayway.jsonpath.JsonPath.read(corpo, "$.content[*].valor");
        double soma = valores.stream().mapToDouble(Number::doubleValue).sum();
        mvc.perform(get("/api/financeiro/caixa").cookie(cookie)).andExpect(jsonPath("$.entradas").value(org.hamcrest.Matchers.closeTo(soma, 0.001)));
        mvc.perform(get("/api/pagamentos?de=" + hoje + "&ate=" + hoje + "&forma=PIX").cookie(cookie)).andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/pagamentos?forma=XYZ").cookie(cookie)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/pagamentos?size=2&page=1").cookie(cookie)).andExpect(jsonPath("$.content.length()").value(2)).andExpect(jsonPath("$.totalPages").value(2));
    }

    // ---------- despesas ----------

    @Test
    void despesasCriaEditaExcluiFiltraEPagina() throws Exception {
        long luz = id(json(post("/api/despesas"), "{\"descricao\":\"Conta de luz\",\"valor\":150.75,\"data\":\"" + hoje + "\",\"categoria\":\"Contas\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.valor").value(150.75)).andReturn().getResponse().getContentAsString());
        json(post("/api/despesas"), "{\"descricao\":\"Ferramenta\",\"valor\":80,\"data\":\"" + ontem + "\",\"categoria\":\"Equipamentos\"}").andExpect(status().isOk());
        json(post("/api/despesas"), "{\"descricao\":\"Sem categoria\",\"valor\":10,\"data\":\"" + ontem + "\"}").andExpect(status().isOk());

        mvc.perform(get("/api/despesas").cookie(cookie)).andExpect(jsonPath("$.totalElements").value(3)).andExpect(jsonPath("$.content[0].descricao").value("Conta de luz"));
        mvc.perform(get("/api/despesas?categoria=contas").cookie(cookie)).andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/despesas?q=ferram").cookie(cookie)).andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/despesas?de=" + hoje + "&ate=" + hoje).cookie(cookie)).andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/despesas?size=2&page=1").cookie(cookie)).andExpect(jsonPath("$.content.length()").value(1)).andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get("/api/despesas/total").cookie(cookie)).andExpect(jsonPath("$").value(240.75));
        mvc.perform(get("/api/despesas/total?de=" + hoje + "&ate=" + hoje).cookie(cookie)).andExpect(jsonPath("$").value(150.75));
        mvc.perform(get("/api/despesas/categorias").cookie(cookie)).andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0]").value("Contas"));

        json(put("/api/despesas/" + luz), "{\"descricao\":\"Luz do mês\",\"valor\":160,\"data\":\"" + hoje + "\",\"categoria\":\"Contas\"}")
                .andExpect(jsonPath("$.descricao").value("Luz do mês")).andExpect(jsonPath("$.valor").value(160.0));
        mvc.perform(delete("/api/despesas/" + luz).cookie(cookie)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/despesas/" + luz).cookie(cookie)).andExpect(status().isNotFound());
        assertEquals(1, jdbc.queryForObject("select count(*) from auditoria where acao = 'DESPESA_EXCLUIDA'", Integer.class));
    }

    @Test
    void despesaRecusaDadosInvalidos() throws Exception {
        for (String corpo : new String[]{"{}", "{\"descricao\":\" \",\"valor\":10,\"data\":\"" + hoje + "\"}",
                "{\"descricao\":\"X\",\"valor\":0,\"data\":\"" + hoje + "\"}", "{\"descricao\":\"X\",\"valor\":10.123,\"data\":\"" + hoje + "\"}",
                "{\"descricao\":\"X\",\"valor\":10}", "{\"descricao\":\"X\",\"valor\":10,\"data\":\"1999-01-01\"}"}) {
            json(post("/api/despesas"), corpo).andExpect(status().isBadRequest());
        }
        assertEquals(0, contar("despesa"));
    }
}
