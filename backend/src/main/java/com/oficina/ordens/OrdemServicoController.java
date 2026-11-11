package com.oficina.ordens;

import com.oficina.erro.NegocioException;
import com.oficina.veiculos.Placa;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ordens")
public class OrdemServicoController {
    record MudancaStatus(String status, boolean confirmar) {}

    private final OrdemServicoService service;
    private final OrdemServicoRepository repo;

    public OrdemServicoController(OrdemServicoService service, OrdemServicoRepository repo) {
        this.service = service;
        this.repo = repo;
    }

    @GetMapping
    Page<OsResumoDto> listar(@RequestParam(required = false) String status,
                             @RequestParam(required = false) String q,
                             @RequestParam(required = false) LocalDate de,
                             @RequestParam(required = false) LocalDate ate,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size) {
        String st = status == null || status.isBlank() ? "" : estado(status).name();
        String texto = q == null ? "" : q.trim();
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "id"));
        return repo.buscar(st, texto, Placa.normalizar(texto),
                (de == null ? LocalDate.of(2000, 1, 1) : de).atStartOfDay(),
                (ate == null ? LocalDate.of(2999, 1, 1) : ate.plusDays(1)).atStartOfDay(), pageable)
                .map(OsResumoDto::de);
    }

    @GetMapping("/{id}")
    OsDto ver(@PathVariable Long id) {
        return service.ver(id);
    }

    @PostMapping
    OsDto criar(@Valid @RequestBody OsForm form) {
        return service.criar(form);
    }

    @PutMapping("/{id}")
    OsDto atualizar(@PathVariable Long id, @Valid @RequestBody OsForm form) {
        return service.atualizar(id, form);
    }

    @PatchMapping("/{id}/status")
    OsDto status(@PathVariable Long id, @RequestBody MudancaStatus corpo) {
        return service.mudarStatus(id, estado(corpo.status()), corpo.confirmar());
    }

    private static StatusOs estado(String s) {
        try {
            return StatusOs.valueOf(s == null ? "" : s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Status inválido.");
        }
    }
}
