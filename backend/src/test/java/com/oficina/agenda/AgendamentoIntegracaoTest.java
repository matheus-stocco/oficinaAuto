package com.oficina.agenda;

import static org.junit.jupiter.api.Assertions.*;
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

/** Sem @Transactional de propósito: a abertura da OS e os desfazimentos precisam de transações reais. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AgendamentoIntegracaoTest {
    @Autowired MockMvc mvc;
    @Autowired FuncionarioService funcionarios;
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
        for (String t : new String[]{"agendamento_servico", "agendamento", "os_item", "ordem_servico", "auditoria", "veiculo", "cliente", "servico", "funcionario"}) {
            jdbc.execute("delete from " + t);
        }
    }

    private long id(String json) { return Long.parseLong(json.replaceAll("^\\{\"id\":(\\d+).*", "$1")); }

    private long criar(String json) throws Exception {
        return id(mvc.perform(post("/api/agendamentos").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private long postar(String url, String json) throws Exception {
        return id(mvc.perform(post(url).cookie(cookie)
                .contentType(MediaType.APPLICATION_JSON).content(json)).andReturn().getResponse().getContentAsString());
    }

    private int contar(String tabela) { return jdbc.queryForObject("select count(*) from " + tabela, Integer.class); }

    @Test
    void exigeLogin() throws Exception {
        mvc.perform(get("/api/agendamentos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/agendamentos/calendario?de=2026-11-01&ate=2026-11-30")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/agendamentos/1/criar-os")).andExpect(status().isUnauthorized());
    }

    @Test
    void criaComClienteEVeiculoAvulsosEServicosDoCatalogoEOuTexto() throws Exception {
        long serv = postar("/api/servicos", "{\"nome\":\"Troca de óleo\",\"precoPadrao\":150}");
        long id = criar("""
                {"data":"2026-11-20","hora":"09:30","clienteNome":"João Avulso","clienteTelefone":"11977776666",
                 "veiculoPlaca":"abc-1d23","veiculoModelo":"Onix","observacoes":"Trazer o carro cedo",
                 "servicos":[{"servicoId":%d,"descricao":"Troca de óleo"},{"descricao":"Barulho na suspensão"}]}""".formatted(serv));
        mvc.perform(get("/api/agendamentos/" + id).cookie(cookie))
                .andExpect(jsonPath("$.status").value("AGENDADO")).andExpect(jsonPath("$.data").value("2026-11-20"))
                .andExpect(jsonPath("$.hora").value("09:30:00")).andExpect(jsonPath("$.clienteNome").value("João Avulso"))
                .andExpect(jsonPath("$.veiculoPlaca").value("ABC1D23")).andExpect(jsonPath("$.servicos.length()").value(2))
                .andExpect(jsonPath("$.servicos[1].descricao").value("Barulho na suspensão")).andExpect(jsonPath("$.osId").doesNotExist());
    }

    @Test
    void sohADataEObrigatoriaEAHoraEOpcional() throws Exception {
        criar("{\"data\":\"2026-11-20\"}");
        for (String json : new String[]{"{}", "{\"data\":\"1999-01-01\"}", "{\"data\":\"2101-01-01\"}", "{\"data\":\"2026-11-20\",\"veiculoPlaca\":\"XYZ\"}",
                "{\"data\":\"2026-11-20\",\"servicos\":[{\"descricao\":\" \"}]}", "{\"data\":\"2026-11-20\",\"clienteId\":99999999}"}) {
            mvc.perform(post("/api/agendamentos").cookie(cookie)
                    .contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().is4xxClientError());
        }
        assertEquals(1, contar("agendamento"));
    }

    @Test
    void clienteCadastradoSubstituiOAvulsoEMecanicoInativoERecusado() throws Exception {
        long cli = postar("/api/clientes", "{\"nome\":\"Maria Souza\",\"telefone\":\"11988887777\"}");
        long id = criar("{\"data\":\"2026-11-20\",\"clienteId\":" + cli + ",\"clienteNome\":\"Ignorado\"}");
        mvc.perform(get("/api/agendamentos/" + id).cookie(cookie)).andExpect(jsonPath("$.cliente.nome").value("Maria Souza"))
                .andExpect(jsonPath("$.clienteNome").doesNotExist());
        long mec = postar("/api/funcionarios", "{\"nome\":\"Carlos\"}");
        mvc.perform(patch("/api/funcionarios/" + mec + "/ativo").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{\"ativo\":false}"));
        mvc.perform(post("/api/agendamentos").cookie(cookie)
                .contentType(MediaType.APPLICATION_JSON).content("{\"data\":\"2026-11-20\",\"funcionarioId\":" + mec + "}")).andExpect(status().isBadRequest());
    }

    @Test
    void editaMudandoDataHoraEServicos() throws Exception {
        long id = criar("{\"data\":\"2026-11-20\",\"hora\":\"09:00\",\"servicos\":[{\"descricao\":\"A\"},{\"descricao\":\"B\"}]}");
        mvc.perform(put("/api/agendamentos/" + id).cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\":\"2026-12-02\",\"servicos\":[{\"descricao\":\"C\"}]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value("2026-12-02")).andExpect(jsonPath("$.hora").doesNotExist())
                .andExpect(jsonPath("$.servicos.length()").value(1));
        assertEquals(1, contar("agendamento_servico"));
    }

    @Test
    void mudaAsQuatroSituacoesERecusaOutra() throws Exception {
        long id = criar("{\"data\":\"2026-11-20\"}");
        for (String s : new String[]{"CANCELADO", "NAO_COMPARECEU", "REALIZADO", "AGENDADO"}) {
            mvc.perform(patch("/api/agendamentos/" + id + "/status").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\":\"" + s + "\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value(s));
        }
        mvc.perform(patch("/api/agendamentos/" + id + "/status").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"XYZ\"}")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/agendamentos/99999999").cookie(cookie)).andExpect(status().isNotFound());
    }

    // ---------- calendário: só o período visível ----------

    @Test
    void calendarioDevolveSoOPeriodoPedidoEmMesesDiferentesEOrdenaPorHora() throws Exception {
        criar("{\"data\":\"2026-10-31\",\"clienteNome\":\"Outubro\"}");
        criar("{\"data\":\"2026-11-15\",\"hora\":\"14:00\",\"clienteNome\":\"Tarde\"}");
        criar("{\"data\":\"2026-11-15\",\"clienteNome\":\"Sem hora\"}");
        criar("{\"data\":\"2026-11-15\",\"hora\":\"08:00\",\"clienteNome\":\"Manhã\"}");
        criar("{\"data\":\"2026-12-01\",\"clienteNome\":\"Dezembro\"}");

        mvc.perform(get("/api/agendamentos/calendario?de=2026-11-01&ate=2026-11-30").cookie(cookie))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].clienteNome").value("Manhã")).andExpect(jsonPath("$[1].clienteNome").value("Tarde"))
                .andExpect(jsonPath("$[2].clienteNome").value("Sem hora"));
        mvc.perform(get("/api/agendamentos/calendario?de=2026-10-25&ate=2026-12-05").cookie(cookie)).andExpect(jsonPath("$.length()").value(5));
        mvc.perform(get("/api/agendamentos/calendario?de=2026-11-15&ate=2026-11-15").cookie(cookie)).andExpect(jsonPath("$.length()").value(3));
        mvc.perform(get("/api/agendamentos/calendario?de=2027-01-01&ate=2027-01-31").cookie(cookie)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void calendarioRecusaPeriodoGrandeOuInvertidoEFaltandoDatas() throws Exception {
        mvc.perform(get("/api/agendamentos/calendario?de=2026-01-01&ate=2026-12-31").cookie(cookie)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/agendamentos/calendario?de=2026-11-30&ate=2026-11-01").cookie(cookie)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/agendamentos/calendario?de=2026-11-01").cookie(cookie)).andExpect(status().is4xxClientError());
        // 62 dias passa, 63 não
        mvc.perform(get("/api/agendamentos/calendario?de=2026-11-01&ate=2027-01-01").cookie(cookie)).andExpect(status().isOk());
        mvc.perform(get("/api/agendamentos/calendario?de=2026-11-01&ate=2027-01-02").cookie(cookie)).andExpect(status().isBadRequest());
    }

    @Test
    void calendarioMostraServicosClienteVeiculoEMecanico() throws Exception {
        long mec = postar("/api/funcionarios", "{\"nome\":\"Carlos\"}");
        criar("{\"data\":\"2026-11-15\",\"clienteNome\":\"João\",\"veiculoModelo\":\"Onix\",\"veiculoPlaca\":\"ABC1D23\",\"funcionarioId\":" + mec
                + ",\"servicos\":[{\"descricao\":\"Freio\"},{\"descricao\":\"Óleo\"}]}");
        mvc.perform(get("/api/agendamentos/calendario?de=2026-11-01&ate=2026-11-30").cookie(cookie))
                .andExpect(jsonPath("$[0].veiculo").value("Onix · ABC1D23")).andExpect(jsonPath("$[0].mecanicoNome").value("Carlos"))
                .andExpect(jsonPath("$[0].servicos[0]").value("Freio")).andExpect(jsonPath("$[0].servicos[1]").value("Óleo"));
    }

    // ---------- consulta posterior ----------

    @Test
    void consultaBuscaPorClienteVeiculoServicoFiltraPeriodoESituacaoEPagina() throws Exception {
        long cli = postar("/api/clientes", "{\"nome\":\"Maria Souza\",\"telefone\":\"11988887777\"}");
        long a = criar("{\"data\":\"2026-03-10\",\"clienteId\":" + cli + ",\"servicos\":[{\"descricao\":\"Alinhamento\"}]}");
        long b = criar("{\"data\":\"2026-05-10\",\"clienteNome\":\"João Avulso\",\"veiculoPlaca\":\"ABC1D23\",\"veiculoModelo\":\"Onix\"}");
        criar("{\"data\":\"2026-07-10\"}");
        mvc.perform(patch("/api/agendamentos/" + b + "/status").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELADO\"}"));

        mvc.perform(get("/api/agendamentos").cookie(cookie)).andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].data").value("2026-07-10")); // mais recente primeiro
        mvc.perform(get("/api/agendamentos?q=souza").cookie(cookie)).andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/agendamentos?q=avulso").cookie(cookie)).andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/agendamentos?q=abc-1d23").cookie(cookie)).andExpect(jsonPath("$.content[0].id").value(b));
        mvc.perform(get("/api/agendamentos?q=alinha").cookie(cookie)).andExpect(jsonPath("$.content[0].id").value(a));
        mvc.perform(get("/api/agendamentos?status=CANCELADO").cookie(cookie)).andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/agendamentos?de=2026-04-01&ate=2026-06-30").cookie(cookie)).andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/agendamentos?de=2026-06-01").cookie(cookie)).andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/agendamentos?status=XYZ").cookie(cookie)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/agendamentos?size=2&page=1").cookie(cookie)).andExpect(jsonPath("$.content.length()").value(1)).andExpect(jsonPath("$.totalPages").value(2));
    }

    // ---------- abrir OS a partir do agendamento ----------

    @Test
    void abrirOsCopiaClienteVeiculoMecanicoEServicosMarcaRealizadoEGuardaONumero() throws Exception {
        long cli = postar("/api/clientes", "{\"nome\":\"Maria Souza\",\"telefone\":\"11988887777\"}");
        long veic = postar("/api/veiculos", "{\"placa\":\"DEF5678\",\"modelo\":\"Uno\"}");
        long mec = postar("/api/funcionarios", "{\"nome\":\"Carlos\"}");
        long serv = postar("/api/servicos", "{\"nome\":\"Troca de óleo\",\"precoPadrao\":150.50}");
        long id = criar("""
                {"data":"2026-11-20","clienteId":%d,"veiculoId":%d,"funcionarioId":%d,"observacoes":"Cliente com pressa",
                 "servicos":[{"servicoId":%d,"descricao":"Troca de óleo"},{"descricao":"Barulho na suspensão"}]}""".formatted(cli, veic, mec, serv));

        var os = mvc.perform(post("/api/agendamentos/" + id + "/criar-os").cookie(cookie))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ABERTA"))
                .andExpect(jsonPath("$.cliente.nome").value("Maria Souza")).andExpect(jsonPath("$.veiculo.placa").value("DEF5678"))
                .andExpect(jsonPath("$.mecanico.nome").value("Carlos")).andExpect(jsonPath("$.observacoes").value("Cliente com pressa"))
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.itens[0].precoUnit").value(150.5)).andExpect(jsonPath("$.itens[0].quantidade").value(1))
                .andExpect(jsonPath("$.itens[1].descricao").value("Barulho na suspensão")).andExpect(jsonPath("$.itens[1].precoUnit").value(0))
                .andExpect(jsonPath("$.totais.total").value(150.5)).andReturn().getResponse().getContentAsString();
        mvc.perform(get("/api/agendamentos/" + id).cookie(cookie)).andExpect(jsonPath("$.status").value("REALIZADO"))
                .andExpect(jsonPath("$.osId").value(id(os)));
        // uma OS só por agendamento
        mvc.perform(post("/api/agendamentos/" + id + "/criar-os").cookie(cookie))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.erro").value(org.hamcrest.Matchers.containsString("Já existe a OS")));
        assertEquals(1, contar("ordem_servico"));
    }

    @Test
    void abrirOsCadastraClienteEVeiculoAvulsosQuandoPedidoEPodePular() throws Exception {
        long a = criar("{\"data\":\"2026-11-20\",\"clienteNome\":\"João Avulso\",\"clienteTelefone\":\"11977776666\",\"veiculoPlaca\":\"ABC1D23\",\"veiculoModelo\":\"Onix\"}");
        mvc.perform(post("/api/agendamentos/" + a + "/criar-os").cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cliente\":{\"nome\":\"João Avulso\",\"telefone\":\"11977776666\"},\"veiculo\":{\"placa\":\"ABC1D23\",\"modelo\":\"Onix\"}}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cliente.nome").value("João Avulso")).andExpect(jsonPath("$.veiculo.placa").value("ABC1D23"));
        assertEquals(1, contar("cliente"));
        assertEquals(1, jdbc.queryForObject("select count(*) from veiculo where cliente_id is not null", Integer.class));

        long b = criar("{\"data\":\"2026-11-21\",\"clienteNome\":\"Outro\"}");
        mvc.perform(post("/api/agendamentos/" + b + "/criar-os").cookie(cookie))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cliente").doesNotExist());
        assertEquals(1, contar("cliente"), "sem pedir, nada é cadastrado");
    }

    @Test
    void falhaAoAbrirOsNaoMudaOAgendamentoNemDeixaSobras() throws Exception {
        postar("/api/veiculos", "{\"placa\":\"ABC1D23\",\"modelo\":\"Outro\"}");
        long id = criar("{\"data\":\"2026-11-20\",\"clienteNome\":\"João Avulso\",\"clienteTelefone\":\"11977776666\",\"veiculoPlaca\":\"ABC1D23\",\"veiculoModelo\":\"Onix\"}");
        int clientes = contar("cliente");
        mvc.perform(post("/api/agendamentos/" + id + "/criar-os").cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cliente\":{\"nome\":\"João Avulso\",\"telefone\":\"11977776666\"},\"veiculo\":{\"placa\":\"ABC1D23\",\"modelo\":\"Onix\"}}"))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/agendamentos/" + id).cookie(cookie)).andExpect(jsonPath("$.status").value("AGENDADO")).andExpect(jsonPath("$.osId").doesNotExist());
        assertEquals(0, contar("ordem_servico"));
        assertEquals(clientes, contar("cliente"));
    }

    @Test
    void agendamentoCanceladoNaoGeraOsMasVoltandoParaAgendadoGera() throws Exception {
        long id = criar("{\"data\":\"2026-11-20\"}");
        mvc.perform(patch("/api/agendamentos/" + id + "/status").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELADO\"}"));
        mvc.perform(post("/api/agendamentos/" + id + "/criar-os").cookie(cookie)).andExpect(status().isConflict());
        mvc.perform(patch("/api/agendamentos/" + id + "/status").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"AGENDADO\"}"));
        mvc.perform(post("/api/agendamentos/" + id + "/criar-os").cookie(cookie)).andExpect(status().isOk());
    }

    @Test
    void mecanicoInativadoDepoisDoAgendamentoNaoImpedeAOs() throws Exception {
        long mec = postar("/api/funcionarios", "{\"nome\":\"Carlos\"}");
        long id = criar("{\"data\":\"2026-11-20\",\"funcionarioId\":" + mec + "}");
        mvc.perform(patch("/api/funcionarios/" + mec + "/ativo").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{\"ativo\":false}"));
        mvc.perform(post("/api/agendamentos/" + id + "/criar-os").cookie(cookie))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mecanico").doesNotExist());
    }
}
