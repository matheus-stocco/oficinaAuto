package com.oficina.agenda;

import com.oficina.clientes.Cliente;
import com.oficina.funcionarios.Funcionario;
import com.oficina.veiculos.Veiculo;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "agendamento")
@Getter
@Setter
public class Agendamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate data;
    private LocalTime hora;
    private String status = StatusAgendamento.AGENDADO.name();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;
    private String clienteNome;
    private String clienteTelefone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id")
    private Veiculo veiculo;
    private String veiculoPlaca;
    private String veiculoModelo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id")
    private Funcionario mecanico;

    private String observacoes;
    private Long osId;
    private LocalDateTime criadoEm = LocalDateTime.now();
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    // BatchSize: uma lista de agendamentos carrega os serviços de todos em poucas consultas, não uma por linha
    @OneToMany(mappedBy = "agendamento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("posicao")
    @BatchSize(size = 100)
    private List<AgendamentoServico> servicos = new ArrayList<>();

    public StatusAgendamento statusAtual() {
        return StatusAgendamento.valueOf(status);
    }
}
