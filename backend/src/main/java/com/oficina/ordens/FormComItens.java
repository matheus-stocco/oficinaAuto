package com.oficina.ordens;

import java.math.BigDecimal;
import java.util.List;

/** O que OS e orçamento têm em comum no formulário: itens e o bloco de valores. */
public interface FormComItens {
    List<OsForm.ItemForm> itens();
    BigDecimal maoDeObra();
    String maoDeObraDescricao();
    BigDecimal pecasValor();
    String pecasDescricao();
    TipoValor taxaCompraTipo();
    BigDecimal taxaCompra();
    TipoValor descontoTipo();
    BigDecimal desconto();
}
