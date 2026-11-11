package com.oficina.clientes;

public record ClienteDto(Long id, String nome, String telefone, String email, String documento,
                         String endereco, String observacoes, boolean anonimizado) {
    static ClienteDto de(Cliente c) {
        return new ClienteDto(c.getId(), c.getContato().getNome(), c.getContato().getTelefone(),
                c.getContato().getEmail(), c.getDocumento(), c.getEndereco(), c.getObservacoes(), c.anonimizado());
    }
}
