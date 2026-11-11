package com.oficina.agenda;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Cliente e veículo: cadastrados (id) ou avulsos. Só a data é obrigatória. */
public record AgendamentoForm(
        @NotNull(message = "Informe a data do agendamento.") LocalDate data,
        LocalTime hora,
        Long clienteId,
        @Size(max = 120) String clienteNome,
        @Size(max = 30) String clienteTelefone,
        Long veiculoId,
        @Size(max = 10) String veiculoPlaca,
        @Size(max = 120) String veiculoModelo,
        Long funcionarioId,
        @Size(max = 1000) String observacoes,
        @Size(max = 50, message = "O agendamento pode ter no máximo 50 serviços.") List<@Valid ServicoForm> servicos) {

    public record ServicoForm(Long servicoId,
                              @NotBlank(message = "Informe a descrição do serviço.") @Size(max = 160) String descricao) {}
}
