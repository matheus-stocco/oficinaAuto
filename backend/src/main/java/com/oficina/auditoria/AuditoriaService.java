package com.oficina.auditoria;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
}

/** Registra quem fez o quê. Nunca guarde dado pessoal no detalhe (use só ids e rótulos). */
@Service
public class AuditoriaService {
    private final AuditoriaRepository repo;

    AuditoriaService(AuditoriaRepository repo) {
        this.repo = repo;
    }

    public void registrar(String acao, String detalhe) {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        Auditoria r = new Auditoria();
        r.setFuncionarioId(a != null && a.getPrincipal() instanceof Long id ? id : null);
        r.setAcao(acao);
        r.setDetalhe(detalhe);
        repo.save(r);
    }
}
