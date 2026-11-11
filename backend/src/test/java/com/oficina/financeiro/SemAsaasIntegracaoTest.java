package com.oficina.financeiro;

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

/** Sem chave do Asaas e sem token de webhook (a configuração padrão): o sistema funciona e a cobrança online fica desligada. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SemAsaasIntegracaoTest {
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
        for (String t : new String[]{"cobranca", "receber", "pagamento", "os_item", "ordem_servico", "auditoria", "cliente", "funcionario"}) jdbc.execute("delete from " + t);
    }

    private long osComCliente() throws Exception {
        String cli = mvc.perform(post("/api/clientes").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Maria\",\"telefone\":\"11988887777\",\"documento\":\"12345678909\"}")).andReturn().getResponse().getContentAsString();
        long cid = Long.parseLong(cli.replaceAll("^\\{\"id\":(\\d+).*", "$1"));
        String os = mvc.perform(post("/api/ordens").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content("{\"clienteId\":" + cid + ",\"itens\":[{\"descricao\":\"S\",\"quantidade\":1,\"precoUnit\":100}]}")).andReturn().getResponse().getContentAsString();
        return Long.parseLong(os.replaceAll("^\\{\"id\":(\\d+).*", "$1"));
    }

    @Test
    void semChaveACobrancaOnlineFicaDesligadaEOFinanceiroManualContinua() throws Exception {
        long os = osComCliente();
        mvc.perform(get("/api/ordens/" + os + "/cobrancas").cookie(cookie)).andExpect(jsonPath("$.online").value(false));
        mvc.perform(post("/api/ordens/" + os + "/cobrancas").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{\"tipo\":\"PIX\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.erro").value("A cobrança online não está configurada."));
        // o financeiro manual funciona normalmente
        mvc.perform(post("/api/ordens/" + os + "/pagamentos").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{\"valor\":100,\"forma\":\"PIX\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.saldo").value(0.0));
    }

    @Test
    void semTokenConfiguradoOWebhookRecusaTudoInclusiveCabecalhoVazio() throws Exception {
        String corpo = "{\"event\":\"PAYMENT_RECEIVED\",\"payment\":{\"id\":\"pay_1\",\"billingType\":\"PIX\"}}";
        mvc.perform(post("/api/webhooks/asaas").contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/webhooks/asaas").header("asaas-access-token", "").contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/webhooks/asaas").header("asaas-access-token", "qualquer").contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isUnauthorized());
    }
}
