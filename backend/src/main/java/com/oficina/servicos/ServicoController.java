package com.oficina.servicos;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/servicos")
public class ServicoController {
    private final ServicoService service;
    private final ServicoRepository repo;

    public ServicoController(ServicoService service, ServicoRepository repo) {
        this.service = service;
        this.repo = repo;
    }

    @GetMapping
    Page<ServicoDto> listar(@RequestParam(required = false) String q,
                            @RequestParam(defaultValue = "false") boolean ativos,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("nome"));
        return repo.buscar(q == null ? "" : q.trim(), ativos, pageable).map(ServicoDto::de);
    }

    @GetMapping("/{id}")
    ServicoDto ver(@PathVariable Long id) {
        return ServicoDto.de(service.buscar(id));
    }

    @PostMapping
    ServicoDto criar(@Valid @RequestBody ServicoForm form) {
        return ServicoDto.de(service.criar(form));
    }

    @PutMapping("/{id}")
    ServicoDto atualizar(@PathVariable Long id, @Valid @RequestBody ServicoForm form) {
        return ServicoDto.de(service.atualizar(id, form));
    }

    @PatchMapping("/{id}/ativo")
    ServicoDto ativo(@PathVariable Long id, @RequestBody Map<String, Boolean> corpo) {
        return ServicoDto.de(service.definirAtivo(id, Boolean.TRUE.equals(corpo.get("ativo"))));
    }
}
