package com.oficina.financeiro;

import com.oficina.ordens.OrdemServico;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "pagamento")
@Getter
@Setter
public class Pagamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "os_id")
    private OrdemServico os;

    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    private FormaPagamento forma;

    private LocalDate data;
    private String origem = "MANUAL";
    private String idExterno;
    private LocalDateTime estornadoEm;
    private LocalDateTime criadoEm = LocalDateTime.now();

    public boolean estornado() {
        return estornadoEm != null;
    }
}
