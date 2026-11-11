package com.oficina.financeiro;

import com.oficina.auditoria.AuditoriaService;
import com.oficina.erro.NegocioException;
import com.oficina.financeiro.FinanceiroDtos.PagamentoDto;
import com.oficina.financeiro.FinanceiroDtos.ReceberDto;
import com.oficina.financeiro.FinanceiroDtos.ResumoOsDto;
import com.oficina.ordens.OrdemServico;
import com.oficina.ordens.OrdemServicoRepository;
import com.oficina.ordens.RegrasFinanceirasDaOs;
import com.oficina.ordens.StatusOs;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dinheiro de uma OS: pagamentos (parciais, em dinheiro, Pix ou cartão), fiado e as regras de entrega e cancelamento.
 * saldo = total - pagamentos válidos - fiado em aberto. Nunca se recebe nem se fia mais que o saldo.
 */
@Service
public class FinanceiroOsService implements RegrasFinanceirasDaOs {
    private final OrdemServicoRepository ordens;
    private final PagamentoRepository pagamentos;
    private final ReceberRepository receber;
    private final AuditoriaService auditoria;
    private final CobrancaRepository cobrancas;

    public FinanceiroOsService(OrdemServicoRepository ordens, PagamentoRepository pagamentos, ReceberRepository receber,
                               AuditoriaService auditoria, CobrancaRepository cobrancas) {
        this.ordens = ordens;
        this.pagamentos = pagamentos;
        this.receber = receber;
        this.auditoria = auditoria;
        this.cobrancas = cobrancas;
    }

    @Transactional(readOnly = true)
    public ResumoOsDto resumo(Long osId) {
        return montarResumo(os(osId));
    }

    @Transactional
    public ResumoOsDto pagar(Long osId, PagamentoForm f) {
        OrdemServico os = os(osId);
        exigirOsAberta(os);
        validarSaldo(os, f.valor());
        Pagamento p = novoPagamento(os, f.valor(), f.forma(), f.data(), "MANUAL");
        auditoria.registrar("PAGAMENTO", "os " + osId + " pagamento " + p.getId() + " " + f.forma());
        return montarResumo(os);
    }

    @Transactional
    public ResumoOsDto fiar(Long osId, FiadoForm f) {
        OrdemServico os = os(osId);
        exigirOsAberta(os);
        if (f.vencimento().isBefore(LocalDate.now())) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "O vencimento não pode ser uma data passada.");
        }
        validarSaldo(os, f.valor());
        Receber r = new Receber();
        r.setOs(os);
        r.setValor(f.valor());
        r.setVencimento(f.vencimento());
        receber.save(r);
        auditoria.registrar("FIADO", "os " + osId + " fiado " + r.getId());
        return montarResumo(os);
    }

    /** O pagamento fica no histórico, marcado como estornado; se veio da baixa de um fiado, o fiado volta a ficar em aberto. */
    @Transactional
    public ResumoOsDto estornar(Long pagamentoId) {
        Pagamento p = pagamentos.findById(pagamentoId).orElseThrow(
                () -> new NegocioException(HttpStatus.NOT_FOUND, "Pagamento não encontrado."));
        if (p.estornado()) {
            throw new NegocioException(HttpStatus.CONFLICT, "Este pagamento já foi estornado.");
        }
        if ("ASAAS".equals(p.getOrigem())) {
            throw new NegocioException(HttpStatus.CONFLICT, "Pagamento recebido pelo Asaas: estorne-o no próprio Asaas.");
        }
        p.setEstornadoEm(LocalDateTime.now());
        receber.findByPagamentoId(pagamentoId).ifPresent(r -> {
            r.setStatus(Receber.ABERTO);
            r.setPagoEm(null);
            r.setPagamentoId(null);
        });
        auditoria.registrar("PAGAMENTO_ESTORNADO", "os " + p.getOs().getId() + " pagamento " + pagamentoId);
        return montarResumo(p.getOs());
    }

    @Transactional
    public ResumoOsDto cancelarFiado(Long receberId) {
        Receber r = receber(receberId);
        if (!Receber.ABERTO.equals(r.getStatus())) {
            throw new NegocioException(HttpStatus.CONFLICT, "Só é possível cancelar um fiado em aberto.");
        }
        r.setStatus(Receber.CANCELADO);
        auditoria.registrar("FIADO_CANCELADO", "os " + r.getOs().getId() + " fiado " + receberId);
        return montarResumo(r.getOs());
    }

    /** Recebe o fiado: gera o pagamento do valor dele, na forma e no dia informados. */
    @Transactional
    public ResumoOsDto baixar(Long receberId, BaixaForm f) {
        Receber r = receber(receberId);
        if (!Receber.ABERTO.equals(r.getStatus())) {
            throw new NegocioException(HttpStatus.CONFLICT, "Só é possível receber um fiado em aberto.");
        }
        Pagamento p = novoPagamento(r.getOs(), r.getValor(), f.forma(), f.data(), "MANUAL");
        r.setStatus(Receber.PAGO);
        r.setPagoEm(p.getData());
        r.setPagamentoId(p.getId());
        auditoria.registrar("FIADO_RECEBIDO", "os " + r.getOs().getId() + " fiado " + receberId + " pagamento " + p.getId());
        return montarResumo(r.getOs());
    }

    @Override
    @Transactional(readOnly = true)
    public void aoEntregar(Long osId, BigDecimal total) {
        BigDecimal comprometido = pagamentos.somaValida(osId).add(receber.somaAberta(osId));
        int cmp = comprometido.compareTo(total);
        if (cmp < 0) {
            throw new NegocioException(HttpStatus.CONFLICT, "Falta receber " + real(total.subtract(comprometido))
                    + ": registre o pagamento ou o fiado antes de entregar.");
        }
        if (cmp > 0) {
            throw new NegocioException(HttpStatus.CONFLICT, "Os pagamentos e fiados passam do total da OS em "
                    + real(comprometido.subtract(total)) + ": ajuste antes de entregar.");
        }
    }

    @Override
    @Transactional
    public void aoCancelar(Long osId) {
        if (pagamentos.contarValidos(osId) > 0) {
            throw new NegocioException(HttpStatus.CONFLICT, "Esta OS tem pagamento recebido: estorne os pagamentos antes de cancelar.");
        }
        if (cobrancas.existsByOsIdAndStatus(osId, Cobranca.PENDENTE)) {
            throw new NegocioException(HttpStatus.CONFLICT, "Há cobrança online pendente: cancele-a antes de cancelar a OS.");
        }
        for (Receber r : receber.findByOsIdAndStatus(osId, Receber.ABERTO)) {
            r.setStatus(Receber.CANCELADO);
            auditoria.registrar("FIADO_CANCELADO", "os " + osId + " fiado " + r.getId() + " (OS cancelada)");
        }
    }

    // ---------- internos ----------

    private Pagamento novoPagamento(OrdemServico os, BigDecimal valor, FormaPagamento forma, LocalDate data, String origem) {
        LocalDate dia = data == null ? LocalDate.now() : data;
        if (dia.isAfter(LocalDate.now())) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "A data do pagamento não pode ser futura.");
        }
        if (dia.getYear() < 2000) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Data fora do intervalo permitido.");
        }
        Pagamento p = new Pagamento();
        p.setOs(os);
        p.setValor(valor);
        p.setForma(forma);
        p.setData(dia);
        p.setOrigem(origem);
        return pagamentos.save(p);
    }

    private void validarSaldo(OrdemServico os, BigDecimal valor) {
        BigDecimal saldo = saldoDe(os);
        if (saldo.signum() <= 0) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Esta OS já está quitada ou totalmente fiada: não há saldo.");
        }
        if (valor.compareTo(saldo) > 0) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "O valor passa do saldo da OS (" + real(saldo) + ").");
        }
    }

    /** total - pagamentos válidos - fiado em aberto. */
    BigDecimal saldoDe(OrdemServico os) {
        return os.getTotal().subtract(pagamentos.somaValida(os.getId())).subtract(receber.somaAberta(os.getId()));
    }

    private ResumoOsDto montarResumo(OrdemServico os) {
        BigDecimal pago = pagamentos.somaValida(os.getId());
        BigDecimal fiado = receber.somaAberta(os.getId());
        BigDecimal saldo = os.getTotal().subtract(pago).subtract(fiado);
        LocalDate hoje = LocalDate.now();
        List<PagamentoDto> pags = pagamentos.findByOsIdOrderByIdAsc(os.getId()).stream().map(PagamentoDto::de).toList();
        List<ReceberDto> fiados = receber.findByOsIdOrderByIdAsc(os.getId()).stream().map(r -> ReceberDto.de(r, hoje)).toList();
        return new ResumoOsDto(os.getTotal(), pago, fiado, saldo, saldo.signum() == 0, pags, fiados);
    }

    private OrdemServico os(Long id) {
        return ordens.findById(id).orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Ordem de serviço não encontrada."));
    }

    private Receber receber(Long id) {
        return receber.findById(id).orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Fiado não encontrado."));
    }

    private static void exigirOsAberta(OrdemServico os) {
        if (os.statusAtual() == StatusOs.CANCELADA) {
            throw new NegocioException(HttpStatus.CONFLICT, "Uma OS cancelada não recebe pagamento nem fiado.");
        }
    }

    static String real(BigDecimal v) {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(v).replace(' ', ' ');
    }
}
