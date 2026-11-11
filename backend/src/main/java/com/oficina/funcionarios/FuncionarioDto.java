package com.oficina.funcionarios;

/** Saída: nunca expõe senha_hash nem contadores de bloqueio. */
public record FuncionarioDto(Long id, String nome, String telefone, String email, String cpf,
                             boolean ativo, boolean temAcesso, String login) {
    static FuncionarioDto de(Funcionario f) {
        return new FuncionarioDto(f.getId(), f.getContato().getNome(), f.getContato().getTelefone(),
                f.getContato().getEmail(), f.getCpf(), f.isAtivo(), f.temAcesso(), f.getLogin());
    }
}
