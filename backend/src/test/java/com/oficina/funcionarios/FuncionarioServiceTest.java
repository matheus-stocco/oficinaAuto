package com.oficina.funcionarios;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.oficina.erro.NegocioException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class FuncionarioServiceTest {
    FuncionarioRepository repo = mock(FuncionarioRepository.class);
    FuncionarioService service = new FuncionarioService(repo, new BCryptPasswordEncoder());

    @BeforeEach
    void salvaDevolveOMesmo() {
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private Funcionario comAcesso(long id) {
        Funcionario f = new Funcionario();
        f.setId(id);
        f.setLogin("ana");
        f.setSenhaHash("hash");
        return f;
    }

    @Test
    void criaSemAcessoQuandoLoginVazio() {
        Funcionario f = service.criar(new FuncionarioForm("Mecânico", null, null, null, " ", null));
        assertFalse(f.temAcesso());
        assertNull(f.getSenhaHash());
    }

    @Test
    void criaComAcessoGravandoSenhaEmHash() {
        Funcionario f = service.criar(new FuncionarioForm("Ana", null, null, null, "ana", "senha-boa-123"));
        assertTrue(f.temAcesso());
        assertNotEquals("senha-boa-123", f.getSenhaHash());
    }

    @Test
    void recusaSenhaCurta() {
        assertThrows(NegocioException.class,
                () -> service.criar(new FuncionarioForm("Ana", null, null, null, "ana", "curta")));
    }

    @Test
    void recusaLoginRepetido() {
        Funcionario outro = comAcesso(9L);
        when(repo.buscarPorLogin("ana")).thenReturn(Optional.of(outro));
        assertThrows(NegocioException.class,
                () -> service.criar(new FuncionarioForm("Ana 2", null, null, null, "ana", "senha-boa-123")));
    }

    @Test
    void naoInativaOUltimoComAcesso() {
        Funcionario f = comAcesso(1L);
        when(repo.findById(1L)).thenReturn(Optional.of(f));
        when(repo.contarOutrosComAcesso(1L)).thenReturn(0L);
        assertThrows(NegocioException.class, () -> service.definirAtivo(1L, false));
    }

    @Test
    void inativaQuandoExisteOutroComAcesso() {
        Funcionario f = comAcesso(1L);
        when(repo.findById(1L)).thenReturn(Optional.of(f));
        when(repo.contarOutrosComAcesso(1L)).thenReturn(1L);
        assertFalse(service.definirAtivo(1L, false).isAtivo());
    }

    @Test
    void naoRemoveOAcessoDoUltimo() {
        Funcionario f = comAcesso(1L);
        f.setSenhaHash("hash");
        when(repo.findById(1L)).thenReturn(Optional.of(f));
        when(repo.contarOutrosComAcesso(1L)).thenReturn(0L);
        assertThrows(NegocioException.class,
                () -> service.atualizar(1L, new FuncionarioForm("Ana", null, null, null, null, null)));
    }
}
