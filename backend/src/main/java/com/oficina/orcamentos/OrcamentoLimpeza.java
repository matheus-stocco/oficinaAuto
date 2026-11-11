package com.oficina.orcamentos;

import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Roda a cada hora e apaga os orçamentos vencidos. */
@Component
public class OrcamentoLimpeza {
    private static final Logger log = LoggerFactory.getLogger(OrcamentoLimpeza.class);

    private final OrcamentoService service;

    public OrcamentoLimpeza(OrcamentoService service) {
        this.service = service;
    }

    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT1H")
    public void limpar() {
        int apagados = service.limparExpirados(LocalDateTime.now());
        if (apagados > 0) log.info("Orçamentos expirados apagados: {}", apagados);
    }
}
