package com.oficina.veiculos;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

    Optional<Veiculo> findByPlaca(String placa);

    @Query("select v from Veiculo v left join fetch v.cliente where v.cliente.id = :clienteId order by v.modelo")
    List<Veiculo> doCliente(@Param("clienteId") Long clienteId);

    /** q = texto; p = o texto sem símbolos em maiúscula (para casar placa com ou sem hífen). */
    @Query(value = """
            select v from Veiculo v left join fetch v.cliente c
            where lower(v.modelo) like lower(concat('%', :q, '%'))
               or lower(coalesce(v.marca, '')) like lower(concat('%', :q, '%'))
               or lower(coalesce(c.contato.nome, '')) like lower(concat('%', :q, '%'))
               or (:p <> '' and v.placa like concat('%', :p, '%'))
            """,
            countQuery = """
            select count(v) from Veiculo v left join v.cliente c
            where lower(v.modelo) like lower(concat('%', :q, '%'))
               or lower(coalesce(v.marca, '')) like lower(concat('%', :q, '%'))
               or lower(coalesce(c.contato.nome, '')) like lower(concat('%', :q, '%'))
               or (:p <> '' and v.placa like concat('%', :p, '%'))
            """)
    Page<Veiculo> buscar(@Param("q") String q, @Param("p") String p, Pageable pageable);
}
