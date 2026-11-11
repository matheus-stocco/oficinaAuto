package com.oficina.ordens;

import java.util.Map;
import java.util.Set;

/** Estados da OS e as únicas transições permitidas. ENTREGUE só volta para PRONTA, com confirmação. */
public enum StatusOs {
    ABERTA, EM_EXECUCAO, PRONTA, ENTREGUE, CANCELADA;

    private static final Map<StatusOs, Set<StatusOs>> PERMITIDAS = Map.of(
            ABERTA, Set.of(EM_EXECUCAO, CANCELADA),
            EM_EXECUCAO, Set.of(PRONTA, CANCELADA),
            PRONTA, Set.of(ENTREGUE, CANCELADA),
            ENTREGUE, Set.of(PRONTA),
            CANCELADA, Set.of());

    public boolean podeIrPara(StatusOs destino) {
        return PERMITIDAS.get(this).contains(destino);
    }

    /** Só nesses estados os dados da OS podem ser alterados. */
    public boolean editavel() {
        return this == ABERTA || this == EM_EXECUCAO || this == PRONTA;
    }
}
