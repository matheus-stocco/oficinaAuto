package com.oficina.empresa;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rotas sem login: a tela de login precisa do nome, do logo e do fundo da oficina. */
@RestController
@RequestMapping("/api/publico")
public class PublicoController {
    private final EmpresaService service;

    public PublicoController(EmpresaService service) {
        this.service = service;
    }

    @GetMapping("/identidade")
    IdentidadeDto identidade() {
        Empresa e = service.buscar();
        return IdentidadeDto.de(e, service.existe(e.getLogoArquivo()), service.existe(e.getFundoArquivo()));
    }

    @GetMapping("/logo")
    ResponseEntity<byte[]> logo() {
        return imagem(service.buscar().getLogoArquivo());
    }

    @GetMapping("/fundo")
    ResponseEntity<byte[]> fundo() {
        return imagem(service.buscar().getFundoArquivo());
    }

    private ResponseEntity<byte[]> imagem(String arquivo) {
        byte[] bytes = service.ler(arquivo);
        if (bytes == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(EmpresaService.tipoDe(arquivo)))
                .cacheControl(CacheControl.noCache())
                .header("X-Content-Type-Options", "nosniff").body(bytes);
    }
}
