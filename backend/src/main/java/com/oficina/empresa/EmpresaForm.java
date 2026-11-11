package com.oficina.empresa;

import jakarta.validation.constraints.*;

public record EmpresaForm(
        @NotBlank(message = "Informe o nome da empresa.") @Size(max = 120) String nome,
        @Size(max = 18) String cnpj,
        @Size(max = 30) String telefone,
        @Size(max = 250) String endereco,
        @Pattern(regexp = "COR|IMAGEM", message = "Tipo de fundo inválido.") String fundoTipo,
        @Pattern(regexp = "#[0-9a-fA-F]{6}", message = "Cor de fundo inválida.") String fundoCor,
        @Min(value = 1, message = "O prazo do rascunho deve ser de 1 a 168 horas.")
        @Max(value = 168, message = "O prazo do rascunho deve ser de 1 a 168 horas.") int orcamentoRascunhoHoras) {
}
