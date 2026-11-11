package com.oficina.veiculos;

import static org.junit.jupiter.api.Assertions.*;

import com.oficina.erro.NegocioException;
import org.junit.jupiter.api.Test;

class PlacaTest {
    @Test
    void normalizaMaiusculaESemSimbolos() {
        assertEquals("ABC1234", Placa.normalizar(" abc-1234 "));
    }

    @Test
    void aceitaFormatoAntigoEMercosul() {
        assertEquals("ABC1234", Placa.validar("abc-1234"));
        assertEquals("ABC1D23", Placa.validar("abc1d23"));
    }

    @Test
    void vazioSignificaSemPlaca() {
        assertNull(Placa.validar(null));
        assertNull(Placa.validar("  "));
    }

    @Test
    void recusaFormatoInvalido() {
        assertThrows(NegocioException.class, () -> Placa.validar("ABCD123"));
        assertThrows(NegocioException.class, () -> Placa.validar("AB12345"));
        assertThrows(NegocioException.class, () -> Placa.validar("ABC-12"));
    }
}
