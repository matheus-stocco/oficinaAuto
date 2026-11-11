package com.oficina.acesso;

import com.oficina.funcionarios.FuncionarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Lê o cookie, valida o token e confere no banco se o funcionário ainda está ativo e com acesso. */
@Component
public class JwtFilter extends OncePerRequestFilter {
    static final String COOKIE = "oficina_token";

    private final JwtService jwt;
    private final FuncionarioRepository repo;

    public JwtFilter(JwtService jwt, FuncionarioRepository repo) {
        this.jwt = jwt;
        this.repo = repo;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (req.getCookies() != null) {
            for (Cookie c : req.getCookies()) {
                if (COOKIE.equals(c.getName())) {
                    jwt.ler(c.getValue()).flatMap(repo::findById)
                            .filter(f -> f.isAtivo() && f.temAcesso())
                            .ifPresent(f -> SecurityContextHolder.getContext().setAuthentication(
                                    new UsernamePasswordAuthenticationToken(f.getId(), null,
                                            List.of(new SimpleGrantedAuthority("ACESSO")))));
                }
            }
        }
        chain.doFilter(req, res);
    }
}
