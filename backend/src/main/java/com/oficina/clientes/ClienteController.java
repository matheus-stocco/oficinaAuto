package com.oficina.clientes;

import com.oficina.veiculos.VeiculoDto;
import com.oficina.veiculos.VeiculoRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {
    record Exportacao(ClienteDto cliente, List<VeiculoDto> veiculos) {}

    private final ClienteService service;
    private final ClienteRepository repo;
    private final VeiculoRepository veiculos;

    public ClienteController(ClienteService service, ClienteRepository repo, VeiculoRepository veiculos) {
        this.service = service;
        this.repo = repo;
        this.veiculos = veiculos;
    }

    @GetMapping
    Page<ClienteDto> listar(@RequestParam(required = false) String q,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("contato.nome"));
        String texto = q == null ? "" : q.trim();
        return repo.buscar(texto, ClienteService.digitos(texto), pageable).map(ClienteDto::de);
    }

    @GetMapping("/{id}")
    ClienteDto ver(@PathVariable Long id) {
        return ClienteDto.de(service.buscar(id));
    }

    @PostMapping
    ClienteDto criar(@Valid @RequestBody ClienteForm form) {
        return ClienteDto.de(service.criar(form));
    }

    @PutMapping("/{id}")
    ClienteDto atualizar(@PathVariable Long id, @Valid @RequestBody ClienteForm form) {
        return ClienteDto.de(service.atualizar(id, form));
    }

    @GetMapping("/{id}/veiculos")
    List<VeiculoDto> veiculosDoCliente(@PathVariable Long id) {
        service.buscar(id);
        return veiculos.doCliente(id).stream().map(VeiculoDto::de).toList();
    }

    /** LGPD: direito de acesso. Devolve os dados do cliente e os veículos dele. */
    @GetMapping("/{id}/exportar")
    Exportacao exportar(@PathVariable Long id) {
        return new Exportacao(ClienteDto.de(service.buscar(id)),
                veiculos.doCliente(id).stream().map(VeiculoDto::de).toList());
    }

    /** LGPD: anonimização (irreversível). */
    @PostMapping("/{id}/anonimizar")
    ClienteDto anonimizar(@PathVariable Long id) {
        return ClienteDto.de(service.anonimizar(id));
    }
}
