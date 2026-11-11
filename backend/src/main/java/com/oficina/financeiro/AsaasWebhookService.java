package com.oficina.financeiro;

import com.fasterxml.jackson.databind.JsonNode;
import com.oficina.auditoria.AuditoriaService;
import com.oficina.erro.NegocioException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Eventos do Asaas (entrega "ao menos uma vez": o mesmo evento pode chegar repetido).
 * Idempotência: cada cobrança do Asaas vira no máximo um pagamento (índice único em pagamento.id_externo).
 * Só reage a cobranças que o próprio sistema criou; qualquer outro evento ou cobrança é ignorado com sucesso,
 * porque o Asaas pausa a fila do webhook depois de 15 falhas seguidas.
 */
@Service
public class AsaasWebhookService {
    private static final Logger log = LoggerFactory.getLogger(AsaasWebhookService.class);

    private final CobrancaRepository cobrancas;
    private final PagamentoRepository pagamentos;
    private final AuditoriaService auditoria;

    public AsaasWebhookService(CobrancaRepository cobrancas, PagamentoRepository pagamentos, AuditoriaService auditoria) {
        this.cobrancas = cobrancas;
        this.pagamentos = pagamentos;
        this.auditoria = auditoria;
    }

    @Transactional
    public void processar(JsonNode corpo) {
        String evento = corpo.path("event").asText("");
        if (evento.isEmpty()) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Evento sem o campo event.");
        }
        switch (evento) {
            case "PAYMENT_RECEIVED", "PAYMENT_CONFIRMED" -> receber(corpo.path("payment"));
            case "PAYMENT_REFUNDED" -> estornar(corpo.path("payment"));
            default -> log.debug("Evento do Asaas ignorado: {}", evento);
        }
    }

    private void receber(JsonNode pay) {
        String id = idDa(pay);
        var cobranca = cobrancas.findByAsaasId(id);
        if (cobranca.isEmpty()) {
            log.info("Cobrança {} não é deste sistema: ignorada.", id);
            return;
        }
        if (pagamentos.findByIdExterno(id).isPresent()) return; // repetido (ou CONFIRMED depois de RECEIVED)
        Cobranca c = cobranca.get();
        Pagamento p = new Pagamento();
        p.setOs(c.getOs());
        p.setValor(c.getValor()); // o valor que o sistema cobrou; juros e multa do boleto vencido não entram
        p.setForma(forma(pay.path("billingType").asText("")));
        p.setData(dia(pay));
        p.setOrigem("ASAAS");
        p.setIdExterno(id);
        pagamentos.save(p);
        c.setStatus(Cobranca.PAGA);
        auditoria.registrar("PAGAMENTO_ASAAS", "os " + c.getOs().getId() + " cobranca " + c.getId());
    }

    private void estornar(JsonNode pay) {
        String id = idDa(pay);
        pagamentos.findByIdExterno(id).filter(p -> !p.estornado()).ifPresent(p -> {
            p.setEstornadoEm(LocalDateTime.now());
            cobrancas.findByAsaasId(id).ifPresent(c -> c.setStatus(Cobranca.ESTORNADA));
            auditoria.registrar("PAGAMENTO_ASAAS_ESTORNADO", "os " + p.getOs().getId() + " pagamento " + p.getId());
        });
    }

    private static String idDa(JsonNode pay) {
        String id = pay.path("id").asText("");
        if (id.isEmpty()) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Evento sem o id da cobrança.");
        }
        return id;
    }

    /** Pix, boleto e cartão de crédito/débito têm forma própria; transferência e depósito contam como Pix. */
    static FormaPagamento forma(String billingType) {
        return switch (billingType) {
            case "BOLETO" -> FormaPagamento.BOLETO;
            case "CREDIT_CARD", "DEBIT_CARD" -> FormaPagamento.CARTAO;
            default -> FormaPagamento.PIX;
        };
    }

    /** Dia do pagamento informado pelo Asaas; se faltar, vier inválido ou no futuro, vale hoje. */
    static LocalDate dia(JsonNode pay) {
        for (String campo : new String[]{"paymentDate", "clientPaymentDate"}) {
            try {
                LocalDate d = LocalDate.parse(pay.path(campo).asText(""));
                if (!d.isAfter(LocalDate.now()) && d.getYear() >= 2000) return d;
            } catch (DateTimeParseException ignorado) {
                // tenta o próximo campo
            }
        }
        return LocalDate.now();
    }
}
