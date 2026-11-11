package com.oficina.financeiro;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record FiadoForm(
        @NotNull(message = "Informe o valor.") @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
        @Digits(integer = 10, fraction = 2, message = "O valor deve ter no máximo 2 casas decimais.") BigDecimal valor,
        @NotNull(message = "Informe o vencimento.") LocalDate vencimento) {
}
