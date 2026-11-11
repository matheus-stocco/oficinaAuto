package com.oficina.orcamentos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Linha da lista de orçamentos. */
public record OrcamentoResumoDto(Long id, String clienteNome, String veiculo, BigDecimal total,
                                 LocalDateTime geradoEm, LocalDateTime expiraEm, LocalDateTime criadoEm) {
    static OrcamentoResumoDto de(Orcamento o, int horasRascunho) {
        String nome = o.getCliente() != null ? o.getCliente().getContato().getNome() : o.getClienteNome();
        String veiculo = o.getVeiculo() != null
                ? ((o.getVeiculo().getMarca() == null ? "" : o.getVeiculo().getMarca() + " ") + o.getVeiculo().getModelo()).trim()
                : o.getVeiculoModelo();
        return new OrcamentoResumoDto(o.getId(), nome, veiculo, o.getTotal(), o.getGeradoEm(),
                OrcamentoDto.expiraEm(o, horasRascunho), o.getCriadoEm());
    }
}
