package com.oficina.pdf;

import static org.junit.jupiter.api.Assertions.*;

import com.oficina.empresa.Empresa;
import com.oficina.erro.NegocioException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CabecalhoPdfTest {
    private Empresa empresa(String cnpj) {
        Empresa e = new Empresa();
        e.setNome("Auto Center Silva");
        e.setCnpj(cnpj);
        return e;
    }

    @Test
    void semLogoENemCnpjDizOQueFalta() {
        NegocioException e = assertThrows(NegocioException.class, () -> CabecalhoPdf.exigirDados(empresa(null), null));
        assertEquals(409, e.getStatus().value());
        assertTrue(e.getMessage().contains("logo") && e.getMessage().contains("CNPJ"), e.getMessage());
        assertTrue(e.getMessage().startsWith("Para gerar o PDF, preencha em Configurações:"));
    }

    @Test
    void faltandoSoOCnpjOuSoOLogoApontaApenasEle() {
        assertTrue(assertThrows(NegocioException.class, () -> CabecalhoPdf.exigirDados(empresa(null), new byte[]{1}))
                .getMessage().contains("CNPJ"));
        String semLogo = assertThrows(NegocioException.class, () -> CabecalhoPdf.exigirDados(empresa("12345678000190"), null)).getMessage();
        assertTrue(semLogo.contains("logo") && !semLogo.contains("CNPJ"));
    }

    @Test
    void comLogoECnpjPassaETelefoneEEnderecoSaoOpcionais() {
        assertDoesNotThrow(() -> CabecalhoPdf.exigirDados(empresa("12345678000190"), new byte[]{1}));
    }

    @Test
    void formataCnpjELeavaOutrosTamanhosComoEstao() {
        assertEquals("12.345.678/0001-90", CabecalhoPdf.cnpj("12345678000190"));
        assertEquals("123", CabecalhoPdf.cnpj("123"));
    }

    @Test
    void formataMoedaEDataNoPadraoBrasileiroSemEspacoEspecial() {
        assertEquals("R$ 1.234,56", Formato.moeda(new BigDecimal("1234.56")));
        assertFalse(Formato.moeda(BigDecimal.TEN).contains(" "));
        assertEquals("05/03/2026", Formato.data(LocalDate.of(2026, 3, 5)));
        assertEquals("", Formato.data(null));
    }
}
