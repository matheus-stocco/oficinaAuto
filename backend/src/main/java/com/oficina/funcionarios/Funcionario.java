package com.oficina.funcionarios;

import com.oficina.comum.Contato;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "funcionario")
@Getter
@Setter
public class Funcionario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private Contato contato = new Contato();

    private String cpf;
    private boolean ativo = true;

    /** Nulo = funcionário sem acesso ao sistema. */
    private String login;
    private String senhaHash;

    private int tentativasFalhas;
    private LocalDateTime bloqueadoAte;
    private LocalDateTime criadoEm = LocalDateTime.now();

    public boolean temAcesso() {
        return login != null;
    }
}
