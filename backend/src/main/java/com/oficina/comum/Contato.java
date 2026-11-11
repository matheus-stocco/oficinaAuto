package com.oficina.comum;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

/** Campos de contato compartilhados por funcionário e cliente (endereço entra no módulo de clientes). */
@Embeddable
@Getter
@Setter
public class Contato {
    private String nome;
    private String telefone;
    private String email;
}
