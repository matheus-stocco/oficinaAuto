package com.oficina.acesso;

import com.oficina.erro.NegocioException;
import com.oficina.funcionarios.Funcionario;
import com.oficina.funcionarios.FuncionarioRepository;
import com.oficina.funcionarios.FuncionarioService;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    static final int MAX_TENTATIVAS = 5;
    static final Duration BLOQUEIO = Duration.ofMinutes(15);
    private static final String INVALIDO = "Usuário ou senha inválidos.";

    private final FuncionarioRepository repo;
    private final FuncionarioService funcionarios;
    private final PasswordEncoder encoder;

    public AuthService(FuncionarioRepository repo, FuncionarioService funcionarios, PasswordEncoder encoder) {
        this.repo = repo;
        this.funcionarios = funcionarios;
        this.encoder = encoder;
    }

    /** noRollbackFor: o contador de erros precisa ser gravado mesmo quando o login falha. */
    @Transactional(noRollbackFor = NegocioException.class)
    public Funcionario entrar(String login, String senha) {
        Funcionario f = repo.buscarPorLogin(login == null ? "" : login.trim())
                .filter(x -> x.isAtivo() && x.temAcesso())
                .orElseThrow(() -> new NegocioException(HttpStatus.UNAUTHORIZED, INVALIDO));

        LocalDateTime agora = LocalDateTime.now();
        if (f.getBloqueadoAte() != null && f.getBloqueadoAte().isAfter(agora)) {
            long min = Math.max(1, Duration.between(agora, f.getBloqueadoAte()).toMinutes() + 1);
            throw new NegocioException(HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas tentativas. Tente de novo em " + min + " minuto(s).");
        }
        if (senha == null || !encoder.matches(senha, f.getSenhaHash())) {
            f.setTentativasFalhas(f.getTentativasFalhas() + 1);
            if (f.getTentativasFalhas() >= MAX_TENTATIVAS) {
                f.setBloqueadoAte(agora.plus(BLOQUEIO));
                f.setTentativasFalhas(0);
            }
            throw new NegocioException(HttpStatus.UNAUTHORIZED, INVALIDO);
        }
        f.setTentativasFalhas(0);
        f.setBloqueadoAte(null);
        return f;
    }

    @Transactional
    public void trocarSenha(Long id, String senhaAtual, String novaSenha) {
        Funcionario f = funcionarios.buscar(id);
        if (senhaAtual == null || !encoder.matches(senhaAtual, f.getSenhaHash())) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "A senha atual está incorreta.");
        }
        funcionarios.validarSenha(novaSenha);
        f.setSenhaHash(encoder.encode(novaSenha));
    }
}
