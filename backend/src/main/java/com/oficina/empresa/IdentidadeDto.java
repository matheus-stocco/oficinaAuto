package com.oficina.empresa;

import java.time.ZoneOffset;

/** Só o que a tela de login precisa (público): nada de CNPJ, telefone ou endereço. */
public record IdentidadeDto(String nome, boolean temLogo, String fundoTipo, String fundoCor,
                            boolean temFundoImagem, long versao) {
    static IdentidadeDto de(Empresa e, boolean temLogo, boolean temFundo) {
        return new IdentidadeDto(e.getNome(), temLogo, e.getFundoTipo(), e.getFundoCor(),
                temFundo, e.getAtualizadoEm().toInstant(ZoneOffset.UTC).toEpochMilli());
    }
}
