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

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    @Query("select coalesce(sum(p.valor), 0) from Pagamento p where p.os.id = :osId and p.estornadoEm is null")
    BigDecimal somaValida(@Param("osId") Long osId);

    @Query("select count(p) from Pagamento p where p.os.id = :osId and p.estornadoEm is null")
    long contarValidos(@Param("osId") Long osId);

    List<Pagamento> findByOsIdOrderByIdAsc(Long osId);

    Optional<Pagamento> findByIdExterno(String idExterno);

    /** Entradas por forma de pagamento no período (estornos não contam). */
    @Query("select p.forma, coalesce(sum(p.valor), 0) from Pagamento p "
            + "where p.data between :de and :ate and p.estornadoEm is null group by p.forma")
    List<Object[]> somaPorForma(@Param("de") LocalDate de, @Param("ate") LocalDate ate);

    /** forma = '' significa todas. */
    @Query(value = """
            select p from Pagamento p join fetch p.os o left join fetch o.cliente
            where p.data between :de and :ate and p.estornadoEm is null
              and (:forma = '' or cast(p.forma as string) = :forma)
            """,
            countQuery = """
            select count(p) from Pagamento p
            where p.data between :de and :ate and p.estornadoEm is null
              and (:forma = '' or cast(p.forma as string) = :forma)
            """)
    Page<Pagamento> doPeriodo(@Param("de") LocalDate de, @Param("ate") LocalDate ate,
                              @Param("forma") String forma, Pageable pageable);
}
