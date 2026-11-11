package com.oficina.veiculos;

public record VeiculoDto(Long id, String placa, String marca, String modelo, String ano, String cor,
                         Long clienteId, String clienteNome) {
    public static VeiculoDto de(Veiculo v) {
        var c = v.getCliente();
        return new VeiculoDto(v.getId(), v.getPlaca(), v.getMarca(), v.getModelo(), v.getAno(), v.getCor(),
                c == null ? null : c.getId(), c == null ? null : c.getContato().getNome());
    }
}
