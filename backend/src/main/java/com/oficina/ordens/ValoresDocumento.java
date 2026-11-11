package com.oficina.ordens;

import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.MappedSuperclass;
import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Colunas de valores compartilhadas por OS e orçamento. Os totais vêm sempre de {@link CalculoValores}. */
@MappedSuperclass
@Getter
@Setter
public abstract class ValoresDocumento {
    private BigDecimal maoDeObra = BigDecimal.ZERO;
    private String maoDeObraDescricao;
    private BigDecimal pecasValor = BigDecimal.ZERO;
    private String pecasDescricao;
    @Enumerated(EnumType.STRING)
    private TipoValor taxaCompraTipo = TipoValor.VALOR;
    private BigDecimal taxaCompra = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING)
    private TipoValor descontoTipo = TipoValor.VALOR;
    private BigDecimal desconto = BigDecimal.ZERO;

    // calculados a cada gravação
    private BigDecimal subtotalServicos = BigDecimal.ZERO;
    private BigDecimal taxaCompraValor = BigDecimal.ZERO;
    private BigDecimal descontoValor = BigDecimal.ZERO;
    private BigDecimal total = BigDecimal.ZERO;
    private BigDecimal receitaOficina = BigDecimal.ZERO;

    /** Copia os valores do formulário e recalcula todos os totais (recusa desconto acima do permitido). */
    public void aplicarValores(FormComItens f) {
        List<OsForm.ItemForm> itens = f.itens() == null ? List.of() : f.itens();
        var calculo = CalculoValores.calcular(new CalculoValores.Entrada(
                itens.stream().map(i -> new CalculoValores.Linha(i.quantidade(), i.precoUnit())).toList(),
                f.maoDeObra(), f.pecasValor(), f.taxaCompraTipo(), f.taxaCompra(), f.descontoTipo(), f.desconto()));
        maoDeObra = calculo.maoDeObra();
        maoDeObraDescricao = vazioParaNulo(f.maoDeObraDescricao());
        pecasValor = calculo.pecas();
        pecasDescricao = vazioParaNulo(f.pecasDescricao());
        taxaCompraTipo = f.taxaCompraTipo() == null ? TipoValor.VALOR : f.taxaCompraTipo();
        taxaCompra = f.taxaCompra() == null ? BigDecimal.ZERO : f.taxaCompra();
        descontoTipo = f.descontoTipo() == null ? TipoValor.VALOR : f.descontoTipo();
        desconto = f.desconto() == null ? BigDecimal.ZERO : f.desconto();
        subtotalServicos = calculo.subtotalServicos();
        taxaCompraValor = calculo.taxaCompra();
        descontoValor = calculo.desconto();
        total = calculo.total();
        receitaOficina = calculo.receitaOficina();
    }

    public OsDto.Valores valoresDto() {
        return new OsDto.Valores(maoDeObra, maoDeObraDescricao, pecasValor, pecasDescricao,
                taxaCompraTipo, taxaCompra, descontoTipo, desconto);
    }

    public OsDto.Totais totaisDto() {
        return new OsDto.Totais(subtotalServicos, taxaCompraValor, descontoValor, total, receitaOficina, pecasValor);
    }

    static String vazioParaNulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
