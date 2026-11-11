package com.oficina.agenda;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Linha do calendário e da lista de consulta. */
public record AgendamentoResumoDto(Long id, LocalDate data, LocalTime hora, String status, String clienteNome,
                                   String veiculo, String mecanicoNome, List<String> servicos, Long osId) {
    static AgendamentoResumoDto de(Agendamento a) {
        String nome = a.getCliente() != null ? a.getCliente().getContato().getNome() : a.getClienteNome();
        String veiculo = a.getVeiculo() != null
                ? ((a.getVeiculo().getMarca() == null ? "" : a.getVeiculo().getMarca() + " ") + a.getVeiculo().getModelo()
                    + (a.getVeiculo().getPlaca() == null ? "" : " · " + a.getVeiculo().getPlaca())).trim()
                : ((a.getVeiculoModelo() == null ? "" : a.getVeiculoModelo())
                    + (a.getVeiculoPlaca() == null ? "" : (a.getVeiculoModelo() == null ? "" : " · ") + a.getVeiculoPlaca())).trim();
        return new AgendamentoResumoDto(a.getId(), a.getData(), a.getHora(), a.getStatus(), nome,
                veiculo.isEmpty() ? null : veiculo,
                a.getMecanico() == null ? null : a.getMecanico().getContato().getNome(),
                a.getServicos().stream().map(AgendamentoServico::getDescricao).toList(), a.getOsId());
    }
}
