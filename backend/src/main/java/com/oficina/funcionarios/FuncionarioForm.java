package com.oficina.funcionarios;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Entrada de criação e edição. login/senha vazios = sem acesso (na edição, senha vazia mantém a atual). */
public record FuncionarioForm(
        @NotBlank(message = "Informe o nome.") @Size(max = 120) String nome,
        @Size(max = 30) String telefone,
        @Size(max = 160) String email,
        @Size(max = 14) String cpf,
        @Size(max = 60) String login,
        String senha) {
}
