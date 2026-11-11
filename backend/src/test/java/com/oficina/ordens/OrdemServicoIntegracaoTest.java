package com.oficina.ordens;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.oficina.funcionarios.FuncionarioForm;
import com.oficina.funcionarios.FuncionarioService;
import jakarta.servlet.http.Cookie;
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

/**
 * De propósito SEM @Transactional: assim as respostas passam pela mesma carga sob demanda de
 * cliente, veículo e itens que acontece em produção (open-in-view desligado). O banco de teste
 * é limpo ao final de cada teste.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrdemServicoIntegracaoTest {
    @Autowired MockMvc mvc;
    @Autowired FuncionarioService funcionarios;
    @Autowired JdbcTemplate jdbc;
    Cookie cookie;

    @BeforeEach
    void entrar() throws Exception {
        limpar();
        funcionarios.criar(new FuncionarioForm("Ana", null, null, null, "ana", "senha-boa-123"));
        cookie = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"ana\",\"senha\":\"senha-boa-123\"}"))
                .andReturn().getResponse().getCookie("oficina_token");
    }

    @AfterEach
    void limpar() {
        for (String t : new String[]{"os_item", "ordem_servico", "auditoria", "veiculo", "cliente", "servico", "funcionario"}) {
            jdbc.execute("delete from " + t);
        }
    }

    private long id(String json) {
        return Long.parseLong(json.replaceAll("^\\{\"id\":(\\d+).*", "$1"));
    }

    private long criarOs(String json) throws Exception {
        return id(mvc.perform(post("/api/ordens").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private long criarCliente(String nome) throws Exception {
        return id(mvc.perform(post("/api/clientes").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"" + nome + "\",\"telefone\":\"11988887777\"}")).andReturn().getResponse().getContentAsString());
    }

    private void mudar(long os, String para, int esperado) throws Exception {
        mvc.perform(patch("/api/ordens/" + os + "/status").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + para + "\"}")).andExpect(status().is(esperado));
    }

    @Test
    void exigeLogin() throws Exception {
        mvc.perform(get("/api/ordens")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/ordens").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
    }

    @Test
    void osSemClienteSemVeiculoESemItensEPermitida() throws Exception {
        mvc.perform(post("/api/ordens").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ABERTA"))
                .andExpect(jsonPath("$.cliente").doesNotExist()).andExpect(jsonPath("$.totais.total").value(0));
    }

    @Test
    void calculaTotaisNoBackendEDevolveCompletoNaLeitura() throws Exception {
        long cli = criarCliente("Maria Souza");
        long os = criarOs("""
                {"clienteId":%d,"kmEntrada":45000,"defeitoRelatado":"Barulho na roda",
                 "itens":[{"descricao":"Balanceamento","quantidade":2,"precoUnit":80.50},
                          {"descricao":"Alinhamento","quantidade":1,"precoUnit":120}],
                 "maoDeObra":100,"maoDeObraDescricao":"Mão de obra extra",
                 "pecasValor":200,"pecasDescricao":"2 pneus",
                 "taxaCompraTipo":"PERCENTUAL","taxaCompra":10,
                 "descontoTipo":"PERCENTUAL","desconto":5}""".formatted(cli));
        // lê de novo (carga sob demanda de cliente e itens, sem transação de teste)
        mvc.perform(get("/api/ordens/" + os).cookie(cookie))
                .andExpect(jsonPath("$.cliente.nome").value("Maria Souza"))
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.itens[0].subtotal").value(161.0))
                .andExpect(jsonPath("$.totais.subtotalServicos").value(281.0))
                .andExpect(jsonPath("$.totais.taxaCompraValor").value(20.0))
                .andExpect(jsonPath("$.totais.descontoValor").value(30.05))
                .andExpect(jsonPath("$.totais.total").value(570.95))
                .andExpect(jsonPath("$.totais.receitaOficina").value(370.95))
                .andExpect(jsonPath("$.totais.repassePecas").value(200.0))
                .andExpect(jsonPath("$.valores.pecasDescricao").value("2 pneus"));
    }

    @Test
    void ignoraTotalEnviadoPeloCliente() throws Exception {
        mvc.perform(post("/api/ordens").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itens\":[{\"descricao\":\"X\",\"quantidade\":1,\"precoUnit\":10}],\"total\":999999}"))
                .andExpect(jsonPath("$.totais.total").value(10.0));
    }

    @Test
    void recusaDadosInvalidos() throws Exception {
        for (String json : new String[]{
                "{\"itens\":[{\"descricao\":\"X\",\"quantidade\":0,\"precoUnit\":10}]}",
                "{\"itens\":[{\"descricao\":\"X\",\"quantidade\":1,\"precoUnit\":-1}]}",
                "{\"itens\":[{\"descricao\":\" \",\"quantidade\":1,\"precoUnit\":1}]}",
                "{\"maoDeObra\":-5}", "{\"pecasValor\":1.234}", "{\"kmEntrada\":-1}",
                "{\"itens\":[{\"descricao\":\"X\",\"quantidade\":1,\"precoUnit\":100}],\"desconto\":100.01}",
                "{\"taxaCompraTipo\":\"PERCENTUAL\",\"taxaCompra\":150,\"pecasValor\":10}"}) {
            mvc.perform(post("/api/ordens").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erro").exists());
        }
    }

    @Test
    void percorreOFluxoSemPularEtapas() throws Exception {
        long os = criarOs("{}");
        mudar(os, "PRONTA", 409);
        mudar(os, "ENTREGUE", 409);
        mudar(os, "EM_EXECUCAO", 200);
        mudar(os, "ABERTA", 409);
        mudar(os, "PRONTA", 200);
        mudar(os, "ENTREGUE", 200);
        mudar(os, "INVALIDO", 400);
        mvc.perform(get("/api/ordens/" + os).cookie(cookie)).andExpect(jsonPath("$.status").value("ENTREGUE"));
    }

    @Test
    void osEntregueFicaImutavelEReabrirExigeConfirmacaoEGeraAuditoria() throws Exception {
        long os = criarOs("{\"observacoes\":\"antes\"}");
        mudar(os, "EM_EXECUCAO", 200); mudar(os, "PRONTA", 200); mudar(os, "ENTREGUE", 200);
        mvc.perform(put("/api/ordens/" + os).cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{\"observacoes\":\"depois\"}"))
                .andExpect(status().isConflict());
        mudar(os, "PRONTA", 409); // sem confirmar
        mvc.perform(patch("/api/ordens/" + os + "/status").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PRONTA\",\"confirmar\":true}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PRONTA"));
        Integer reaberturas = jdbc.queryForObject("select count(*) from auditoria where acao = 'OS_REABERTA'", Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(1, reaberturas);
        Integer mudancas = jdbc.queryForObject("select count(*) from auditoria where acao = 'OS_STATUS'", Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(3, mudancas);
    }

    @Test
    void osCanceladaNaoMudaMais() throws Exception {
        long os = criarOs("{}");
        mudar(os, "CANCELADA", 200);
        mudar(os, "EM_EXECUCAO", 409);
        mvc.perform(put("/api/ordens/" + os).cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    void editaItensEPrecosRecalculando() throws Exception {
        long os = criarOs("{\"itens\":[{\"descricao\":\"A\",\"quantidade\":1,\"precoUnit\":10}]}");
        mvc.perform(put("/api/ordens/" + os).cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itens\":[{\"descricao\":\"B\",\"quantidade\":3,\"precoUnit\":7.25}]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].descricao").value("B")).andExpect(jsonPath("$.totais.total").value(21.75));
    }

    @Test
    void historicoDoItemNaoMudaQuandoOPrecoDoCatalogoMuda() throws Exception {
        long serv = id(mvc.perform(post("/api/servicos").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Freio\",\"precoPadrao\":200}")).andReturn().getResponse().getContentAsString());
        long os = criarOs("{\"itens\":[{\"servicoId\":" + serv + ",\"descricao\":\"Freio\",\"quantidade\":1,\"precoUnit\":200}]}");
        mvc.perform(put("/api/servicos/" + serv).cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Freio dianteiro\",\"precoPadrao\":350}")).andExpect(status().isOk());
        mvc.perform(get("/api/ordens/" + os).cookie(cookie))
                .andExpect(jsonPath("$.itens[0].descricao").value("Freio"))
                .andExpect(jsonPath("$.itens[0].precoUnit").value(200.0))
                .andExpect(jsonPath("$.totais.total").value(200.0));
    }

    @Test
    void recusaClienteVeiculoOuMecanicoInexistenteOuInativo() throws Exception {
        mvc.perform(post("/api/ordens").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{\"clienteId\":999999}"))
                .andExpect(status().isNotFound());
        long mec = id(mvc.perform(post("/api/funcionarios").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Carlos\"}")).andReturn().getResponse().getContentAsString());
        mvc.perform(patch("/api/funcionarios/" + mec + "/ativo").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content("{\"ativo\":false}")).andExpect(status().isOk());
        mvc.perform(post("/api/ordens").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{\"funcionarioId\":" + mec + "}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listaFiltraPorStatusBuscaEPeriodoEPagina() throws Exception {
        long cli = criarCliente("Maria Souza");
        long veic = id(mvc.perform(post("/api/veiculos").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content("{\"placa\":\"ABC1D23\",\"modelo\":\"Onix\"}")).andReturn().getResponse().getContentAsString());
        long a = criarOs("{\"clienteId\":" + cli + ",\"veiculoId\":" + veic + "}");
        criarOs("{}");
        criarOs("{}");
        mudar(a, "EM_EXECUCAO", 200);

        mvc.perform(get("/api/ordens").cookie(cookie)).andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].id").value(a + 2)); // mais nova primeiro
        mvc.perform(get("/api/ordens?status=EM_EXECUCAO").cookie(cookie)).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].clienteNome").value("Maria Souza"))
                .andExpect(jsonPath("$.content[0].placa").value("ABC1D23"));
        mvc.perform(get("/api/ordens?q=abc-1d23").cookie(cookie)).andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/ordens?q=souza").cookie(cookie)).andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/ordens?q=" + a).cookie(cookie)).andExpect(jsonPath("$.content[0].id").value(a));
        mvc.perform(get("/api/ordens?status=XYZ").cookie(cookie)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/ordens?de=2000-01-01&ate=2000-12-31").cookie(cookie)).andExpect(jsonPath("$.content.length()").value(0));
        mvc.perform(get("/api/ordens?de=2000-01-01").cookie(cookie)).andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/ordens?size=2&page=1").cookie(cookie)).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalPages").value(2));
    }
}
