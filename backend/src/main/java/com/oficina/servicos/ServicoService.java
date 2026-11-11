package com.oficina.servicos;

import com.oficina.erro.NegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoService {
    private final ServicoRepository repo;

    public ServicoService(ServicoRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public Servico criar(ServicoForm form) {
        Servico s = new Servico();
        aplicar(s, form);
        return repo.save(s);
    }

    @Transactional
    public Servico atualizar(Long id, ServicoForm form) {
        Servico s = buscar(id);
        aplicar(s, form);
        return s;
    }

    /** Serviço nunca é excluído: inativo some das listas novas mas continua nas OS antigas. */
    @Transactional
    public Servico definirAtivo(Long id, boolean ativo) {
        Servico s = buscar(id);
        s.setAtivo(ativo);
        return s;
    }

    public Servico buscar(Long id) {
        return repo.findById(id).orElseThrow(
                () -> new NegocioException(HttpStatus.NOT_FOUND, "Serviço não encontrado."));
    }

    private void aplicar(Servico s, ServicoForm f) {
        String nome = f.nome().trim();
        repo.porNome(nome).filter(o -> !o.getId().equals(s.getId())).ifPresent(o -> {
            throw new NegocioException(HttpStatus.CONFLICT, "Já existe um serviço com esse nome.");
        });
        s.setNome(nome);
        s.setPrecoPadrao(f.precoPadrao());
    }
}
