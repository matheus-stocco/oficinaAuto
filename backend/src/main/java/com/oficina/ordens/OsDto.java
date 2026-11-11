package com.oficina.ordens;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** OS completa (tela de edição). Telefone do cliente é nulo quando ele foi anonimizado. */
public record OsDto(Long id, String status, Pessoa cliente, VeiculoResumo veiculo, Pessoa mecanico,
                    Integer kmEntrada, String defeitoRelatado, String observacoes, LocalDate previsaoEntrega,
                    List<Item> itens, Valores valores, Totais totais, LocalDateTime criadoEm, LocalDateTime atualizadoEm) {

    public record Pessoa(Long id, String nome, String telefone) {}
    public record VeiculoResumo(Long id, String placa, String marca, String modelo) {}
    public record Item(Long id, Long servicoId, String descricao, BigDecimal quantidade, BigDecimal precoUnit, BigDecimal subtotal) {}
    public record Valores(BigDecimal maoDeObra, String maoDeObraDescricao, BigDecimal pecasValor, String pecasDescricao,
                          TipoValor taxaCompraTipo, BigDecimal taxaCompra, TipoValor descontoTipo, BigDecimal desconto) {}
    public record Totais(BigDecimal subtotalServicos, BigDecimal taxaCompraValor, BigDecimal descontoValor,
                         BigDecimal total, BigDecimal receitaOficina, BigDecimal repassePecas) {}

    public static OsDto de(OrdemServico o) {
        var c = o.getCliente();
        var v = o.getVeiculo();
        var m = o.getMecanico();
        return new OsDto(o.getId(), o.getStatus(),
                c == null ? null : new Pessoa(c.getId(), c.getContato().getNome(), c.getContato().getTelefone()),
                v == null ? null : new VeiculoResumo(v.getId(), v.getPlaca(), v.getMarca(), v.getModelo()),
                m == null ? null : new Pessoa(m.getId(), m.getContato().getNome(), m.getContato().getTelefone()),
                o.getKmEntrada(), o.getDefeitoRelatado(), o.getObservacoes(), o.getPrevisaoEntrega(),
                o.getItens().stream().map(i -> new Item(i.getId(), i.getServico() == null ? null : i.getServico().getId(),
                        i.getDescricao(), i.getQuantidade(), i.getPrecoUnit(),
                        i.getQuantidade().multiply(i.getPrecoUnit()).setScale(2, java.math.RoundingMode.HALF_UP))).toList(),
                new Valores(o.getMaoDeObra(), o.getMaoDeObraDescricao(), o.getPecasValor(), o.getPecasDescricao(),
                        o.getTaxaCompraTipo(), o.getTaxaCompra(), o.getDescontoTipo(), o.getDesconto()),
                new Totais(o.getSubtotalServicos(), o.getTaxaCompraValor(), o.getDescontoValor(), o.getTotal(),
                        o.getReceitaOficina(), o.getPecasValor()),
                o.getCriadoEm(), o.getAtualizadoEm());
    }
}
