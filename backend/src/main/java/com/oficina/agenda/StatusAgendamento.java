package com.oficina.agenda;

public enum StatusAgendamento {
    AGENDADO, REALIZADO, CANCELADO, NAO_COMPARECEU;

    public String rotulo() {
        return switch (this) {
            case AGENDADO -> "agendado";
            case REALIZADO -> "realizado";
            case CANCELADO -> "cancelado";
            case NAO_COMPARECEU -> "não compareceu";
        };
    }
}
