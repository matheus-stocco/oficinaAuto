package com.oficina.financeiro;

import com.fasterxml.jackson.databind.JsonNode;
import com.oficina.erro.NegocioException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Webhook do Asaas. Público de propósito (o Asaas não tem login), protegido pelo token do cabeçalho asaas-access-token. */
@RestController
@RequestMapping("/api/webhooks")
public class AsaasWebhookController {
    private final AsaasWebhookService service;
    private final String token;

    public AsaasWebhookController(AsaasWebhookService service, @Value("${app.asaas.webhook-token:}") String token) {
        this.service = service;
        this.token = token;
    }

    @PostMapping("/asaas")
    Map<String, Boolean> receber(@RequestHeader(value = "asaas-access-token", required = false) String recebido,
                                 @RequestBody JsonNode corpo) {
        if (!tokenConfere(recebido)) {
            throw new NegocioException(HttpStatus.UNAUTHORIZED, "Token inválido.");
        }
        try {
            service.processar(corpo);
        } catch (DataIntegrityViolationException duplicado) {
            // duas entregas do mesmo evento ao mesmo tempo: o índice único segurou a segunda, que já está registrada
        }
        return Map.of("received", true);
    }

    /** Sem token configurado nada é aceito. A comparação leva o mesmo tempo, acerte ou erre. */
    private boolean tokenConfere(String recebido) {
        if (token.isBlank() || recebido == null) return false;
        return MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), recebido.getBytes(StandardCharsets.UTF_8));
    }
}
