package com.oficina.empresa;

import java.time.ZoneOffset;

/** Só o que a tela de login precisa (público): nada de CNPJ, telefone ou endereço. */
public record IdentidadeDto(String nome, boolean temLogo, String fundoTipo, String fundoCor,
                            boolean temFundoImagem, long versao) {
    static IdentidadeDto de(Empresa e) {
        return new IdentidadeDto(e.getNome(), e.getLogoArquivo() != null, e.getFundoTipo(), e.getFundoCor(),
                e.getFundoArquivo() != null, e.getAtualizadoEm().toInstant(ZoneOffset.UTC).toEpochMilli());
    }
}
