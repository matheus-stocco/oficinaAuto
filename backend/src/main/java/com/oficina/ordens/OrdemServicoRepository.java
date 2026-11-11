package com.oficina.ordens;

import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Long> {

    /**
     * status = '' significa todos. q casa número da OS, nome do cliente, placa, modelo e marca.
     * p = q sem símbolos em maiúscula (placa com ou sem hífen). veiculoId = 0 significa todos os veículos.
     */
    @Query(value = """
            select o from OrdemServico o
              left join fetch o.cliente c left join fetch o.veiculo v left join fetch o.mecanico m
            where (:status = '' or o.status = :status)
              and o.criadoEm >= :de and o.criadoEm < :ate
              and (:veiculoId = 0 or o.veiculo.id = :veiculoId)
              and (:q = ''
                   or cast(o.id as string) like concat('%', :q, '%')
                   or lower(coalesce(c.contato.nome, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(v.modelo, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(v.marca, '')) like lower(concat('%', :q, '%'))
                   or (:p <> '' and v.placa like concat('%', :p, '%')))
            """,
            countQuery = """
            select count(o) from OrdemServico o
              left join o.cliente c left join o.veiculo v
            where (:status = '' or o.status = :status)
              and o.criadoEm >= :de and o.criadoEm < :ate
              and (:veiculoId = 0 or o.veiculo.id = :veiculoId)
              and (:q = ''
                   or cast(o.id as string) like concat('%', :q, '%')
                   or lower(coalesce(c.contato.nome, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(v.modelo, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(v.marca, '')) like lower(concat('%', :q, '%'))
                   or (:p <> '' and v.placa like concat('%', :p, '%')))
            """)
    Page<OrdemServico> buscar(@Param("status") String status, @Param("q") String q, @Param("p") String p,
                              @Param("de") LocalDateTime de, @Param("ate") LocalDateTime ate,
                              @Param("veiculoId") long veiculoId, Pageable pageable);
}
