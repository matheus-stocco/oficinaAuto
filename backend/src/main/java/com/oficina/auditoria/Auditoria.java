package com.oficina.auditoria;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "auditoria")
@Getter
@Setter
public class Auditoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long funcionarioId;
    private String acao;
    private String detalhe;
    private LocalDateTime criadoEm = LocalDateTime.now();
}
