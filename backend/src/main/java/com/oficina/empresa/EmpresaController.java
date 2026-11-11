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
        return dto(service.buscar());
    }

    @PutMapping
    EmpresaDto atualizar(@Valid @RequestBody EmpresaForm form) {
        return dto(service.atualizar(form));
    }

    @PostMapping("/logo")
    EmpresaDto enviarLogo(@RequestParam("arquivo") MultipartFile arquivo) {
        return dto(service.salvarLogo(arquivo));
    }

    @DeleteMapping("/logo")
    EmpresaDto removerLogo() {
        return dto(service.removerLogo());
    }

    @PostMapping("/fundo")
    EmpresaDto enviarFundo(@RequestParam("arquivo") MultipartFile arquivo) {
        return dto(service.salvarFundo(arquivo));
    }

    @DeleteMapping("/fundo")
    EmpresaDto removerFundo() {
        return dto(service.removerFundo());
    }

    private EmpresaDto dto(Empresa e) {
        return EmpresaDto.de(e, service.existe(e.getLogoArquivo()), service.existe(e.getFundoArquivo()));
    }
}
