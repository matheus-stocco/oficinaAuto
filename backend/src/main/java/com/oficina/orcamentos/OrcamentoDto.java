package com.oficina.orcamentos;

import com.oficina.ordens.ItemDocumento;
import com.oficina.ordens.OsDto;
import java.time.LocalDateTime;
import java.util.List;

/** Orçamento completo. `expiraEm` só existe depois de gerado o PDF. */
public record OrcamentoDto(Long id, OsDto.Pessoa cliente, String clienteNome, String clienteTelefone,
                           OsDto.VeiculoResumo veiculo, String veiculoPlaca, String veiculoModelo, String observacoes,
                           List<OsDto.Item> itens, OsDto.Valores valores, OsDto.Totais totais,
                           LocalDateTime geradoEm, LocalDateTime expiraEm, LocalDateTime criadoEm, LocalDateTime atualizadoEm) {

    static OrcamentoDto de(Orcamento o, int horasRascunho) {
        var c = o.getCliente();
        var v = o.getVeiculo();
        return new OrcamentoDto(o.getId(),
                c == null ? null : new OsDto.Pessoa(c.getId(), c.getContato().getNome(), c.getContato().getTelefone()),
                o.getClienteNome(), o.getClienteTelefone(),
                v == null ? null : new OsDto.VeiculoResumo(v.getId(), v.getPlaca(), v.getMarca(), v.getModelo()),
                o.getVeiculoPlaca(), o.getVeiculoModelo(), o.getObservacoes(),
                o.getItens().stream().map(ItemDocumento::dto).toList(), o.valoresDto(), o.totaisDto(),
                o.getGeradoEm(), expiraEm(o, horasRascunho), o.getCriadoEm(), o.getAtualizadoEm());
    }

    static LocalDateTime expiraEm(Orcamento o, int horasRascunho) {
        return o.getGeradoEm() == null ? null : o.getGeradoEm().plusHours(horasRascunho);
    }
}
