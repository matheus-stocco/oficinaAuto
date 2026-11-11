package com.oficina.clientes;

import com.oficina.auditoria.AuditoriaService;
import com.oficina.erro.NegocioException;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteService {
    private final ClienteRepository repo;
    private final AuditoriaService auditoria;

    public ClienteService(ClienteRepository repo, AuditoriaService auditoria) {
        this.repo = repo;
        this.auditoria = auditoria;
    }

    @Transactional
    public Cliente criar(ClienteForm form) {
        Cliente c = new Cliente();
        aplicar(c, form);
        return repo.save(c);
    }

    @Transactional
    public Cliente atualizar(Long id, ClienteForm form) {
        Cliente c = buscar(id);
        if (c.anonimizado()) {
            throw new NegocioException(HttpStatus.CONFLICT, "Cliente anonimizado não pode ser editado.");
        }
        aplicar(c, form);
        return c;
    }

    public Cliente buscar(Long id) {
        return repo.findById(id).orElseThrow(
                () -> new NegocioException(HttpStatus.NOT_FOUND, "Cliente não encontrado."));
    }

    /**
     * LGPD: troca os dados pessoais por valores neutros e mantém o registro (e as OS, valores e veículos
     * ligados a ele) para a contabilidade. É irreversível.
     */
    @Transactional
    public Cliente anonimizar(Long id) {
        Cliente c = buscar(id);
        if (c.anonimizado()) return c;
        c.getContato().setNome("Cliente anonimizado");
        c.getContato().setTelefone(null);
        c.getContato().setEmail(null);
        c.setTelefoneDigitos(null);
        c.setDocumento(null);
        c.setEndereco(null);
        c.setObservacoes(null);
        c.setAnonimizadoEm(LocalDateTime.now());
        auditoria.registrar("CLIENTE_ANONIMIZADO", "cliente " + id);
        return c;
    }

    static String digitos(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    private void aplicar(Cliente c, ClienteForm f) {
        c.getContato().setNome(f.nome().trim());
        c.getContato().setTelefone(f.telefone().trim());
        c.setTelefoneDigitos(digitos(f.telefone()));
        c.getContato().setEmail(vazioParaNulo(f.email()));
        c.setDocumento(vazioParaNulo(f.documento()));
        c.setEndereco(vazioParaNulo(f.endereco()));
        c.setObservacoes(vazioParaNulo(f.observacoes()));
    }

    private static String vazioParaNulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
