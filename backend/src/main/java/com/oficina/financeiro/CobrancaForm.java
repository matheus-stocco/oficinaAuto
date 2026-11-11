package com.oficina.financeiro;

import com.oficina.financeiro.gateway.GatewayPagamento.TipoCobranca;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** valor e vencimento são opcionais: o padrão é o saldo disponível e 3 dias. cpfCnpj só é preciso se o cliente ainda não tem. */
public record CobrancaForm(
        @NotNull(message = "Informe a forma da cobrança.") TipoCobranca tipo,
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
        @Digits(integer = 10, fraction = 2, message = "O valor deve ter no máximo 2 casas decimais.") BigDecimal valor,
        LocalDate vencimento,
        @Size(max = 18) String cpfCnpj) {
}
