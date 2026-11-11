package com.oficina.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DespesaRepository extends JpaRepository<Despesa, Long> {

    @Query("""
            select d from Despesa d
            where d.data between :de and :ate
              and (:categoria = '' or lower(d.categoria) = lower(:categoria))
              and (:q = '' or lower(d.descricao) like lower(concat('%', :q, '%')))
            """)
    Page<Despesa> buscar(@Param("de") LocalDate de, @Param("ate") LocalDate ate, @Param("categoria") String categoria,
                         @Param("q") String q, Pageable pageable);

    @Query("select coalesce(sum(d.valor), 0) from Despesa d where d.data between :de and :ate")
    BigDecimal somaPeriodo(@Param("de") LocalDate de, @Param("ate") LocalDate ate);

    @Query("select distinct d.categoria from Despesa d where d.categoria is not null order by d.categoria")
    List<String> categorias();
}
