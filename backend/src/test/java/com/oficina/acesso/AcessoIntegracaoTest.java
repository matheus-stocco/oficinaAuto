package com.oficina.acesso;

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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AcessoIntegracaoTest {
    @Autowired MockMvc mvc;
    @Autowired FuncionarioService funcionarios;

    @BeforeEach
    void dados() {
        funcionarios.criar(new FuncionarioForm("Ana Atendente", "11999990000", null, null, "ana", "senha-boa-123"));
    }

    private String login(String login, String senha) {
        return "{\"login\":\"" + login + "\",\"senha\":\"" + senha + "\"}";
    }

    private Cookie entrar() throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(login("ana", "senha-boa-123"))).andExpect(status().isOk()).andReturn();
        return r.getResponse().getCookie("oficina_token");
    }

    @Test
    void saudeEPublica() throws Exception {
        mvc.perform(get("/api/saude")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void rotaProtegidaSemLoginDevolve401() throws Exception {
        mvc.perform(get("/api/funcionarios")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/eu")).andExpect(status().isNoContent());
    }

    @Test
    void loginCertoGravaCookieHttpOnlyStrict() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("ana", "senha-boa-123")))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly("oficina_token", true))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("SameSite=Strict")))
                .andExpect(jsonPath("$.nome").value("Ana Atendente"));
    }

    @Test
    void loginErradoDevolve401SemCookie() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("ana", "errada")))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().doesNotExist("oficina_token"));
    }

    @Test
    void cookieValidoAbreRotaProtegidaETokenInvalidoNao() throws Exception {
        mvc.perform(get("/api/funcionarios").cookie(entrar())).andExpect(status().isOk());
        mvc.perform(get("/api/funcionarios").cookie(new Cookie("oficina_token", "lixo")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cincoErrosBloqueiamMesmoComSenhaCerta() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(login("ana", "errada"))).andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("ana", "senha-boa-123")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void trocarSenhaExigeSenhaAtualENovaValida() throws Exception {
        Cookie c = entrar();
        mvc.perform(post("/api/auth/trocar-senha").cookie(c).contentType(MediaType.APPLICATION_JSON)
                .content("{\"senhaAtual\":\"errada\",\"novaSenha\":\"nova-senha-123\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/trocar-senha").cookie(c).contentType(MediaType.APPLICATION_JSON)
                .content("{\"senhaAtual\":\"senha-boa-123\",\"novaSenha\":\"curta\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/trocar-senha").cookie(c).contentType(MediaType.APPLICATION_JSON)
                .content("{\"senhaAtual\":\"senha-boa-123\",\"novaSenha\":\"nova-senha-123\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(login("ana", "nova-senha-123"))).andExpect(status().isOk());
    }

    @Test
    void euComSessaoDevolveOUsuario() throws Exception {
        mvc.perform(get("/api/auth/eu").cookie(entrar())).andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("ana"));
    }

    @Test
    void logoutLimpaOCookie() throws Exception {
        mvc.perform(post("/api/auth/logout")).andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("oficina_token", 0));
    }

    @Test
    void naoInativaOUltimoFuncionarioComAcesso() throws Exception {
        Cookie c = entrar();
        Long id = idDaAna();
        mvc.perform(patch("/api/funcionarios/" + id + "/ativo").cookie(c)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ativo\":false}"))
                .andExpect(status().isConflict());
    }

    @Autowired com.oficina.funcionarios.FuncionarioRepository repo;

    private Long idDaAna() {
        return repo.buscarPorLogin("ana").orElseThrow().getId();
    }

    @Test
    void criaMecanicoSemAcessoELista() throws Exception {
        Cookie c = entrar();
        mvc.perform(post("/api/funcionarios").cookie(c).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Carlos Mecanico\",\"telefone\":\"11988887777\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.temAcesso").value(false))
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
        mvc.perform(get("/api/funcionarios?q=carlos&size=5").cookie(c))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    void nomeEmBrancoDevolve400() throws Exception {
        mvc.perform(post("/api/funcionarios").cookie(entrar()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\" \"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erro").exists());
    }
}
