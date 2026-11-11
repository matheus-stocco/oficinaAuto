package com.oficina.erro;

import org.springframework.http.HttpStatus;

/** Erro de regra de negócio, devolvido ao cliente como JSON {"erro": "..."}. */
public class NegocioException extends RuntimeException {
    private final HttpStatus status;

    public NegocioException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
