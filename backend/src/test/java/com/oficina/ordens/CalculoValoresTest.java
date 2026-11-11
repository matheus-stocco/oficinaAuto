package com.oficina.ordens;

import static com.oficina.ordens.TipoValor.PERCENTUAL;
import static com.oficina.ordens.TipoValor.VALOR;
import static org.junit.jupiter.api.Assertions.*;

import com.oficina.erro.NegocioException;
import com.oficina.ordens.CalculoValores.Entrada;
import com.oficina.ordens.CalculoValores.Linha;
import com.oficina.ordens.CalculoValores.Resultado;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculoValoresTest {
    private static BigDecimal d(String s) { return new BigDecimal(s); }

    private static Linha l(String qtd, String preco) { return new Linha(d(qtd), d(preco)); }

    private static Entrada entrada(List<Linha> itens, String mao, String pecas,
                                   TipoValor taxaTipo, String taxa, TipoValor descTipo, String desc) {
        return new Entrada(itens, d(mao), d(pecas), taxaTipo, d(taxa), descTipo, d(desc));
    }

    @Test
    void exemploCompletoComPercentuais() {
        // serviços 2×80,50 + 120 = 281,00; mão de obra 100; peças 200; taxa 10% = 20,00
        // total bruto = 601,00; desconto 5% = 30,05
        Resultado r = CalculoValores.calcular(entrada(List.of(l("2", "80.50"), l("1", "120")),
                "100", "200", PERCENTUAL, "10", PERCENTUAL, "5"));
        assertEquals(d("281.00"), r.subtotalServicos());
        assertEquals(d("20.00"), r.taxaCompra());
        assertEquals(d("30.05"), r.desconto());
        assertEquals(d("570.95"), r.total());
        assertEquals(d("370.95"), r.receitaOficina());
        assertEquals(d("200.00"), r.repassePecas());
    }

    @Test
    void repasseNuncaEntraNaReceitaETotalEhReceitaMaisRepasse() {
        Resultado r = CalculoValores.calcular(entrada(List.of(l("1", "50")), "30", "999.99", VALOR, "0", VALOR, "0"));
        assertEquals(d("80.00"), r.receitaOficina());
        assertEquals(d("999.99"), r.repassePecas());
        assertEquals(r.receitaOficina().add(r.repassePecas()), r.total());
    }

    @Test
    void arredondaMeioParaCimaEmCadaEtapa() {
        // 3 × 33,33 = 99,99 ; peças 199,99 com taxa 7,5% = 14,99925 -> 15,00
        Resultado r = CalculoValores.calcular(entrada(List.of(l("3", "33.33")), "0", "199.99", PERCENTUAL, "7.5", VALOR, "0"));
        assertEquals(d("99.99"), r.subtotalServicos());
        assertEquals(d("15.00"), r.taxaCompra());
        assertEquals(d("314.98"), r.total());
    }

    @Test
    void quantidadeFracionadaEMultiplicaSemPerderCentavos() {
        Resultado r = CalculoValores.calcular(entrada(List.of(l("1.5", "10.10")), "0", "0", VALOR, "0", VALOR, "0"));
        assertEquals(d("15.15"), r.subtotalServicos());
    }

    @Test
    void descontoEmValorEPercentualSobreOTotalBrutoComRepasse() {
        // total bruto = 100 + 100 (peças) = 200 ; 10% = 20,00
        Resultado r = CalculoValores.calcular(entrada(List.of(l("1", "100")), "0", "100", VALOR, "0", PERCENTUAL, "10"));
        assertEquals(d("20.00"), r.desconto());
        assertEquals(d("180.00"), r.total());
        assertEquals(d("80.00"), r.receitaOficina());
        Resultado v = CalculoValores.calcular(entrada(List.of(l("1", "100")), "0", "100", VALOR, "0", VALOR, "15.50"));
        assertEquals(d("15.50"), v.desconto());
    }

    @Test
    void descontoNoLimiteDaReceitaPassaEAcimaDeleNao() {
        // receita bruta = 100 + 50 + taxa 0 = 150
        assertEquals(d("0.00"), CalculoValores.calcular(entrada(List.of(l("1", "100")), "50", "300", VALOR, "0", VALOR, "150"))
                .receitaOficina());
        NegocioException e = assertThrows(NegocioException.class,
                () -> CalculoValores.calcular(entrada(List.of(l("1", "100")), "50", "300", VALOR, "0", VALOR, "150.01")));
        assertEquals(400, e.getStatus().value());
        assertTrue(e.getMessage().contains("150,00"));
    }

    @Test
    void descontoPercentualQueEstouraAReceitaTambemEhRecusado() {
        // 100% do total bruto (com peças) passa da receita bruta
        assertThrows(NegocioException.class,
                () -> CalculoValores.calcular(entrada(List.of(l("1", "100")), "0", "100", VALOR, "0", PERCENTUAL, "100")));
    }

    @Test
    void percentualAcimaDeCemEhRecusado() {
        assertThrows(NegocioException.class,
                () -> CalculoValores.calcular(entrada(List.of(l("1", "100")), "0", "100", PERCENTUAL, "100.01", VALOR, "0")));
        assertThrows(NegocioException.class,
                () -> CalculoValores.calcular(entrada(List.of(l("1", "100")), "0", "0", VALOR, "0", PERCENTUAL, "101")));
    }

    @Test
    void tudoZeradoDaZeroEValoresNulosViramZero() {
        Resultado r = CalculoValores.calcular(new Entrada(List.of(), null, null, null, null, null, null));
        assertEquals(d("0.00"), r.total());
        assertEquals(d("0.00"), r.receitaOficina());
    }

    @Test
    void somaDeMuitosItensNaoAcumulaErroDeArredondamento() {
        List<Linha> itens = java.util.Collections.nCopies(10, l("1", "0.10"));
        assertEquals(d("1.00"), CalculoValores.calcular(new Entrada(itens, null, null, VALOR, null, VALOR, null)).total());
    }
}
