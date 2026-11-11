package com.oficina.financeiro.gateway;

import org.springframework.http.HttpStatus;

/** Falha ao falar com o gateway. A mensagem é segura para mostrar ao atendente (nunca traz chave nem resposta crua). */
public class GatewayException extends RuntimeException {
    private final HttpStatus status;

    public GatewayException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
