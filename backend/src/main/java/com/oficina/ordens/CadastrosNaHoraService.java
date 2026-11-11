package com.oficina.ordens;

import com.oficina.clientes.Cliente;
import com.oficina.clientes.ClienteForm;
import com.oficina.clientes.ClienteService;
import com.oficina.veiculos.Veiculo;
import com.oficina.veiculos.VeiculoForm;
import com.oficina.veiculos.VeiculoService;
import org.springframework.stereotype.Service;

/**
 * Resolve cliente e veículo ao criar uma OS a partir de um orçamento ou de um agendamento: usa o que já está
 * cadastrado e, se foi pedido, cadastra o avulso. Roda dentro da transação de quem chama, então uma falha
 * (por exemplo, placa repetida) desfaz tudo, inclusive o cliente criado antes.
 */
@Service
public class CadastrosNaHoraService {
    public record Resolvido(Cliente cliente, Veiculo veiculo) {}

    private final ClienteService clientes;
    private final VeiculoService veiculos;

    public CadastrosNaHoraService(ClienteService clientes, VeiculoService veiculos) {
        this.clientes = clientes;
        this.veiculos = veiculos;
    }

    public Resolvido resolver(Cliente cliente, Veiculo veiculo, CadastrosNaHora novos) {
        if (cliente == null && novos != null && novos.cliente() != null) {
            cliente = clientes.criar(new ClienteForm(novos.cliente().nome(), novos.cliente().telefone(), null, null, null, null));
        }
        if (veiculo == null && novos != null && novos.veiculo() != null) {
            veiculo = veiculos.criar(new VeiculoForm(novos.veiculo().placa(), null, novos.veiculo().modelo(), null, null,
                    cliente == null ? null : cliente.getId()));
        }
        return new Resolvido(cliente, veiculo);
    }
}
