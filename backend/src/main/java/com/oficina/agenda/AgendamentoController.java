package com.oficina.agenda;

import com.oficina.erro.NegocioException;
import com.oficina.ordens.CadastrosNaHora;
import com.oficina.ordens.OsDto;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agendamentos")
public class AgendamentoController {
    record MudancaStatus(String status) {}

    private final AgendamentoService service;

    public AgendamentoController(AgendamentoService service) {
        this.service = service;
    }

    /** Calendário: devolve só o período visível (no máximo 62 dias). */
    @GetMapping("/calendario")
    List<AgendamentoResumoDto> calendario(@RequestParam LocalDate de, @RequestParam LocalDate ate) {
        return service.calendario(de, ate);
    }

    /** Consulta posterior: paginada, com busca e filtros de período e situação. */
    @GetMapping
    Page<AgendamentoResumoDto> consultar(@RequestParam(required = false) String q,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(required = false) LocalDate de,
                                         @RequestParam(required = false) LocalDate ate,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Order.desc("data"), Sort.Order.desc("hora"), Sort.Order.desc("id")));
        return service.consultar(q, status == null || status.isBlank() ? null : estado(status), de, ate, pageable);
    }

    @GetMapping("/{id}")
    AgendamentoDto ver(@PathVariable Long id) {
        return service.ver(id);
    }

    @PostMapping
    AgendamentoDto criar(@Valid @RequestBody AgendamentoForm form) {
        return service.criar(form);
    }

    @PutMapping("/{id}")
    AgendamentoDto atualizar(@PathVariable Long id, @Valid @RequestBody AgendamentoForm form) {
        return service.atualizar(id, form);
    }

    @PatchMapping("/{id}/status")
    AgendamentoDto status(@PathVariable Long id, @RequestBody MudancaStatus corpo) {
        return service.mudarStatus(id, estado(corpo.status()));
    }

    @PostMapping("/{id}/criar-os")
    OsDto criarOs(@PathVariable Long id, @Valid @RequestBody(required = false) CadastrosNaHora novos) {
        return service.criarOs(id, novos);
    }

    private static StatusAgendamento estado(String s) {
        try {
            return StatusAgendamento.valueOf(s == null ? "" : s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Situação inválida.");
        }
    }
}
