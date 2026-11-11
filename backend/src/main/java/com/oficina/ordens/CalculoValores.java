package com.oficina.ordens;

import com.oficina.erro.NegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * A única fórmula de valores do sistema (OS, orçamento, PDF e relatórios usam esta classe).
 *
 * total           = Σ(itens) + mão de obra + peças + taxa de compra − desconto
 * receita oficina = Σ(itens) + mão de obra + taxa de compra − desconto   (peças é repasse, não receita)
 * taxa de compra  = valor fixo, ou % sobre o valor das peças
 * desconto        = valor fixo, ou % sobre o total bruto (antes do desconto)
 * Regra: o desconto não pode passar de Σ(itens) + mão de obra + taxa de compra.
 * Todo arredondamento é HALF_UP em 2 casas.
 */
public final class CalculoValores {
    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    public record Linha(BigDecimal quantidade, BigDecimal precoUnit) {}

    public record Entrada(List<Linha> itens, BigDecimal maoDeObra, BigDecimal pecas,
                          TipoValor taxaTipo, BigDecimal taxa, TipoValor descontoTipo, BigDecimal desconto) {}

    public record Resultado(BigDecimal subtotalServicos, BigDecimal maoDeObra, BigDecimal pecas,
                            BigDecimal taxaCompra, BigDecimal desconto, BigDecimal total,
                            BigDecimal receitaOficina, BigDecimal repassePecas) {}

    private CalculoValores() {}

    public static Resultado calcular(Entrada e) {
        BigDecimal servicos = e.itens().stream()
                .map(l -> dinheiro(l.quantidade().multiply(l.precoUnit())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal mao = dinheiro(zero(e.maoDeObra()));
        BigDecimal pecas = dinheiro(zero(e.pecas()));

        BigDecimal taxa = e.taxaTipo() == TipoValor.PERCENTUAL
                ? dinheiro(pecas.multiply(percentual(e.taxa(), "taxa de compra")).divide(CEM))
                : dinheiro(zero(e.taxa()));

        BigDecimal receitaBruta = servicos.add(mao).add(taxa);
        BigDecimal totalBruto = receitaBruta.add(pecas);

        BigDecimal desconto = e.descontoTipo() == TipoValor.PERCENTUAL
                ? dinheiro(totalBruto.multiply(percentual(e.desconto(), "desconto")).divide(CEM))
                : dinheiro(zero(e.desconto()));
        if (desconto.compareTo(receitaBruta) > 0) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "O desconto não pode passar de R$ "
                    + receitaBruta.toPlainString().replace('.', ',')
                    + " (serviços + mão de obra + taxa de compra).");
        }
        return new Resultado(servicos, mao, pecas, taxa, desconto,
                totalBruto.subtract(desconto), receitaBruta.subtract(desconto), pecas);
    }

    private static BigDecimal percentual(BigDecimal p, String campo) {
        BigDecimal v = zero(p);
        if (v.compareTo(CEM) > 0) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "O percentual de " + campo + " não pode passar de 100%.");
        }
        return v;
    }

    private static BigDecimal zero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal dinheiro(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
