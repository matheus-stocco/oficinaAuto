package com.oficina.financeiro.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class GatewayConfig {
    private static final Logger log = LoggerFactory.getLogger(GatewayConfig.class);

    /**
     * Sem ASAAS_API_KEY o gateway fica inativo e o resto do sistema funciona normalmente.
     * A URL padrão é a do SANDBOX: produção só com ASAAS_URL=https://api.asaas.com/v3 e a chave de produção.
     */
    @Bean
    GatewayPagamento gatewayPagamento(@Value("${app.asaas.url}") String url, @Value("${app.asaas.key:}") String chave) {
        boolean ativo = !chave.isBlank();
        if (ativo) avisarSeAmbienteNaoCombina(url, chave);
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(5_000);
        fabrica.setReadTimeout(15_000);
        RestClient http = RestClient.builder().requestFactory(fabrica).baseUrl(url)
                .defaultHeader("access_token", chave).defaultHeader("User-Agent", "oficinaAuto")
                .defaultHeader("Content-Type", "application/json").build();
        return new AsaasGateway(http, ativo);
    }

    /** Chave de produção começa com $aact_prod_ e a de sandbox com $aact_hmlg_ (documentação do Asaas). */
    static void avisarSeAmbienteNaoCombina(String url, String chave) {
        boolean urlSandbox = url.contains("sandbox");
        if (chave.startsWith("$aact_prod_") && urlSandbox || chave.startsWith("$aact_hmlg_") && !urlSandbox) {
            log.warn("ASAAS_API_KEY e ASAAS_URL são de ambientes diferentes (sandbox x produção): as chamadas vão falhar com 401.");
        }
    }
}
