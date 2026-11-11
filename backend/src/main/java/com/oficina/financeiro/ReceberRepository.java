package com.oficina.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReceberRepository extends JpaRepository<Receber, Long> {

    @Query("select coalesce(sum(r.valor), 0) from Receber r where r.os.id = :osId and r.status = 'ABERTO'")
    BigDecimal somaAberta(@Param("osId") Long osId);

    List<Receber> findByOsIdOrderByIdAsc(Long osId);

    List<Receber> findByOsIdAndStatus(Long osId, String status);

    Optional<Receber> findByPagamentoId(Long pagamentoId);

    @Query("select coalesce(sum(r.valor), 0) from Receber r where r.status = 'ABERTO'")
    BigDecimal totalAberto();

    @Query("select coalesce(sum(r.valor), 0) from Receber r where r.status = 'ABERTO' and r.vencimento < :hoje")
    BigDecimal totalVencido(@Param("hoje") LocalDate hoje);

    /** status = '' significa todos; q casa número da OS e nome do cliente. */
    @Query(value = """
            select r from Receber r join fetch r.os o left join fetch o.cliente c
            where (:status = '' or r.status = :status)
              and (:q = '' or cast(o.id as string) like concat('%', :q, '%')
                   or lower(coalesce(c.contato.nome, '')) like lower(concat('%', :q, '%')))
            """,
            countQuery = """
            select count(r) from Receber r join r.os o left join o.cliente c
            where (:status = '' or r.status = :status)
              and (:q = '' or cast(o.id as string) like concat('%', :q, '%')
                   or lower(coalesce(c.contato.nome, '')) like lower(concat('%', :q, '%')))
            """)
    Page<Receber> buscar(@Param("status") String status, @Param("q") String q, Pageable pageable);
}
