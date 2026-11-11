package com.oficina.orcamentos;

import com.oficina.ordens.FormComItens;
import com.oficina.ordens.OsForm;
import com.oficina.ordens.TipoValor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/** Cliente e veículo: cadastrados (id) ou avulsos (nome/telefone, placa/modelo). Valores iguais aos da OS. */
public record OrcamentoForm(
        Long clienteId,
        @Size(max = 120) String clienteNome,
        @Size(max = 30) String clienteTelefone,
        Long veiculoId,
        @Size(max = 10) String veiculoPlaca,
        @Size(max = 120) String veiculoModelo,
        @Size(max = 1000) String observacoes,
        @Size(max = 100, message = "O orçamento pode ter no máximo 100 itens.") List<OsForm.@Valid ItemForm> itens,
        @DecimalMin(value = "0.00", message = "A mão de obra não pode ser negativa.")
        @Digits(integer = 10, fraction = 2, message = "A mão de obra deve ter no máximo 2 casas decimais.") BigDecimal maoDeObra,
        @Size(max = 250) String maoDeObraDescricao,
        @DecimalMin(value = "0.00", message = "O valor das peças não pode ser negativo.")
        @Digits(integer = 10, fraction = 2, message = "O valor das peças deve ter no máximo 2 casas decimais.") BigDecimal pecasValor,
        @Size(max = 500) String pecasDescricao,
        TipoValor taxaCompraTipo,
        @DecimalMin(value = "0.00", message = "A taxa de compra não pode ser negativa.")
        @Digits(integer = 10, fraction = 2, message = "A taxa de compra deve ter no máximo 2 casas decimais.") BigDecimal taxaCompra,
        TipoValor descontoTipo,
        @DecimalMin(value = "0.00", message = "O desconto não pode ser negativo.")
        @Digits(integer = 10, fraction = 2, message = "O desconto deve ter no máximo 2 casas decimais.") BigDecimal desconto)
        implements FormComItens {
}
