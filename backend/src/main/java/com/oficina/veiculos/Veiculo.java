package com.oficina.veiculos;

import com.oficina.clientes.Cliente;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "veiculo")
@Getter
@Setter
public class Veiculo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String placa;
    private String marca;
    private String modelo;
    private String ano;
    private String cor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    private LocalDateTime criadoEm = LocalDateTime.now();
}
