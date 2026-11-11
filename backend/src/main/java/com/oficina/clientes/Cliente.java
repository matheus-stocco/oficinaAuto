package com.oficina.clientes;

import com.oficina.comum.Contato;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "cliente")
@Getter
@Setter
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private Contato contato = new Contato();

    /** Só dígitos do telefone, para a busca ignorar a máscara. */
    private String telefoneDigitos;
    private String documento;
    private String endereco;
    private String observacoes;
    private LocalDateTime anonimizadoEm;
    private LocalDateTime criadoEm = LocalDateTime.now();

    public boolean anonimizado() {
        return anonimizadoEm != null;
    }
}
