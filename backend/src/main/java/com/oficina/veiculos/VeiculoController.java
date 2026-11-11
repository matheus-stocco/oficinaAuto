package com.oficina.veiculos;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/veiculos")
public class VeiculoController {
    private final VeiculoService service;
    private final VeiculoRepository repo;

    public VeiculoController(VeiculoService service, VeiculoRepository repo) {
        this.service = service;
        this.repo = repo;
    }

    @GetMapping
    Page<VeiculoDto> listar(@RequestParam(required = false) String q,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("modelo"));
        String texto = q == null ? "" : q.trim();
        return repo.buscar(texto, Placa.normalizar(texto), pageable).map(VeiculoDto::de);
    }

    @GetMapping("/{id}")
    VeiculoDto ver(@PathVariable Long id) {
        return VeiculoDto.de(service.buscar(id));
    }

    @PostMapping
    VeiculoDto criar(@Valid @RequestBody VeiculoForm form) {
        return VeiculoDto.de(service.criar(form));
    }

    @PutMapping("/{id}")
    VeiculoDto atualizar(@PathVariable Long id, @Valid @RequestBody VeiculoForm form) {
        return VeiculoDto.de(service.atualizar(id, form));
    }
}
