package com.oficina.ordens;

import com.oficina.clientes.Cliente;
import com.oficina.funcionarios.Funcionario;
import com.oficina.veiculos.Veiculo;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ordem_servico")
@Getter
@Setter
public class OrdemServico {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String status = StatusOs.ABERTA.name();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id")
    private Veiculo veiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id")
    private Funcionario mecanico;

    private Integer kmEntrada;
    private String defeitoRelatado;
    private String observacoes;
    private LocalDate previsaoEntrega;

    private BigDecimal maoDeObra = BigDecimal.ZERO;
    private String maoDeObraDescricao;
    private BigDecimal pecasValor = BigDecimal.ZERO;
    private String pecasDescricao;
    @Enumerated(EnumType.STRING)
    private TipoValor taxaCompraTipo = TipoValor.VALOR;
    private BigDecimal taxaCompra = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING)
    private TipoValor descontoTipo = TipoValor.VALOR;
    private BigDecimal desconto = BigDecimal.ZERO;

    // calculados por CalculoValores a cada gravação
    private BigDecimal subtotalServicos = BigDecimal.ZERO;
    private BigDecimal taxaCompraValor = BigDecimal.ZERO;
    private BigDecimal descontoValor = BigDecimal.ZERO;
    private BigDecimal total = BigDecimal.ZERO;
    private BigDecimal receitaOficina = BigDecimal.ZERO;

    private LocalDateTime criadoEm = LocalDateTime.now();
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    @OneToMany(mappedBy = "os", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("posicao")
    private List<OsItem> itens = new ArrayList<>();

    public StatusOs statusAtual() {
        return StatusOs.valueOf(status);
    }
}
