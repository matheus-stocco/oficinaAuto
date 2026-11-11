package com.oficina.orcamentos;

import com.oficina.clientes.Cliente;
import com.oficina.ordens.ValoresDocumento;
import com.oficina.veiculos.Veiculo;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "orcamento")
@Getter
@Setter
public class Orcamento extends ValoresDocumento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    private String observacoes;
    private LocalDateTime geradoEm;
    private LocalDateTime criadoEm = LocalDateTime.now();
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    @OneToMany(mappedBy = "orcamento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("posicao")
    private List<OrcamentoItem> itens = new ArrayList<>();
}
