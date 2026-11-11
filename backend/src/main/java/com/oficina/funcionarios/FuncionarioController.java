package com.oficina.funcionarios;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/funcionarios")
public class FuncionarioController {
    private final FuncionarioService service;
    private final FuncionarioRepository repo;

    public FuncionarioController(FuncionarioService service, FuncionarioRepository repo) {
        this.service = service;
        this.repo = repo;
    }

    @GetMapping
    Page<FuncionarioDto> listar(@RequestParam(required = false) String q,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by("contato.nome"));
        return repo.buscar(q == null ? "" : q.trim(), pageable).map(FuncionarioDto::de);
    }

    @GetMapping("/{id}")
    FuncionarioDto ver(@PathVariable Long id) {
        return FuncionarioDto.de(service.buscar(id));
    }

    @PostMapping
    FuncionarioDto criar(@Valid @RequestBody FuncionarioForm form) {
        return FuncionarioDto.de(service.criar(form));
    }

    @PutMapping("/{id}")
    FuncionarioDto atualizar(@PathVariable Long id, @Valid @RequestBody FuncionarioForm form) {
        return FuncionarioDto.de(service.atualizar(id, form));
    }

    @PatchMapping("/{id}/ativo")
    FuncionarioDto ativo(@PathVariable Long id, @RequestBody Map<String, Boolean> corpo) {
        return FuncionarioDto.de(service.definirAtivo(id, Boolean.TRUE.equals(corpo.get("ativo"))));
    }
}
