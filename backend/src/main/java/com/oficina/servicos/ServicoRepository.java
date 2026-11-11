package com.oficina.servicos;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServicoRepository extends JpaRepository<Servico, Long> {

    @Query("select s from Servico s where lower(s.nome) = lower(:nome)")
    Optional<Servico> porNome(@Param("nome") String nome);

    /** somenteAtivos = true é o que as telas de OS, orçamento e agenda usam para montar listas. */
    @Query("""
            select s from Servico s
            where lower(s.nome) like lower(concat('%', :q, '%'))
              and (:somenteAtivos = false or s.ativo = true)
            """)
    Page<Servico> buscar(@Param("q") String q, @Param("somenteAtivos") boolean somenteAtivos, Pageable pageable);
}
