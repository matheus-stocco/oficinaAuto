package com.oficina.ordens;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Linha da lista de OS: leve, sem os itens. */
public record OsResumoDto(Long id, String status, String clienteNome, String veiculo, String placa,
                          String mecanicoNome, BigDecimal total, LocalDate previsaoEntrega, LocalDateTime criadoEm) {
    static OsResumoDto de(OrdemServico o) {
        var v = o.getVeiculo();
        return new OsResumoDto(o.getId(), o.getStatus(),
                o.getCliente() == null ? null : o.getCliente().getContato().getNome(),
                v == null ? null : ((v.getMarca() == null ? "" : v.getMarca() + " ") + v.getModelo()).trim(),
                v == null ? null : v.getPlaca(),
                o.getMecanico() == null ? null : o.getMecanico().getContato().getNome(),
                o.getTotal(), o.getPrevisaoEntrega(), o.getCriadoEm());
    }
}
