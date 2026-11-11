package com.oficina.empresa;

import java.time.ZoneOffset;

/** Dados completos (só para quem está logado). */
public record EmpresaDto(String nome, String cnpj, String telefone, String endereco, boolean temLogo,
                         String fundoTipo, String fundoCor, boolean temFundoImagem,
                         int orcamentoRascunhoHoras, long versao) {
    /** temLogo e temFundo dizem se o ARQUIVO existe (não só se o banco aponta para ele). */
    static EmpresaDto de(Empresa e, boolean temLogo, boolean temFundo) {
        return new EmpresaDto(e.getNome(), e.getCnpj(), e.getTelefone(), e.getEndereco(),
                temLogo, e.getFundoTipo(), e.getFundoCor(), temFundo,
                e.getOrcamentoRascunhoHoras(), e.getAtualizadoEm().toInstant(ZoneOffset.UTC).toEpochMilli());
    }
}
