package com.oficina.empresa;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EmpresaIntegracaoTest {
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};

    @Autowired MockMvc mvc;
    @Autowired FuncionarioService funcionarios;

    @BeforeEach
    void dados() {
        funcionarios.criar(new FuncionarioForm("Ana", null, null, null, "ana", "senha-boa-123"));
    }

    private Cookie entrar() throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"ana\",\"senha\":\"senha-boa-123\"}"))
                .andReturn().getResponse().getCookie("oficina_token");
    }

    @Test
    void empresaCompletaExigeLoginMasIdentidadeEPublica() throws Exception {
        mvc.perform(get("/api/empresa")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/publico/identidade")).andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").exists())
                .andExpect(jsonPath("$.cnpj").doesNotExist())
                .andExpect(jsonPath("$.telefone").doesNotExist());
    }

    @Test
    void atualizaDadosENormalizaCnpj() throws Exception {
        mvc.perform(put("/api/empresa").cookie(entrar()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Auto Center Silva\",\"cnpj\":\"12.345.678/0001-90\",\"telefone\":\"(11) 4000-0000\","
                                + "\"fundoTipo\":\"COR\",\"fundoCor\":\"#112233\",\"orcamentoRascunhoHoras\":12}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cnpj").value("12345678000190"))
                .andExpect(jsonPath("$.orcamentoRascunhoHoras").value(12));
    }

    @Test
    void recusaDadosInvalidos() throws Exception {
        Cookie c = entrar();
        mvc.perform(put("/api/empresa").cookie(c).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"X\",\"cnpj\":\"123\",\"orcamentoRascunhoHoras\":24}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/empresa").cookie(c).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"X\",\"orcamentoRascunhoHoras\":999}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/empresa").cookie(c).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"X\",\"fundoTipo\":\"COR\",\"fundoCor\":\"vermelho\",\"orcamentoRascunhoHoras\":24}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void enviaLogoPngEAServePublicamente() throws Exception {
        Cookie c = entrar();
        mvc.perform(multipart("/api/empresa/logo").file(new MockMultipartFile("arquivo", "l.png", "image/png", PNG)).cookie(c))
                .andExpect(status().isOk()).andExpect(jsonPath("$.temLogo").value(true));
        mvc.perform(get("/api/publico/logo")).andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        mvc.perform(delete("/api/empresa/logo").cookie(c)).andExpect(jsonPath("$.temLogo").value(false));
        mvc.perform(get("/api/publico/logo")).andExpect(status().isNotFound());
    }

    @Test
    void recusaSvgEUploadSemLogin() throws Exception {
        Cookie c = entrar();
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>".getBytes();
        mvc.perform(multipart("/api/empresa/logo").file(new MockMultipartFile("arquivo", "l.svg", "image/svg+xml", svg)).cookie(c))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/empresa/logo").file(new MockMultipartFile("arquivo", "l.png", "image/png", PNG)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void fundoEmImagemExigeArquivoEnviado() throws Exception {
        mvc.perform(put("/api/empresa").cookie(entrar()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"X\",\"fundoTipo\":\"IMAGEM\",\"orcamentoRascunhoHoras\":24}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logoCujoArquivoSumiuDoDiscoNaoContaComoLogo() throws Exception {
        Cookie c = entrar();
        mvc.perform(multipart("/api/empresa/logo").file(new MockMultipartFile("arquivo", "l.png", "image/png", PNG)).cookie(c))
                .andExpect(jsonPath("$.temLogo").value(true));
        // simula o volume de uploads não montado: o banco aponta para o arquivo, mas ele não está no disco
        java.nio.file.Files.deleteIfExists(java.nio.file.Path.of("target/uploads-test/logo.png"));
        mvc.perform(get("/api/empresa").cookie(c)).andExpect(jsonPath("$.temLogo").value(false));
        mvc.perform(get("/api/publico/identidade")).andExpect(jsonPath("$.temLogo").value(false));
        mvc.perform(get("/api/publico/logo")).andExpect(status().isNotFound());
    }
}
