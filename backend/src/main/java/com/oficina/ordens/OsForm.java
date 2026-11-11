package com.oficina.ordens;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Tudo é opcional, menos o que a OS precisa para ter algum valor. Valores nulos contam como zero. */
public record OsForm(
        Long clienteId,
        Long veiculoId,
        Long funcionarioId,
        @Min(value = 0, message = "O km não pode ser negativo.") @Max(value = 9_999_999, message = "Km inválido.") Integer kmEntrada,
        @Size(max = 1000) String defeitoRelatado,
        @Size(max = 1000) String observacoes,
        LocalDate previsaoEntrega,
        @Size(max = 100, message = "A OS pode ter no máximo 100 itens.") List<@Valid ItemForm> itens,
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
        @Digits(integer = 10, fraction = 2, message = "O desconto deve ter no máximo 2 casas decimais.") BigDecimal desconto) {

    public record ItemForm(
            Long servicoId,
            @NotBlank(message = "Informe a descrição do serviço.") @Size(max = 160) String descricao,
            @NotNull(message = "Informe a quantidade.")
            @DecimalMin(value = "0.01", message = "A quantidade deve ser maior que zero.")
            @Digits(integer = 8, fraction = 2, message = "Quantidade inválida.") BigDecimal quantidade,
            @NotNull(message = "Informe o preço do item.")
            @DecimalMin(value = "0.00", message = "O preço do item não pode ser negativo.")
            @Digits(integer = 10, fraction = 2, message = "O preço do item deve ter no máximo 2 casas decimais.") BigDecimal precoUnit) {
    }
}
