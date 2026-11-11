package com.oficina.acesso;

import com.oficina.funcionarios.FuncionarioForm;
import com.oficina.funcionarios.FuncionarioRepository;
import com.oficina.funcionarios.FuncionarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Cria o primeiro funcionário com acesso a partir de variáveis de ambiente (nunca senha fixa no código). */
@Component
public class AdminInicial implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminInicial.class);

    private final FuncionarioRepository repo;
    private final FuncionarioService service;
    private final String nome, login, senha;

    public AdminInicial(FuncionarioRepository repo, FuncionarioService service,
                        @Value("${app.admin.nome:Atendente}") String nome,
                        @Value("${app.admin.login:}") String login,
                        @Value("${app.admin.senha:}") String senha) {
        this.repo = repo;
        this.service = service;
        this.nome = nome;
        this.login = login;
        this.senha = senha;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repo.countByLoginIsNotNull() > 0) return;
        if (login.isBlank() || senha.isBlank()) {
            log.warn("Nenhum funcionário com acesso. Defina ADMIN_LOGIN e ADMIN_SENHA para criar o primeiro.");
            return;
        }
        service.criar(new FuncionarioForm(nome, null, null, null, login, senha));
        log.info("Primeiro funcionário com acesso criado (login: {}).", login);
    }
}
