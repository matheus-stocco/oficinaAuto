package com.oficina.financeiro;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CobrancaRepository extends JpaRepository<Cobranca, Long> {

    Optional<Cobranca> findByAsaasId(String asaasId);

    List<Cobranca> findByOsIdOrderByIdAsc(Long osId);

    boolean existsByOsIdAndStatus(Long osId, String status);

    /** O que já foi cobrado e ainda não foi pago: reserva parte do saldo da OS. */
    @Query("select coalesce(sum(c.valor), 0) from Cobranca c where c.os.id = :osId and c.status = 'PENDENTE'")
    BigDecimal somaPendentes(@Param("osId") Long osId);
}
