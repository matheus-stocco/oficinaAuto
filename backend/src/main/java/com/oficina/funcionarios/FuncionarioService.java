package com.oficina.funcionarios;

import com.oficina.erro.NegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FuncionarioService {
    static final int SENHA_MINIMA = 8;

    private final FuncionarioRepository repo;
    private final PasswordEncoder encoder;

    public FuncionarioService(FuncionarioRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @Transactional
    public Funcionario criar(FuncionarioForm form) {
        Funcionario f = new Funcionario();
        aplicar(f, form);
        return repo.save(f);
    }

    @Transactional
    public Funcionario atualizar(Long id, FuncionarioForm form) {
        Funcionario f = buscar(id);
        aplicar(f, form);
        garantirAcesso(f);
        return f;
    }

    @Transactional
    public Funcionario definirAtivo(Long id, boolean ativo) {
        Funcionario f = buscar(id);
        f.setAtivo(ativo);
        garantirAcesso(f);
        return f;
    }

    public Funcionario buscar(Long id) {
        return repo.findById(id).orElseThrow(
                () -> new NegocioException(HttpStatus.NOT_FOUND, "Funcionário não encontrado."));
    }

    /** Valida a senha nova. Usada também na troca de senha. */
    public void validarSenha(String senha) {
        if (senha == null || senha.length() < SENHA_MINIMA) {
            throw new NegocioException(HttpStatus.BAD_REQUEST,
                    "A senha precisa ter pelo menos " + SENHA_MINIMA + " caracteres.");
        }
    }

    private void aplicar(Funcionario f, FuncionarioForm form) {
        f.getContato().setNome(form.nome().trim());
        f.getContato().setTelefone(vazioParaNulo(form.telefone()));
        f.getContato().setEmail(vazioParaNulo(form.email()));
        f.setCpf(vazioParaNulo(form.cpf()));

        String login = vazioParaNulo(form.login());
        if (login == null) {
            f.setLogin(null);
            f.setSenhaHash(null);
            return;
        }
        repo.buscarPorLogin(login).filter(o -> !o.getId().equals(f.getId())).ifPresent(o -> {
            throw new NegocioException(HttpStatus.CONFLICT, "Esse login já está em uso.");
        });
        f.setLogin(login);
        String senha = vazioParaNulo(form.senha());
        if (senha != null || f.getSenhaHash() == null) {
            validarSenha(senha);
            f.setSenhaHash(encoder.encode(senha));
            f.setTentativasFalhas(0);
            f.setBloqueadoAte(null);
        }
    }

    /** Sempre precisa sobrar ao menos um funcionário ativo com acesso. */
    private void garantirAcesso(Funcionario f) {
        boolean continuaComAcesso = f.isAtivo() && f.temAcesso();
        if (!continuaComAcesso && f.getId() != null && repo.contarOutrosComAcesso(f.getId()) == 0) {
            throw new NegocioException(HttpStatus.CONFLICT,
                    "Não é possível: precisa existir pelo menos um funcionário ativo com acesso.");
        }
    }

    private static String vazioParaNulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
