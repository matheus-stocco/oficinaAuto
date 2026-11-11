package com.oficina.clientes;

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
class ClientesVeiculosIntegracaoTest {
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

    private long criarCliente(String nome, String telefone) throws Exception {
        String r = mvc.perform(post("/api/clientes").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + nome + "\",\"telefone\":\"" + telefone + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return Long.parseLong(r.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    private void criarVeiculo(String placa, String modelo, Long clienteId) throws Exception {
        mvc.perform(post("/api/veiculos").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content("{\"placa\":\"" + placa + "\",\"modelo\":\"" + modelo + "\""
                        + (clienteId == null ? "" : ",\"clienteId\":" + clienteId) + "}"))
                .andExpect(status().isOk());
    }

    @Test
    void rotasExigemLogin() throws Exception {
        mvc.perform(get("/api/clientes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/veiculos")).andExpect(status().isUnauthorized());
    }

    @Test
    void clienteSoExigeNomeETelefone() throws Exception {
        mvc.perform(post("/api/clientes").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Só nome\"}")).andExpect(status().isBadRequest());
        criarCliente("Maria", "11988887777");
    }

    @Test
    void buscaPorTelefoneIgnoraAMascaraEPorNome() throws Exception {
        criarCliente("Maria Souza", "(11) 98888-7777");
        criarCliente("João Lima", "21 97777-1111");
        mvc.perform(get("/api/clientes?q=988887777").cookie(cookie))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].nome").value("Maria Souza"));
        mvc.perform(get("/api/clientes?q=(11) 98888").cookie(cookie))
                .andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/clientes?q=lima").cookie(cookie))
                .andExpect(jsonPath("$.content[0].nome").value("João Lima"));
        // texto sem dígitos não pode casar todos os telefones
        mvc.perform(get("/api/clientes?q=zzz").cookie(cookie)).andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void listaEPaginada() throws Exception {
        for (int i = 0; i < 3; i++) criarCliente("Cliente " + i, "1190000000" + i);
        mvc.perform(get("/api/clientes?size=2&page=0").cookie(cookie))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void placaDuplicadaEInvalidaSaoRecusadasEVeiculoSemPlacaPode() throws Exception {
        criarVeiculo("abc-1234", "Uno", null);
        mvc.perform(post("/api/veiculos").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content("{\"placa\":\"ABC1234\",\"modelo\":\"Gol\"}")).andExpect(status().isConflict());
        mvc.perform(post("/api/veiculos").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content("{\"placa\":\"XYZ\",\"modelo\":\"Gol\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/veiculos").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content("{\"modelo\":\"Fusca\"}")).andExpect(status().isOk());
        mvc.perform(post("/api/veiculos").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content("{\"modelo\":\"Brasília\"}")).andExpect(status().isOk());
    }

    @Test
    void buscaVeiculoPorPlacaComOuSemHifenModeloENomeDoCliente() throws Exception {
        long maria = criarCliente("Maria Souza", "11988887777");
        criarVeiculo("ABC1D23", "Onix", maria);
        criarVeiculo("DEF-5678", "Corsa", null);
        mvc.perform(get("/api/veiculos?q=abc-1d23").cookie(cookie))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].clienteNome").value("Maria Souza"));
        mvc.perform(get("/api/veiculos?q=corsa").cookie(cookie)).andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/veiculos?q=souza").cookie(cookie)).andExpect(jsonPath("$.content[0].modelo").value("Onix"));
        mvc.perform(get("/api/veiculos").cookie(cookie)).andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void veiculosDoClienteEExportacaoLgpd() throws Exception {
        long maria = criarCliente("Maria Souza", "11988887777");
        criarVeiculo("ABC1234", "Onix", maria);
        mvc.perform(get("/api/clientes/" + maria + "/veiculos").cookie(cookie))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/clientes/" + maria + "/exportar").cookie(cookie))
                .andExpect(jsonPath("$.cliente.nome").value("Maria Souza"))
                .andExpect(jsonPath("$.veiculos[0].placa").value("ABC1234"));
    }

    @Test
    void anonimizarApagaDadosMantemVeiculoEBloqueiaEdicao() throws Exception {
        long maria = criarCliente("Maria Souza", "11988887777");
        criarVeiculo("ABC1234", "Onix", maria);
        mvc.perform(post("/api/clientes/" + maria + "/anonimizar").cookie(cookie))
                .andExpect(status().isOk()).andExpect(jsonPath("$.anonimizado").value(true))
                .andExpect(jsonPath("$.nome").value("Cliente anonimizado"))
                .andExpect(jsonPath("$.telefone").doesNotExist());
        mvc.perform(get("/api/clientes?q=988887777").cookie(cookie)).andExpect(jsonPath("$.content.length()").value(0));
        mvc.perform(get("/api/clientes/" + maria + "/veiculos").cookie(cookie)).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(put("/api/clientes/" + maria).cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Outro\",\"telefone\":\"1\"}")).andExpect(status().isConflict());
    }
}
