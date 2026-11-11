package com.oficina.empresa;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "empresa")
@Getter
@Setter
public class Empresa {
    @Id
    private Long id = 1L;

    private String nome;
    private String cnpj;
    private String telefone;
    private String endereco;
    private String logoArquivo;
    private String fundoTipo;
    private String fundoCor;
    private String fundoArquivo;
    private int orcamentoRascunhoHoras = 24;
    private LocalDateTime atualizadoEm = LocalDateTime.now();
}
