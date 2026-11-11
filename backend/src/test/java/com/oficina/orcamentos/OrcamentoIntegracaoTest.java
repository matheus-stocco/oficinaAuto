package com.oficina.orcamentos;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.oficina.funcionarios.FuncionarioForm;
import com.oficina.funcionarios.FuncionarioService;
import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Sem @Transactional de propósito: a conversão em OS e a limpeza precisam de transações reais. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrcamentoIntegracaoTest {
    static final String COMPLETO = """
            {"clienteNome":"João Avulso","clienteTelefone":"11977776666","veiculoPlaca":"abc-1d23","veiculoModelo":"Onix",
             "observacoes":"Cliente quer rápido",
             "itens":[{"descricao":"Balanceamento","quantidade":2,"precoUnit":80.50},{"descricao":"Alinhamento","quantidade":1,"precoUnit":120}],
             "maoDeObra":100,"pecasValor":200,"pecasDescricao":"2 pneus",
             "taxaCompraTipo":"PERCENTUAL","taxaCompra":10,"descontoTipo":"PERCENTUAL","desconto":5}""";

    @Autowired MockMvc mvc;
    @Autowired FuncionarioService funcionarios;
    @Autowired OrcamentoService servico;
    @Autowired JdbcTemplate jdbc;
    Cookie cookie;

    @BeforeEach
    void entrar() throws Exception {
        limpar();
        funcionarios.criar(new FuncionarioForm("Ana", null, null, null, "ana", "senha-boa-123"));
        cookie = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"ana\",\"senha\":\"senha-boa-123\"}")).andReturn().getResponse().getCookie("oficina_token");
    }

    @AfterEach
    void limpar() {
        for (String t : new String[]{"os_item", "ordem_servico", "orcamento_item", "orcamento", "auditoria", "veiculo", "cliente", "servico", "funcionario"}) {
            jdbc.execute("delete from " + t);
        }
        jdbc.execute("update empresa set cnpj = null, logo_arquivo = null, endereco = null, telefone = null, orcamento_rascunho_horas = 24");
    }

    private long id(String json) { return Long.parseLong(json.replaceAll("^\\{\"id\":(\\d+).*", "$1")); }

    private long criar(String json) throws Exception {
        return id(mvc.perform(post("/api/orcamentos").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private long criarCliente(String nome) throws Exception {
        return id(mvc.perform(post("/api/clientes").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"" + nome + "\",\"telefone\":\"11988887777\"}")).andReturn().getResponse().getContentAsString());
    }

    private long criarVeiculo(String placa) throws Exception {
        return id(mvc.perform(post("/api/veiculos").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content("{\"placa\":\"" + placa + "\",\"modelo\":\"Uno\"}")).andReturn().getResponse().getContentAsString());
    }

    private int contar(String tabela) { return jdbc.queryForObject("select count(*) from " + tabela, Integer.class); }

    private byte[] pngReal() throws Exception {
        var img = new java.awt.image.BufferedImage(40, 20, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var s = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", s);
        return s.toByteArray();
    }

    private void configurarEmpresa() throws Exception {
        mvc.perform(put("/api/empresa").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Auto Center Silva\",\"cnpj\":\"12.345.678/0001-90\",\"orcamentoRascunhoHoras\":24}")).andExpect(status().isOk());
        mvc.perform(multipart("/api/empresa/logo").file(new MockMultipartFile("arquivo", "l.png", "image/png", pngReal())).cookie(cookie))
                .andExpect(status().isOk());
    }

    @Test
    void exigeLogin() throws Exception {
        mvc.perform(get("/api/orcamentos")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/orcamentos/1/criar-os")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/orcamentos/1/pdf")).andExpect(status().isUnauthorized());
    }

    @Test
    void criaComClienteEVeiculoAvulsosCalculaTotaisESemPrazoAteGerar() throws Exception {
        long id = criar(COMPLETO);
        mvc.perform(get("/api/orcamentos/" + id).cookie(cookie))
                .andExpect(jsonPath("$.clienteNome").value("João Avulso")).andExpect(jsonPath("$.veiculoPlaca").value("ABC1D23"))
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.totais.total").value(570.95)).andExpect(jsonPath("$.totais.receitaOficina").value(370.95))
                .andExpect(jsonPath("$.geradoEm").doesNotExist()).andExpect(jsonPath("$.expiraEm").doesNotExist());
    }

    @Test
    void clienteCadastradoSubstituiOAvulso() throws Exception {
        long cli = criarCliente("Maria Souza");
        long id = criar("{\"clienteId\":" + cli + ",\"clienteNome\":\"Ignorado\",\"clienteTelefone\":\"1\"}");
        mvc.perform(get("/api/orcamentos/" + id).cookie(cookie)).andExpect(jsonPath("$.cliente.nome").value("Maria Souza"))
                .andExpect(jsonPath("$.clienteNome").doesNotExist()).andExpect(jsonPath("$.clienteTelefone").doesNotExist());
    }

    @Test
    void recusaDadosInvalidos() throws Exception {
        for (String json : new String[]{"{\"veiculoPlaca\":\"XYZ\"}", "{\"maoDeObra\":-1}",
                "{\"itens\":[{\"descricao\":\"X\",\"quantidade\":0,\"precoUnit\":1}]}",
                "{\"itens\":[{\"descricao\":\"X\",\"quantidade\":1,\"precoUnit\":100}],\"desconto\":100.01}", "{\"clienteId\":99999999}"}) {
            mvc.perform(post("/api/orcamentos").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().is4xxClientError());
        }
        assertEquals(0, contar("orcamento"));
    }

    @Test
    void editaERecalcula() throws Exception {
        long id = criar(COMPLETO);
        mvc.perform(put("/api/orcamentos/" + id).cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itens\":[{\"descricao\":\"Só um\",\"quantidade\":3,\"precoUnit\":7.25}]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens.length()").value(1)).andExpect(jsonPath("$.totais.total").value(21.75));
        assertEquals(1, contar("orcamento_item"));
    }

    @Test
    void listaBuscaPorClienteAvulsoOuCadastradoPlacaEPagina() throws Exception {
        long cli = criarCliente("Maria Souza");
        criar(COMPLETO);
        criar("{\"clienteId\":" + cli + "}");
        criar("{}");
        mvc.perform(get("/api/orcamentos").cookie(cookie)).andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/orcamentos?q=avulso").cookie(cookie)).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].clienteNome").value("João Avulso"));
        mvc.perform(get("/api/orcamentos?q=souza").cookie(cookie)).andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/orcamentos?q=abc-1d23").cookie(cookie)).andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/orcamentos?size=2&page=1").cookie(cookie)).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void excluirManualmenteERecusaIdInexistente() throws Exception {
        long id = criar("{}");
        mvc.perform(delete("/api/orcamentos/" + id).cookie(cookie)).andExpect(status().isNoContent());
        mvc.perform(get("/api/orcamentos/" + id).cookie(cookie)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/orcamentos/" + id).cookie(cookie)).andExpect(status().isNotFound());
    }

    // ---------- PDF e prazo ----------

    @Test
    void pdfSemLogoECnpjDa409ENaoComecaOPrazo() throws Exception {
        long id = criar(COMPLETO);
        mvc.perform(get("/api/orcamentos/" + id + "/pdf").cookie(cookie)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value(org.hamcrest.Matchers.containsString("CNPJ")));
        assertNull(jdbc.queryForObject("select gerado_em from orcamento where id = ?", java.sql.Timestamp.class, id));
    }

    @Test
    void pdfAbreNoNavegadorTemOsDadosEComecaOPrazoUmaSoVez() throws Exception {
        configurarEmpresa();
        long id = criar(COMPLETO);
        var r = mvc.perform(get("/api/orcamentos/" + id + "/pdf").cookie(cookie)).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("inline")))
                .andReturn().getResponse();
        byte[] bytes = r.getContentAsByteArray();
        assertEquals("%PDF", new String(bytes, 0, 4));
        String texto = new com.lowagie.text.pdf.parser.PdfTextExtractor(new com.lowagie.text.pdf.PdfReader(bytes)).getTextFromPage(1);
        for (String esperado : new String[]{"Auto Center Silva", "Orçamento nº " + id, "João Avulso", "Onix", "ABC1D23",
                "Balanceamento", "2 pneus", "TOTAL: R$ 570,95"}) {
            assertTrue(texto.contains(esperado), "faltou: " + esperado + "\n" + texto);
        }
        var primeiro = jdbc.queryForObject("select gerado_em from orcamento where id = ?", java.sql.Timestamp.class, id);
        assertNotNull(primeiro);
        mvc.perform(get("/api/orcamentos/" + id + "/pdf").cookie(cookie)).andExpect(status().isOk());
        assertEquals(primeiro, jdbc.queryForObject("select gerado_em from orcamento where id = ?", java.sql.Timestamp.class, id));
        mvc.perform(get("/api/orcamentos/" + id).cookie(cookie)).andExpect(jsonPath("$.geradoEm").exists()).andExpect(jsonPath("$.expiraEm").exists());
    }

    @Test
    void limpezaApagaSoOsVencidosEDeixaOsDentroDoPrazo() throws Exception {
        long vencido = criar("{\"itens\":[{\"descricao\":\"A\",\"quantidade\":1,\"precoUnit\":1}]}");
        long dentro = criar("{}");
        long esquecido = criar("{}");
        long recente = criar("{}");
        jdbc.update("update orcamento set gerado_em = now() - interval '25 hours' where id = ?", vencido);   // prazo de 24 h passou
        jdbc.update("update orcamento set gerado_em = now() - interval '23 hours' where id = ?", dentro);    // ainda vale
        jdbc.update("update orcamento set criado_em = now() - interval '8 days' where id = ?", esquecido);   // nunca gerado, > 7 dias
        jdbc.update("update orcamento set criado_em = now() - interval '6 days' where id = ?", recente);     // nunca gerado, < 7 dias

        assertEquals(2, servico.limparExpirados(LocalDateTime.now()));
        mvc.perform(get("/api/orcamentos/" + vencido).cookie(cookie)).andExpect(status().isNotFound());
        mvc.perform(get("/api/orcamentos/" + esquecido).cookie(cookie)).andExpect(status().isNotFound());
        mvc.perform(get("/api/orcamentos/" + dentro).cookie(cookie)).andExpect(status().isOk());
        mvc.perform(get("/api/orcamentos/" + recente).cookie(cookie)).andExpect(status().isOk());
        assertEquals(0, contar("orcamento_item"), "os itens do orçamento apagado saem junto");
    }

    @Test
    void limpezaRespeitaOPrazoConfiguradoNasConfiguracoes() throws Exception {
        long id = criar("{}");
        jdbc.update("update orcamento set gerado_em = now() - interval '3 hours' where id = ?", id);
        assertEquals(0, servico.limparExpirados(LocalDateTime.now()));
        jdbc.update("update empresa set orcamento_rascunho_horas = 2");
        assertEquals(1, servico.limparExpirados(LocalDateTime.now()));
    }

    // ---------- criar OS a partir do orçamento ----------

    @Test
    void criarOsCopiaItensEValoresEApagaOOrcamento() throws Exception {
        long cli = criarCliente("Maria Souza");
        long veic = criarVeiculo("DEF5678");
        long id = criar("{\"clienteId\":" + cli + ",\"veiculoId\":" + veic + ",\"observacoes\":\"obs\","
                + COMPLETO.substring(COMPLETO.indexOf("\"itens\"")));
        mvc.perform(post("/api/orcamentos/" + id + "/criar-os").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ABERTA"))
                .andExpect(jsonPath("$.cliente.nome").value("Maria Souza")).andExpect(jsonPath("$.veiculo.placa").value("DEF5678"))
                .andExpect(jsonPath("$.itens.length()").value(2)).andExpect(jsonPath("$.totais.total").value(570.95))
                .andExpect(jsonPath("$.totais.receitaOficina").value(370.95)).andExpect(jsonPath("$.valores.pecasDescricao").value("2 pneus"))
                .andExpect(jsonPath("$.observacoes").value("obs"));
        mvc.perform(get("/api/orcamentos/" + id).cookie(cookie)).andExpect(status().isNotFound());
        assertEquals(1, contar("ordem_servico"));
    }

    @Test
    void criarOsCadastraClienteEVeiculoAvulsosQuandoPedido() throws Exception {
        long id = criar(COMPLETO);
        mvc.perform(post("/api/orcamentos/" + id + "/criar-os").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cliente\":{\"nome\":\"João Avulso\",\"telefone\":\"11977776666\"},\"veiculo\":{\"placa\":\"ABC1D23\",\"modelo\":\"Onix\"}}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cliente.nome").value("João Avulso"))
                .andExpect(jsonPath("$.veiculo.placa").value("ABC1D23"));
        assertEquals(1, contar("cliente"));
        assertEquals(1, contar("veiculo"));
        assertEquals(1, jdbc.queryForObject("select count(*) from veiculo where cliente_id is not null", Integer.class), "o veículo novo fica ligado ao cliente novo");
        assertEquals(0, contar("orcamento"));
    }

    @Test
    void criarOsPulandoOCadastroDeixaAOsSemClienteESemVeiculo() throws Exception {
        long id = criar(COMPLETO);
        mvc.perform(post("/api/orcamentos/" + id + "/criar-os").cookie(cookie)).andExpect(status().isOk())
                .andExpect(jsonPath("$.cliente").doesNotExist()).andExpect(jsonPath("$.veiculo").doesNotExist())
                .andExpect(jsonPath("$.totais.total").value(570.95));
        assertEquals(0, contar("cliente"));
        assertEquals(0, contar("orcamento"));
    }

    @Test
    void falhaNaConversaoNaoApagaOOrcamentoNemDeixaSobras() throws Exception {
        criarVeiculo("ABC1D23");                       // já existe: o veículo novo vai falhar por placa repetida
        long id = criar(COMPLETO);
        int clientesAntes = contar("cliente");
        mvc.perform(post("/api/orcamentos/" + id + "/criar-os").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cliente\":{\"nome\":\"João Avulso\",\"telefone\":\"11977776666\"},\"veiculo\":{\"placa\":\"ABC1D23\",\"modelo\":\"Onix\"}}"))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/orcamentos/" + id).cookie(cookie)).andExpect(status().isOk());   // continua lá
        assertEquals(0, contar("ordem_servico"), "nenhuma OS criada");
        assertEquals(clientesAntes, contar("cliente"), "o cliente criado no meio do caminho foi desfeito");
    }

    @Test
    void naoConverteOrcamentoQueJaExpirouOuNaoExiste() throws Exception {
        long id = criar("{}");
        jdbc.update("update orcamento set gerado_em = now() - interval '30 hours' where id = ?", id);
        servico.limparExpirados(LocalDateTime.now());
        mvc.perform(post("/api/orcamentos/" + id + "/criar-os").cookie(cookie)).andExpect(status().isNotFound());
        assertEquals(0, contar("ordem_servico"));
    }

    @Test
    void conversaoComDadosNovosInvalidosEDevolve400() throws Exception {
        long id = criar(COMPLETO);
        mvc.perform(post("/api/orcamentos/" + id + "/criar-os").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content("{\"cliente\":{\"nome\":\"\",\"telefone\":\"1\"}}")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/orcamentos/" + id).cookie(cookie)).andExpect(status().isOk());
    }
}
