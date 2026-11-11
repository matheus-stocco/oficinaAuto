package com.oficina.servicos;

import java.math.BigDecimal;

public record ServicoDto(Long id, String nome, BigDecimal precoPadrao, boolean ativo) {
    static ServicoDto de(Servico s) {
        return new ServicoDto(s.getId(), s.getNome(), s.getPrecoPadrao(), s.isAtivo());
    }
}
