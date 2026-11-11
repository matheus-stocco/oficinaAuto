package com.oficina.clientes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.oficina.auditoria.AuditoriaService;
import com.oficina.erro.NegocioException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClienteServiceTest {
    ClienteRepository repo = mock(ClienteRepository.class);
    AuditoriaService auditoria = mock(AuditoriaService.class);
    ClienteService service = new ClienteService(repo, auditoria);

    @BeforeEach
    void salvaDevolveOMesmo() {
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private ClienteForm form() {
        return new ClienteForm("  Maria Silva ", "(11) 98888-7777", "m@x.com", "123.456.789-00", "Rua A, 1", "obs");
    }

    @Test
    void guardaTelefoneEMesmoSoDigitosParaBusca() {
        Cliente c = service.criar(form());
        assertEquals("Maria Silva", c.getContato().getNome());
        assertEquals("(11) 98888-7777", c.getContato().getTelefone());
        assertEquals("11988887777", c.getTelefoneDigitos());
    }

    @Test
    void anonimizarApagaDadosPessoaisERegistraAuditoria() {
        Cliente c = service.criar(form());
        c.setId(7L);
        when(repo.findById(7L)).thenReturn(Optional.of(c));

        service.anonimizar(7L);

        assertTrue(c.anonimizado());
        assertEquals("Cliente anonimizado", c.getContato().getNome());
        assertNull(c.getContato().getTelefone());
        assertNull(c.getTelefoneDigitos());
        assertNull(c.getContato().getEmail());
        assertNull(c.getDocumento());
        assertNull(c.getEndereco());
        assertNull(c.getObservacoes());
        verify(auditoria).registrar("CLIENTE_ANONIMIZADO", "cliente 7");
    }

    @Test
    void anonimizarDeNovoNaoRepeteAuditoria() {
        Cliente c = service.criar(form());
        c.setId(7L);
        when(repo.findById(7L)).thenReturn(Optional.of(c));
        service.anonimizar(7L);
        service.anonimizar(7L);
        verify(auditoria, times(1)).registrar(any(), any());
    }

    @Test
    void naoEditaClienteAnonimizado() {
        Cliente c = service.criar(form());
        c.setId(7L);
        when(repo.findById(7L)).thenReturn(Optional.of(c));
        service.anonimizar(7L);
        assertThrows(NegocioException.class, () -> service.atualizar(7L, form()));
    }
}
