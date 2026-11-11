package com.oficina.financeiro;

import com.oficina.financeiro.FinanceiroDtos.*;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class FinanceiroController {
    private final FinanceiroOsService os;
    private final ConsultasFinanceiras consultas;

    public FinanceiroController(FinanceiroOsService os, ConsultasFinanceiras consultas) {
        this.os = os;
        this.consultas = consultas;
    }

    private static PageRequest pagina(int page, int size, Sort ordem) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), ordem);
    }

    // ----- na OS -----

    @GetMapping("/ordens/{id}/financeiro")
    ResumoOsDto resumo(@PathVariable Long id) {
        return os.resumo(id);
    }

    @PostMapping("/ordens/{id}/pagamentos")
    ResumoOsDto pagar(@PathVariable Long id, @Valid @RequestBody PagamentoForm form) {
        return os.pagar(id, form);
    }

    @PostMapping("/ordens/{id}/fiado")
    ResumoOsDto fiar(@PathVariable Long id, @Valid @RequestBody FiadoForm form) {
        return os.fiar(id, form);
    }

    @PostMapping("/pagamentos/{id}/estornar")
    ResumoOsDto estornar(@PathVariable Long id) {
        return os.estornar(id);
    }

    // ----- caixa e movimentos -----

    @GetMapping("/financeiro/caixa")
    CaixaDto caixa(@RequestParam(required = false) LocalDate data) {
        return consultas.caixa(data);
    }

    @GetMapping("/pagamentos")
    Page<PagamentoDto> pagamentos(@RequestParam(required = false) LocalDate de, @RequestParam(required = false) LocalDate ate,
                                  @RequestParam(required = false) FormaPagamento forma,
                                  @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return consultas.pagamentos(de, ate, forma, pagina(page, size, Sort.by(Sort.Order.desc("data"), Sort.Order.desc("id"))));
    }

    // ----- contas a receber (fiado) -----

    @GetMapping("/receber")
    Page<ReceberDto> receber(@RequestParam(required = false) String status, @RequestParam(required = false) String q,
                             @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return consultas.receber(status, q, pagina(page, size, Sort.by(Sort.Order.asc("vencimento"), Sort.Order.asc("id"))));
    }

    @GetMapping("/receber/resumo")
    ResumoReceberDto resumoReceber() {
        return consultas.resumoReceber();
    }

    @PostMapping("/receber/{id}/baixa")
    ResumoOsDto baixar(@PathVariable Long id, @Valid @RequestBody BaixaForm form) {
        return os.baixar(id, form);
    }

    @PostMapping("/receber/{id}/cancelar")
    ResumoOsDto cancelarFiado(@PathVariable Long id) {
        return os.cancelarFiado(id);
    }

    // ----- despesas -----

    @GetMapping("/despesas")
    Page<DespesaDto> despesas(@RequestParam(required = false) LocalDate de, @RequestParam(required = false) LocalDate ate,
                              @RequestParam(required = false) String categoria, @RequestParam(required = false) String q,
                              @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return consultas.despesas(de, ate, categoria, q, pagina(page, size, Sort.by(Sort.Order.desc("data"), Sort.Order.desc("id"))));
    }

    @GetMapping("/despesas/total")
    BigDecimal totalDespesas(@RequestParam(required = false) LocalDate de, @RequestParam(required = false) LocalDate ate) {
        return consultas.totalDespesas(de, ate);
    }

    @GetMapping("/despesas/categorias")
    List<String> categorias() {
        return consultas.categorias();
    }

    @PostMapping("/despesas")
    DespesaDto criarDespesa(@Valid @RequestBody DespesaForm form) {
        return consultas.criarDespesa(form);
    }

    @PutMapping("/despesas/{id}")
    DespesaDto atualizarDespesa(@PathVariable Long id, @Valid @RequestBody DespesaForm form) {
        return consultas.atualizarDespesa(id, form);
    }

    @DeleteMapping("/despesas/{id}")
    ResponseEntity<Void> excluirDespesa(@PathVariable Long id) {
        consultas.excluirDespesa(id);
        return ResponseEntity.noContent().build();
    }
}
