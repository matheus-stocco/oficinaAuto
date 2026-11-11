package com.oficina.empresa;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/empresa")
public class EmpresaController {
    private final EmpresaService service;

    public EmpresaController(EmpresaService service) {
        this.service = service;
    }

    @GetMapping
    EmpresaDto ver() {
        return EmpresaDto.de(service.buscar());
    }

    @PutMapping
    EmpresaDto atualizar(@Valid @RequestBody EmpresaForm form) {
        return EmpresaDto.de(service.atualizar(form));
    }

    @PostMapping("/logo")
    EmpresaDto enviarLogo(@RequestParam("arquivo") MultipartFile arquivo) {
        return EmpresaDto.de(service.salvarLogo(arquivo));
    }

    @DeleteMapping("/logo")
    EmpresaDto removerLogo() {
        return EmpresaDto.de(service.removerLogo());
    }

    @PostMapping("/fundo")
    EmpresaDto enviarFundo(@RequestParam("arquivo") MultipartFile arquivo) {
        return EmpresaDto.de(service.salvarFundo(arquivo));
    }

    @DeleteMapping("/fundo")
    EmpresaDto removerFundo() {
        return EmpresaDto.de(service.removerFundo());
    }
}
