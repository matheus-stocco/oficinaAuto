package com.oficina.financeiro;

import com.oficina.auditoria.AuditoriaService;
import com.oficina.erro.NegocioException;
import com.oficina.financeiro.FinanceiroDtos.CaixaDto;
import com.oficina.financeiro.FinanceiroDtos.DespesaDto;
import com.oficina.financeiro.FinanceiroDtos.PagamentoDto;
import com.oficina.financeiro.FinanceiroDtos.ReceberDto;
import com.oficina.financeiro.FinanceiroDtos.ResumoReceberDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Listas e totais do financeiro (caixa do dia, contas a receber, despesas). Tudo paginado ou agregado no banco. */
@Service
public class ConsultasFinanceiras {
    private static final LocalDate INICIO = LocalDate.of(2000, 1, 1);
    private static final LocalDate FIM = LocalDate.of(2100, 12, 31);

    private final PagamentoRepository pagamentos;
    private final ReceberRepository receber;
    private final DespesaRepository despesas;
    private final AuditoriaService auditoria;

    public ConsultasFinanceiras(PagamentoRepository pagamentos, ReceberRepository receber, DespesaRepository despesas,
                                AuditoriaService auditoria) {
        this.pagamentos = pagamentos;
        this.receber = receber;
        this.despesas = despesas;
        this.auditoria = auditoria;
    }

    /** Entradas do dia por forma, despesas do dia e o saldo (entradas - despesas). Estornos não entram. */
    @Transactional(readOnly = true)
    public CaixaDto caixa(LocalDate data) {
        LocalDate dia = data == null ? LocalDate.now() : data;
        Map<FormaPagamento, BigDecimal> porForma = new EnumMap<>(FormaPagamento.class);
        for (FormaPagamento f : FormaPagamento.values()) porForma.put(f, BigDecimal.ZERO);
        for (Object[] linha : pagamentos.somaPorForma(dia, dia)) porForma.put((FormaPagamento) linha[0], (BigDecimal) linha[1]);
        BigDecimal entradas = porForma.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal saidas = despesas.somaPeriodo(dia, dia);
        return new CaixaDto(dia, porForma, entradas, saidas, entradas.subtract(saidas));
    }

    @Transactional(readOnly = true)
    public Page<PagamentoDto> pagamentos(LocalDate de, LocalDate ate, FormaPagamento forma, Pageable pageable) {
        return pagamentos.doPeriodo(de == null ? INICIO : de, ate == null ? FIM : ate, forma == null ? "" : forma.name(), pageable)
                .map(PagamentoDto::de);
    }

    @Transactional(readOnly = true)
    public Page<ReceberDto> receber(String status, String q, Pageable pageable) {
        LocalDate hoje = LocalDate.now();
        return receber.buscar(status == null ? "" : status, q == null ? "" : q.trim(), pageable).map(r -> ReceberDto.de(r, hoje));
    }

    @Transactional(readOnly = true)
    public ResumoReceberDto resumoReceber() {
        return new ResumoReceberDto(receber.totalAberto(), receber.totalVencido(LocalDate.now()));
    }

    // ---------- despesas ----------

    @Transactional
    public DespesaDto criarDespesa(DespesaForm f) {
        Despesa d = new Despesa();
        aplicar(d, f);
        return DespesaDto.de(despesas.save(d));
    }

    @Transactional
    public DespesaDto atualizarDespesa(Long id, DespesaForm f) {
        Despesa d = despesa(id);
        aplicar(d, f);
        return DespesaDto.de(d);
    }

    @Transactional
    public void excluirDespesa(Long id) {
        Despesa d = despesa(id);
        despesas.delete(d);
        auditoria.registrar("DESPESA_EXCLUIDA", "despesa " + id);
    }

    @Transactional(readOnly = true)
    public Page<DespesaDto> despesas(LocalDate de, LocalDate ate, String categoria, String q, Pageable pageable) {
        return despesas.buscar(de == null ? INICIO : de, ate == null ? FIM : ate, categoria == null ? "" : categoria.trim(),
                q == null ? "" : q.trim(), pageable).map(DespesaDto::de);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalDespesas(LocalDate de, LocalDate ate) {
        return despesas.somaPeriodo(de == null ? INICIO : de, ate == null ? FIM : ate);
    }

    @Transactional(readOnly = true)
    public List<String> categorias() {
        return despesas.categorias();
    }

    private Despesa despesa(Long id) {
        return despesas.findById(id).orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Despesa não encontrada."));
    }

    private static void aplicar(Despesa d, DespesaForm f) {
        if (f.data().getYear() < 2000 || f.data().getYear() > 2100) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Data fora do intervalo permitido.");
        }
        d.setDescricao(f.descricao().trim());
        d.setValor(f.valor());
        d.setData(f.data());
        d.setCategoria(f.categoria() == null || f.categoria().isBlank() ? null : f.categoria().trim());
    }
}
