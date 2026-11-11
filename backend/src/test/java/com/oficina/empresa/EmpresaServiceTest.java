package com.oficina.empresa;

import static org.junit.jupiter.api.Assertions.*;

import com.oficina.erro.NegocioException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class EmpresaServiceTest {
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
    static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};

    @TempDir Path tmp;

    private EmpresaService servico() {
        return new EmpresaService(null, tmp.toString());
    }

    private MockMultipartFile arq(byte[] b, String nome, String tipo) {
        return new MockMultipartFile("arquivo", nome, tipo, b);
    }

    @Test
    void aceitaPngEJpgPelaAssinatura() {
        assertEquals("logo.png", servico().gravar("logo", arq(PNG, "a.png", "image/png"), 1000));
        assertEquals("logo.jpg", servico().gravar("logo", arq(JPG, "a.jpg", "image/jpeg"), 1000));
        assertFalse(tmp.resolve("logo.png").toFile().exists(), "a versão antiga deve ser apagada");
    }

    @Test
    void ignoraExtensaoEContentTypeEnviados() {
        // um SVG que se declara PNG continua sendo recusado
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>".getBytes();
        assertThrows(NegocioException.class, () -> servico().gravar("logo", arq(svg, "a.png", "image/png"), 1000));
    }

    @Test
    void recusaArquivoVazioEMuitoGrande() {
        assertThrows(NegocioException.class, () -> servico().gravar("logo", arq(new byte[0], "a.png", "image/png"), 1000));
        byte[] grande = new byte[2000];
        System.arraycopy(PNG, 0, grande, 0, PNG.length);
        assertThrows(NegocioException.class, () -> servico().gravar("logo", arq(grande, "a.png", "image/png"), 1000));
    }

    @Test
    void tipoDeConteudoPelaExtensaoGravada() {
        assertEquals("image/png", EmpresaService.tipoDe("fundo.png"));
        assertEquals("image/jpeg", EmpresaService.tipoDe("fundo.jpg"));
    }
}
