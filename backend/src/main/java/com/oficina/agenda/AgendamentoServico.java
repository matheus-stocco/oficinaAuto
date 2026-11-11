package com.oficina.agenda;

import com.oficina.servicos.Servico;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Serviço a executar no agendamento: do catálogo ou texto livre. Sem preço: o preço entra na OS. */
@Entity
@Table(name = "agendamento_servico")
@Getter
@Setter
public class AgendamentoServico {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agendamento_id")
    private Agendamento agendamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servico_id")
    private Servico servico;

    private String descricao;
    private int posicao;
}
