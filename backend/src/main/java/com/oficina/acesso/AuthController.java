package com.oficina.acesso;

import com.oficina.funcionarios.Funcionario;
import com.oficina.funcionarios.FuncionarioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    record LoginForm(@NotBlank(message = "Informe o usuário.") String login,
                     @NotBlank(message = "Informe a senha.") String senha) {}
    record TrocaSenhaForm(String senhaAtual, String novaSenha) {}
    record Eu(Long id, String nome, String login) {
        static Eu de(Funcionario f) {
            return new Eu(f.getId(), f.getContato().getNome(), f.getLogin());
        }
    }

    private final AuthService auth;
    private final JwtService jwt;
    private final FuncionarioService funcionarios;
    private final boolean cookieSeguro;

    public AuthController(AuthService auth, JwtService jwt, FuncionarioService funcionarios,
                          @Value("${app.cookie.secure:true}") boolean cookieSeguro) {
        this.auth = auth;
        this.jwt = jwt;
        this.funcionarios = funcionarios;
        this.cookieSeguro = cookieSeguro;
    }

    @PostMapping("/login")
    ResponseEntity<Eu> login(@Valid @RequestBody LoginForm form) {
        Funcionario f = auth.entrar(form.login(), form.senha());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie(jwt.gerar(f.getId()), JwtService.VALIDADE).toString())
                .body(Eu.de(f));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString()).build();
    }

    @GetMapping("/eu")
    Eu eu(Authentication a) {
        return Eu.de(funcionarios.buscar((Long) a.getPrincipal()));
    }

    @PostMapping("/trocar-senha")
    ResponseEntity<Map<String, String>> trocarSenha(Authentication a, @RequestBody TrocaSenhaForm form) {
        auth.trocarSenha((Long) a.getPrincipal(), form.senhaAtual(), form.novaSenha());
        return ResponseEntity.ok(Map.of("mensagem", "Senha alterada."));
    }

    private ResponseCookie cookie(String valor, Duration duracao) {
        return ResponseCookie.from(JwtFilter.COOKIE, valor).httpOnly(true).secure(cookieSeguro)
                .sameSite("Strict").path("/").maxAge(duracao).build();
    }
}
