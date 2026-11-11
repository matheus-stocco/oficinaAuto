package com.oficina.financeiro;

import com.oficina.financeiro.CobrancaService.InfoDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CobrancaController {
    private final CobrancaService service;

    public CobrancaController(CobrancaService service) {
        this.service = service;
    }

    @GetMapping("/ordens/{id}/cobrancas")
    InfoDto info(@PathVariable Long id) {
        return service.info(id);
    }

    @PostMapping("/ordens/{id}/cobrancas")
    InfoDto criar(@PathVariable Long id, @Valid @RequestBody CobrancaForm form) {
        return service.criar(id, form);
    }

    @PostMapping("/cobrancas/{id}/cancelar")
    InfoDto cancelar(@PathVariable Long id) {
        return service.cancelar(id);
    }
}
