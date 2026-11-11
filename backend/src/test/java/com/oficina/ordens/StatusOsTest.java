package com.oficina.ordens;

import static com.oficina.ordens.StatusOs.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StatusOsTest {
    @Test
    void fluxoNormalAvancaUmEstadoPorVez() {
        assertTrue(ABERTA.podeIrPara(EM_EXECUCAO));
        assertTrue(EM_EXECUCAO.podeIrPara(PRONTA));
        assertTrue(PRONTA.podeIrPara(ENTREGUE));
    }

    @Test
    void naoPulaEtapasNemVoltaSemSerAReabertura() {
        assertFalse(ABERTA.podeIrPara(PRONTA));
        assertFalse(ABERTA.podeIrPara(ENTREGUE));
        assertFalse(EM_EXECUCAO.podeIrPara(ENTREGUE));
        assertFalse(EM_EXECUCAO.podeIrPara(ABERTA));
        assertFalse(PRONTA.podeIrPara(EM_EXECUCAO));
        assertFalse(ABERTA.podeIrPara(ABERTA));
    }

    @Test
    void canceladaDeQualquerEstadoAbertoMasNaoDeEntregueOuCancelada() {
        assertTrue(ABERTA.podeIrPara(CANCELADA));
        assertTrue(EM_EXECUCAO.podeIrPara(CANCELADA));
        assertTrue(PRONTA.podeIrPara(CANCELADA));
        assertFalse(ENTREGUE.podeIrPara(CANCELADA));
        for (StatusOs s : values()) assertFalse(CANCELADA.podeIrPara(s));
    }

    @Test
    void entregueSoVoltaParaPronta() {
        assertTrue(ENTREGUE.podeIrPara(PRONTA));
        for (StatusOs s : values()) if (s != PRONTA) assertFalse(ENTREGUE.podeIrPara(s));
    }

    @Test
    void soAbertasEmExecucaoEProntasSaoEditaveis() {
        assertTrue(ABERTA.editavel());
        assertTrue(EM_EXECUCAO.editavel());
        assertTrue(PRONTA.editavel());
        assertFalse(ENTREGUE.editavel());
        assertFalse(CANCELADA.editavel());
    }
}
