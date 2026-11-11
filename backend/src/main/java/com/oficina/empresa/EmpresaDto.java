package com.oficina.empresa;

import java.time.ZoneOffset;

/** Dados completos (só para quem está logado). */
public record EmpresaDto(String nome, String cnpj, String telefone, String endereco, boolean temLogo,
                         String fundoTipo, String fundoCor, boolean temFundoImagem,
                         int orcamentoRascunhoHoras, long versao) {
    static EmpresaDto de(Empresa e) {
        return new EmpresaDto(e.getNome(), e.getCnpj(), e.getTelefone(), e.getEndereco(),
                e.getLogoArquivo() != null, e.getFundoTipo(), e.getFundoCor(), e.getFundoArquivo() != null,
                e.getOrcamentoRascunhoHoras(), e.getAtualizadoEm().toInstant(ZoneOffset.UTC).toEpochMilli());
    }
}
