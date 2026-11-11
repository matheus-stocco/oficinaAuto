package com.oficina.veiculos;

import com.oficina.clientes.ClienteService;
import com.oficina.erro.NegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VeiculoService {
    private final VeiculoRepository repo;
    private final ClienteService clientes;

    public VeiculoService(VeiculoRepository repo, ClienteService clientes) {
        this.repo = repo;
        this.clientes = clientes;
    }

    @Transactional
    public Veiculo criar(VeiculoForm form) {
        Veiculo v = new Veiculo();
        aplicar(v, form);
        return repo.save(v);
    }

    @Transactional
    public Veiculo atualizar(Long id, VeiculoForm form) {
        Veiculo v = buscar(id);
        aplicar(v, form);
        return v;
    }

    public Veiculo buscar(Long id) {
        return repo.findById(id).orElseThrow(
                () -> new NegocioException(HttpStatus.NOT_FOUND, "Veículo não encontrado."));
    }

    private void aplicar(Veiculo v, VeiculoForm f) {
        String placa = Placa.validar(f.placa());
        if (placa != null) {
            repo.findByPlaca(placa).filter(o -> !o.getId().equals(v.getId())).ifPresent(o -> {
                throw new NegocioException(HttpStatus.CONFLICT, "Já existe um veículo com essa placa.");
            });
        }
        v.setPlaca(placa);
        v.setMarca(vazioParaNulo(f.marca()));
        v.setModelo(f.modelo().trim());
        v.setAno(vazioParaNulo(f.ano()));
        v.setCor(vazioParaNulo(f.cor()));
        v.setCliente(f.clienteId() == null ? null : clientes.buscar(f.clienteId()));
    }

    private static String vazioParaNulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
