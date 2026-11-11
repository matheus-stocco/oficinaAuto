package com.oficina.clientes;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    /** q = texto digitado; d = só os dígitos dele (vazio quando não há número, para não casar tudo). */
    @Query("""
            select c from Cliente c
            where lower(c.contato.nome) like lower(concat('%', :q, '%'))
               or (:d <> '' and c.telefoneDigitos like concat('%', :d, '%'))
            """)
    Page<Cliente> buscar(@Param("q") String q, @Param("d") String d, Pageable pageable);
}
