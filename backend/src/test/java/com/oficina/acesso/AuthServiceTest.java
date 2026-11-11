package com.oficina.acesso;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.oficina.erro.NegocioException;
import com.oficina.funcionarios.Funcionario;
import com.oficina.funcionarios.FuncionarioRepository;
import com.oficina.funcionarios.FuncionarioService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AuthServiceTest {
    BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
    FuncionarioRepository repo = mock(FuncionarioRepository.class);
    AuthService auth = new AuthService(repo, mock(FuncionarioService.class), enc);

    private Funcionario ana() {
        Funcionario f = new Funcionario();
        f.setId(1L);
        f.setLogin("ana");
        f.setSenhaHash(enc.encode("certa-123456"));
        when(repo.buscarPorLogin("ana")).thenReturn(Optional.of(f));
        return f;
    }

    private HttpStatus statusDe(Runnable r) {
        return assertThrows(NegocioException.class, r::run).getStatus();
    }

    @Test
    void senhaCertaZeraContadorEEntra() {
        Funcionario f = ana();
        f.setTentativasFalhas(3);
        assertSame(f, auth.entrar("ana", "certa-123456"));
        assertEquals(0, f.getTentativasFalhas());
    }

    @Test
    void senhaErradaContaFalhaEBloqueiaNaQuinta() {
        Funcionario f = ana();
        for (int i = 0; i < AuthService.MAX_TENTATIVAS; i++) {
            assertEquals(HttpStatus.UNAUTHORIZED, statusDe(() -> auth.entrar("ana", "errada")));
        }
        assertNotNull(f.getBloqueadoAte());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, statusDe(() -> auth.entrar("ana", "certa-123456")));
    }

    @Test
    void bloqueioVencidoPermiteEntrar() {
        Funcionario f = ana();
        f.setBloqueadoAte(LocalDateTime.now().minusMinutes(1));
        assertSame(f, auth.entrar("ana", "certa-123456"));
        assertNull(f.getBloqueadoAte());
    }

    @Test
    void usuarioInativoOuInexistenteRecebeMesmaMensagem() {
        Funcionario f = ana();
        f.setAtivo(false);
        assertEquals(HttpStatus.UNAUTHORIZED, statusDe(() -> auth.entrar("ana", "certa-123456")));
        assertEquals(HttpStatus.UNAUTHORIZED, statusDe(() -> auth.entrar("ninguem", "x")));
    }
}
