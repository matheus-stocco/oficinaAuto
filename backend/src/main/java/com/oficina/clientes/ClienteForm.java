package com.oficina.clientes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Só nome e telefone são obrigatórios: o balcão não tem tempo. */
public record ClienteForm(
        @NotBlank(message = "Informe o nome.") @Size(max = 120) String nome,
        @NotBlank(message = "Informe o telefone.") @Size(max = 30) String telefone,
        @Size(max = 160) String email,
        @Size(max = 18) String documento,
        @Size(max = 250) String endereco,
        @Size(max = 500) String observacoes) {
}
