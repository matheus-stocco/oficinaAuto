package com.oficina.servicos;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.oficina.erro.NegocioException;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServicoServiceTest {
    ServicoRepository repo = mock(ServicoRepository.class);
    ServicoService service = new ServicoService(repo);

    @BeforeEach
    void salvaDevolveOMesmo() {
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void criaComNomeSemEspacosEPrecoExato() {
        Servico s = service.criar(new ServicoForm("  Balanceamento ", new BigDecimal("80.50")));
        assertEquals("Balanceamento", s.getNome());
        assertEquals(new BigDecimal("80.50"), s.getPrecoPadrao());
        assertTrue(s.isAtivo());
    }

    @Test
    void recusaNomeRepetidoDeOutroServico() {
        Servico outro = new Servico();
        outro.setId(9L);
        when(repo.porNome("Balanceamento")).thenReturn(Optional.of(outro));
        assertEquals(409, assertThrows(NegocioException.class,
                () -> service.criar(new ServicoForm("Balanceamento", BigDecimal.TEN))).getStatus().value());
    }

    @Test
    void permiteSalvarOMesmoServicoComOMesmoNome() {
        Servico s = new Servico();
        s.setId(3L);
        when(repo.findById(3L)).thenReturn(Optional.of(s));
        when(repo.porNome("Alinhamento")).thenReturn(Optional.of(s));
        assertEquals(new BigDecimal("120.00"),
                service.atualizar(3L, new ServicoForm("Alinhamento", new BigDecimal("120.00"))).getPrecoPadrao());
    }

    @Test
    void inativarENaoExcluir() {
        Servico s = new Servico();
        s.setId(3L);
        when(repo.findById(3L)).thenReturn(Optional.of(s));
        assertFalse(service.definirAtivo(3L, false).isAtivo());
        verify(repo, never()).delete(any());
        verify(repo, never()).deleteById(any());
    }
}
