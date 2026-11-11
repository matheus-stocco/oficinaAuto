package com.oficina.financeiro;

import com.oficina.ordens.OrdemServico;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** Fiado: o que o cliente ficou devendo numa OS. */
@Entity
@Table(name = "receber")
@Getter
@Setter
public class Receber {
    public static final String ABERTO = "ABERTO";
    public static final String PAGO = "PAGO";
    public static final String CANCELADO = "CANCELADO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "os_id")
    private OrdemServico os;

    private BigDecimal valor;
    private LocalDate vencimento;
    private String status = ABERTO;
    private LocalDate pagoEm;
    private Long pagamentoId;
    private LocalDateTime criadoEm = LocalDateTime.now();
}
