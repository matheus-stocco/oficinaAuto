package com.oficina.financeiro.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Asaas, API v3 (campos conferidos na documentação oficial em 2026-11-11; ver docs/asaas.md).
 * Autenticação: header `access_token` (+ `User-Agent`). Sandbox: https://api-sandbox.asaas.com/v3.
 * O RestClient já vem com a URL base e os headers; este código não guarda nem imprime a chave.
 */
public class AsaasGateway implements GatewayPagamento {
    private final RestClient http;
    private final boolean ativo;

    public AsaasGateway(RestClient http, boolean ativo) {
        this.http = http;
        this.ativo = ativo;
    }

    @Override
    public boolean ativo() {
        return ativo;
    }

    @Override
    public CobrancaCriada criarCobranca(NovaCobranca n) {
        return chamar(() -> {
            String cliente = n.clienteIdExterno();
            if (cliente == null) {
                ObjectNode c = JsonNodeFactory.instance.objectNode();
                c.put("name", n.clienteNome());
                c.put("cpfCnpj", n.clienteDocumento());
                if (n.clienteEmail() != null) c.put("email", n.clienteEmail());
                cliente = http.post().uri("/customers").body(c).retrieve().body(JsonNode.class).path("id").asText();
            }
            ObjectNode p = JsonNodeFactory.instance.objectNode();
            p.put("customer", cliente);
            p.put("billingType", n.tipo().name());
            p.put("value", n.valor());
            p.put("dueDate", n.vencimento().toString());
            if (n.descricao() != null) p.put("description", n.descricao());
            if (n.referencia() != null) p.put("externalReference", n.referencia());
            JsonNode r = http.post().uri("/payments").body(p).retrieve().body(JsonNode.class);
            return new CobrancaCriada(r.path("id").asText(), cliente, textoOuNulo(r, "invoiceUrl"), r.path("status").asText());
        });
    }

    @Override
    public void cancelarCobranca(String idCobranca) {
        chamar(() -> {
            http.delete().uri("/payments/{id}", idCobranca).retrieve().toBodilessEntity();
            return null;
        });
    }

    @Override
    public ConsultaCobranca consultar(String idCobranca) {
        return chamar(() -> {
            JsonNode r = http.get().uri("/payments/{id}", idCobranca).retrieve().body(JsonNode.class);
            return new ConsultaCobranca(r.path("id").asText(), r.path("status").asText(),
                    r.hasNonNull("value") ? r.get("value").decimalValue() : BigDecimal.ZERO,
                    data(r.path("paymentDate").asText(null)), textoOuNulo(r, "billingType"));
        });
    }

    /** Converte qualquer falha em GatewayException com texto seguro. */
    private <T> T chamar(java.util.function.Supplier<T> acao) {
        if (!ativo) throw new GatewayException(HttpStatus.CONFLICT, "A cobrança online não está configurada.");
        try {
            return acao.get();
        } catch (HttpClientErrorException e) {
            String motivo = descricaoDoErro(e);
            if (e.getStatusCode().value() == 401) {
                throw new GatewayException(HttpStatus.BAD_GATEWAY, "O Asaas recusou a chave de API. Confira a configuração.");
            }
            throw new GatewayException(HttpStatus.BAD_REQUEST, motivo != null ? motivo : "O Asaas recusou a cobrança.");
        } catch (RestClientException e) {
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "Não foi possível falar com o Asaas agora. Tente de novo em instantes.");
        }
    }

    /** O Asaas devolve {"errors":[{"code":"...","description":"CPF/CNPJ inválido"}]}: a descrição é feita para o usuário. */
    private static String descricaoDoErro(HttpClientErrorException e) {
        try {
            JsonNode erros = new com.fasterxml.jackson.databind.ObjectMapper().readTree(e.getResponseBodyAsString()).path("errors");
            return erros.isArray() && !erros.isEmpty() ? textoOuNulo(erros.get(0), "description") : null;
        } catch (Exception ignorado) {
            return null;
        }
    }

    private static String textoOuNulo(JsonNode n, String campo) {
        return n.hasNonNull(campo) ? n.get(campo).asText() : null;
    }

    private static LocalDate data(String s) {
        try {
            return s == null ? null : LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
