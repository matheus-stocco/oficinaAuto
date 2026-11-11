package com.oficina.agenda;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    /** Só o período pedido: é o que o calendário usa. Quem chama limita o tamanho do período. */
    @Query("""
            select a from Agendamento a
              left join fetch a.cliente left join fetch a.veiculo left join fetch a.mecanico
            where a.data between :de and :ate
            order by a.data, a.hora nulls last, a.id
            """)
    List<Agendamento> doPeriodo(@Param("de") LocalDate de, @Param("ate") LocalDate ate);

    /** Consulta posterior: busca por cliente, veículo ou serviço, com filtro de período e de status ('' = todos). */
    @Query(value = """
            select a from Agendamento a
              left join fetch a.cliente c left join fetch a.veiculo v left join fetch a.mecanico
            where a.data between :de and :ate
              and (:status = '' or a.status = :status)
              and (:q = ''
                   or lower(coalesce(c.contato.nome, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(a.clienteNome, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(v.modelo, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(a.veiculoModelo, '')) like lower(concat('%', :q, '%'))
                   or (:p <> '' and (v.placa like concat('%', :p, '%') or upper(coalesce(a.veiculoPlaca, '')) like concat('%', :p, '%')))
                   or exists (select 1 from AgendamentoServico s where s.agendamento = a
                              and lower(s.descricao) like lower(concat('%', :q, '%'))))
            """,
            countQuery = """
            select count(a) from Agendamento a
              left join a.cliente c left join a.veiculo v
            where a.data between :de and :ate
              and (:status = '' or a.status = :status)
              and (:q = ''
                   or lower(coalesce(c.contato.nome, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(a.clienteNome, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(v.modelo, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(a.veiculoModelo, '')) like lower(concat('%', :q, '%'))
                   or (:p <> '' and (v.placa like concat('%', :p, '%') or upper(coalesce(a.veiculoPlaca, '')) like concat('%', :p, '%')))
                   or exists (select 1 from AgendamentoServico s where s.agendamento = a
                              and lower(s.descricao) like lower(concat('%', :q, '%'))))
            """)
    Page<Agendamento> buscar(@Param("q") String q, @Param("p") String p, @Param("status") String status,
                             @Param("de") LocalDate de, @Param("ate") LocalDate ate, Pageable pageable);
}
