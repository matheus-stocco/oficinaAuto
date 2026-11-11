package com.oficina.acesso;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    static final Duration VALIDADE = Duration.ofHours(8);

    private final SecretKey chave;

    public JwtService(@Value("${app.jwt.secret}") String segredo) {
        if (segredo.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET precisa ter pelo menos 32 caracteres.");
        }
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
    }

    public String gerar(Long funcionarioId) {
        Date agora = new Date();
        return Jwts.builder().subject(String.valueOf(funcionarioId)).issuedAt(agora)
                .expiration(new Date(agora.getTime() + VALIDADE.toMillis())).signWith(chave).compact();
    }

    /** Devolve o id do funcionário se o token for válido e não expirado. */
    public Optional<Long> ler(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(chave).build().parseSignedClaims(token).getPayload();
            return Optional.of(Long.valueOf(c.getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
