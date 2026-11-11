package com.oficina.orcamentos;

import com.oficina.ordens.ItemDocumento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "orcamento_item")
@Getter
@Setter
public class OrcamentoItem extends ItemDocumento {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orcamento_id")
    private Orcamento orcamento;
}
