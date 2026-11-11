package com.oficina.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/** Respostas do financeiro. */
public final class FinanceiroDtos {
    private FinanceiroDtos() {}

    public record PagamentoDto(Long id, Long osId, String clienteNome, BigDecimal valor, FormaPagamento forma,
                               LocalDate data, String origem, boolean estornado) {
        static PagamentoDto de(Pagamento p) {
            var c = p.getOs().getCliente();
            return new PagamentoDto(p.getId(), p.getOs().getId(), c == null ? null : c.getContato().getNome(),
                    p.getValor(), p.getForma(), p.getData(), p.getOrigem(), p.estornado());
        }
    }

    public record ReceberDto(Long id, Long osId, String clienteNome, BigDecimal valor, LocalDate vencimento,
                             String status, LocalDate pagoEm, long diasAtraso) {
        static ReceberDto de(Receber r, LocalDate hoje) {
            var c = r.getOs().getCliente();
            long atraso = Receber.ABERTO.equals(r.getStatus()) && r.getVencimento().isBefore(hoje)
                    ? ChronoUnit.DAYS.between(r.getVencimento(), hoje) : 0;
            return new ReceberDto(r.getId(), r.getOs().getId(), c == null ? null : c.getContato().getNome(),
                    r.getValor(), r.getVencimento(), r.getStatus(), r.getPagoEm(), atraso);
        }
    }

    /** Situação financeira de uma OS. saldo = total - pago - fiado em aberto. */
    public record ResumoOsDto(BigDecimal total, BigDecimal pago, BigDecimal fiadoAberto, BigDecimal saldo,
                              boolean podeEntregar, List<PagamentoDto> pagamentos, List<ReceberDto> fiados) {}

    public record ResumoReceberDto(BigDecimal totalAberto, BigDecimal totalVencido) {}

    public record CaixaDto(LocalDate data, Map<FormaPagamento, BigDecimal> porForma, BigDecimal entradas,
                           BigDecimal despesas, BigDecimal saldo) {}

    public record DespesaDto(Long id, String descricao, BigDecimal valor, LocalDate data, String categoria) {
        static DespesaDto de(Despesa d) {
            return new DespesaDto(d.getId(), d.getDescricao(), d.getValor(), d.getData(), d.getCategoria());
        }
    }
}
