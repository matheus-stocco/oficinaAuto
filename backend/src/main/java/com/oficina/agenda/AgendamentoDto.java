package com.oficina.agenda;

import com.oficina.ordens.OsDto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record AgendamentoDto(Long id, LocalDate data, LocalTime hora, String status,
                             OsDto.Pessoa cliente, String clienteNome, String clienteTelefone,
                             OsDto.VeiculoResumo veiculo, String veiculoPlaca, String veiculoModelo,
                             OsDto.Pessoa mecanico, String observacoes, List<Servico> servicos, Long osId,
                             LocalDateTime criadoEm) {
    public record Servico(Long servicoId, String descricao) {}

    static AgendamentoDto de(Agendamento a) {
        var c = a.getCliente();
        var v = a.getVeiculo();
        var m = a.getMecanico();
        return new AgendamentoDto(a.getId(), a.getData(), a.getHora(), a.getStatus(),
                c == null ? null : new OsDto.Pessoa(c.getId(), c.getContato().getNome(), c.getContato().getTelefone()),
                a.getClienteNome(), a.getClienteTelefone(),
                v == null ? null : new OsDto.VeiculoResumo(v.getId(), v.getPlaca(), v.getMarca(), v.getModelo()),
                a.getVeiculoPlaca(), a.getVeiculoModelo(),
                m == null ? null : new OsDto.Pessoa(m.getId(), m.getContato().getNome(), m.getContato().getTelefone()),
                a.getObservacoes(),
                a.getServicos().stream().map(s -> new Servico(s.getServico() == null ? null : s.getServico().getId(), s.getDescricao())).toList(),
                a.getOsId(), a.getCriadoEm());
    }
}
