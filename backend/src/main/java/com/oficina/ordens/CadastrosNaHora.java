package com.oficina.ordens;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** O que cadastrar na hora de criar a OS (tudo opcional): o cliente e o veículo avulsos do orçamento ou do agendamento. */
public record CadastrosNaHora(@Valid ClienteNovo cliente, @Valid VeiculoNovo veiculo) {
    public record ClienteNovo(@NotBlank(message = "Informe o nome do cliente.") @Size(max = 120) String nome,
                              @NotBlank(message = "Informe o telefone do cliente.") @Size(max = 30) String telefone) {}

    public record VeiculoNovo(@Size(max = 10) String placa,
                              @NotBlank(message = "Informe o modelo do veículo.") @Size(max = 120) String modelo) {}
}
