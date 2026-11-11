package com.oficina.ordens;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "os_item")
@Getter
@Setter
public class OsItem extends ItemDocumento {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "os_id")
    private OrdemServico os;
}
