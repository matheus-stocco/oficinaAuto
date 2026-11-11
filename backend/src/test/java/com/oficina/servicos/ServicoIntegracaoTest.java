package com.oficina.servicos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.oficina.funcionarios.FuncionarioForm;
import com.oficina.funcionarios.FuncionarioService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ServicoIntegracaoTest {
    @Autowired MockMvc mvc;
    @Autowired FuncionarioService funcionarios;
    Cookie cookie;

    @BeforeEach
    void entrar() throws Exception {
        funcionarios.criar(new FuncionarioForm("Ana", null, null, null, "ana", "senha-boa-123"));
        cookie = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"ana\",\"senha\":\"senha-boa-123\"}"))
                .andReturn().getResponse().getCookie("oficina_token");
    }

    private long criar(String nome, String preco) throws Exception {
        String r = mvc.perform(post("/api/servicos").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + nome + "\",\"precoPadrao\":" + preco + "}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return Long.parseLong(r.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    @Test
    void exigeLogin() throws Exception {
        mvc.perform(get("/api/servicos")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/servicos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"X\",\"precoPadrao\":1}")).andExpect(status().isUnauthorized());
    }

    @Test
    void criaEMantemOsCentavos() throws Exception {
        mvc.perform(post("/api/servicos").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Balanceamento\",\"precoPadrao\":80.5}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.precoPadrao").value(80.5))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void recusaPrecoNegativoAusenteOuComMuitasCasas() throws Exception {
        for (String corpo : new String[]{
                "{\"nome\":\"A\",\"precoPadrao\":-1}", "{\"nome\":\"A\"}", "{\"nome\":\"A\",\"precoPadrao\":1.234}",
                "{\"nome\":\" \",\"precoPadrao\":1}"}) {
            mvc.perform(post("/api/servicos").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erro").exists());
        }
    }

    @Test
    void nomeRepetidoSemDiferenciarMaiusculasDa409() throws Exception {
        criar("Alinhamento", "100");
        mvc.perform(post("/api/servicos").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"ALINHAMENTO\",\"precoPadrao\":90}")).andExpect(status().isConflict());
    }

    @Test
    void inativoSomeDoFiltroDeAtivosMasContinuaNaListaCompleta() throws Exception {
        long id = criar("Troca de óleo", "150");
        criar("Revisão", "300");
        mvc.perform(patch("/api/servicos/" + id + "/ativo").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content("{\"ativo\":false}")).andExpect(jsonPath("$.ativo").value(false));
        mvc.perform(get("/api/servicos?ativos=true").cookie(cookie)).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].nome").value("Revisão"));
        mvc.perform(get("/api/servicos").cookie(cookie)).andExpect(jsonPath("$.content.length()").value(2));
        mvc.perform(get("/api/servicos/" + id).cookie(cookie)).andExpect(jsonPath("$.nome").value("Troca de óleo"));
    }

    @Test
    void buscaPorNomeEPagina() throws Exception {
        for (int i = 0; i < 3; i++) criar("Serviço " + i, "10");
        criar("Pintura", "500");
        mvc.perform(get("/api/servicos?q=pint").cookie(cookie)).andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/servicos?size=2&page=1").cookie(cookie))
                .andExpect(jsonPath("$.content.length()").value(2)).andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void editaNomeEPreco() throws Exception {
        long id = criar("Freio", "200");
        mvc.perform(put("/api/servicos/" + id).cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Freio dianteiro\",\"precoPadrao\":220.9}"))
                .andExpect(jsonPath("$.nome").value("Freio dianteiro")).andExpect(jsonPath("$.precoPadrao").value(220.9));
        mvc.perform(get("/api/servicos/9999999").cookie(cookie)).andExpect(status().isNotFound());
    }
}
