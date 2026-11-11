package com.oficina.orcamentos;

import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrcamentoRepository extends JpaRepository<Orcamento, Long> {

    @Query(value = """
            select o from Orcamento o left join fetch o.cliente c left join fetch o.veiculo v
            where :q = ''
               or cast(o.id as string) like concat('%', :q, '%')
               or lower(coalesce(c.contato.nome, '')) like lower(concat('%', :q, '%'))
               or lower(coalesce(o.clienteNome, '')) like lower(concat('%', :q, '%'))
               or lower(coalesce(v.modelo, '')) like lower(concat('%', :q, '%'))
               or lower(coalesce(o.veiculoModelo, '')) like lower(concat('%', :q, '%'))
               or (:p <> '' and (v.placa like concat('%', :p, '%') or upper(coalesce(o.veiculoPlaca, '')) like concat('%', :p, '%')))
            """,
            countQuery = """
            select count(o) from Orcamento o left join o.cliente c left join o.veiculo v
            where :q = ''
               or cast(o.id as string) like concat('%', :q, '%')
               or lower(coalesce(c.contato.nome, '')) like lower(concat('%', :q, '%'))
               or lower(coalesce(o.clienteNome, '')) like lower(concat('%', :q, '%'))
               or lower(coalesce(v.modelo, '')) like lower(concat('%', :q, '%'))
               or lower(coalesce(o.veiculoModelo, '')) like lower(concat('%', :q, '%'))
               or (:p <> '' and (v.placa like concat('%', :p, '%') or upper(coalesce(o.veiculoPlaca, '')) like concat('%', :p, '%')))
            """)
    Page<Orcamento> buscar(@Param("q") String q, @Param("p") String p, Pageable pageable);

    /** Apaga os já gerados cujo prazo venceu e os rascunhos nunca gerados e esquecidos. Os itens saem por cascata no banco. */
    @Modifying
    @Query("""
            delete from Orcamento o
            where (o.geradoEm is not null and o.geradoEm < :limiteGerado)
               or (o.geradoEm is null and o.criadoEm < :limiteRascunho)
            """)
    int apagarExpirados(@Param("limiteGerado") LocalDateTime limiteGerado, @Param("limiteRascunho") LocalDateTime limiteRascunho);
}
