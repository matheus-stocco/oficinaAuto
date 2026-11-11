package com.oficina.servicos;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ServicoForm(
        @NotBlank(message = "Informe o nome do serviço.") @Size(max = 120) String nome,
        @NotNull(message = "Informe o preço.")
        @DecimalMin(value = "0.00", message = "O preço não pode ser negativo.")
        @Digits(integer = 10, fraction = 2, message = "O preço deve ter no máximo 2 casas decimais.") BigDecimal precoPadrao) {
}
