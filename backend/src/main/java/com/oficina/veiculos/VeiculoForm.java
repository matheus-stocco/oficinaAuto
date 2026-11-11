package com.oficina.veiculos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Placa e cliente são opcionais. Marca, modelo e ano são digitados (a FIPE ficou para o fim do projeto). */
public record VeiculoForm(
        @Size(max = 10) String placa,
        @Size(max = 60) String marca,
        @NotBlank(message = "Informe o modelo.") @Size(max = 120) String modelo,
        @Size(max = 30) String ano,
        @Size(max = 30) String cor,
        Long clienteId) {
}
