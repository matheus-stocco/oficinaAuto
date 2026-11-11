package com.oficina.orcamentos;

import com.oficina.ordens.OsDto;
import com.oficina.veiculos.Placa;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orcamentos")
public class OrcamentoController {
    private final OrcamentoService service;
    private final OrcamentoPdfService pdf;

    public OrcamentoController(OrcamentoService service, OrcamentoPdfService pdf) {
        this.service = service;
        this.pdf = pdf;
    }

    @GetMapping
    Page<OrcamentoResumoDto> listar(@RequestParam(required = false) String q,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        String texto = q == null ? "" : q.trim();
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "id"));
        return service.listar(texto, Placa.normalizar(texto), pageable);
    }

    @GetMapping("/{id}")
    OrcamentoDto ver(@PathVariable Long id) {
        return service.ver(id);
    }

    @PostMapping
    OrcamentoDto criar(@Valid @RequestBody OrcamentoForm form) {
        return service.criar(form);
    }

    @PutMapping("/{id}")
    OrcamentoDto atualizar(@PathVariable Long id, @Valid @RequestBody OrcamentoForm form) {
        return service.atualizar(id, form);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }

    /** Abre no navegador (inline). A primeira abertura bem-sucedida começa o prazo de expiração. */
    @GetMapping("/{id}/pdf")
    ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "inline; filename=\"orcamento-" + id + ".pdf\"")
                .header("X-Content-Type-Options", "nosniff").body(pdf.gerar(id));
    }

    @PostMapping("/{id}/criar-os")
    OsDto criarOs(@PathVariable Long id, @Valid @RequestBody(required = false) ConversaoForm conversao) {
        return service.criarOs(id, conversao);
    }
}
