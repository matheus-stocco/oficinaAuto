package com.oficina.ordens;

import java.math.BigDecimal;

/**
 * O que o financeiro exige da OS ao mudar de situação. Fica aqui, e é implementado no pacote financeiro,
 * para a OS não depender do financeiro (e vice-versa).
 */
public interface RegrasFinanceirasDaOs {
    /** Só entrega com tudo pago ou fiado registrado: recusa (409) se faltar ou sobrar dinheiro em relação ao total. */
    void aoEntregar(Long osId, BigDecimal total);

    /** Não cancela OS com pagamento recebido (estorne antes) e cancela o fiado que ainda estiver em aberto. */
    void aoCancelar(Long osId);
}
