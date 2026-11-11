package com.oficina.ordens;

import com.oficina.servicos.Servico;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/** Linha de serviço de OS ou orçamento. Descrição e preço ficam gravados aqui (o catálogo pode mudar depois). */
@MappedSuperclass
@Getter
@Setter
public abstract class ItemDocumento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servico_id")
    private Servico servico;

    private String descricao;
    private BigDecimal quantidade;
    private BigDecimal precoUnit;
    private int posicao;

    public OsDto.Item dto() {
        return new OsDto.Item(id, servico == null ? null : servico.getId(), descricao, quantidade, precoUnit,
                quantidade.multiply(precoUnit).setScale(2, java.math.RoundingMode.HALF_UP));
    }
}
