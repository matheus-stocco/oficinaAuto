package com.oficina.financeiro;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Recebimento de um fiado: o valor é o do próprio fiado. */
public record BaixaForm(@NotNull(message = "Informe a forma de pagamento.") FormaPagamento forma, LocalDate data) {
}
