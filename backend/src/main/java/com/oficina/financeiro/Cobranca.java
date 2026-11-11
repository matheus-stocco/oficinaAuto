package com.oficina.financeiro;

import com.oficina.ordens.OrdemServico;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** Cobrança online de uma OS. Fica PENDENTE até o webhook avisar o pagamento. */
@Entity
@Table(name = "cobranca")
@Getter
@Setter
public class Cobranca {
    public static final String PENDENTE = "PENDENTE";
    public static final String PAGA = "PAGA";
    public static final String CANCELADA = "CANCELADA";
    public static final String ESTORNADA = "ESTORNADA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "os_id")
    private OrdemServico os;

    private String asaasId;
    private String tipo;
    private BigDecimal valor;
    private LocalDate vencimento;
    private String link;
    private String status = PENDENTE;
    private LocalDateTime criadoEm = LocalDateTime.now();
}
