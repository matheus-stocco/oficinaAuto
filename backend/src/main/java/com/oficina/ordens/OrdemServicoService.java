package com.oficina.ordens;

import com.oficina.auditoria.AuditoriaService;
import com.oficina.clientes.ClienteService;
import com.oficina.erro.NegocioException;
import com.oficina.funcionarios.Funcionario;
import com.oficina.funcionarios.FuncionarioService;
import com.oficina.servicos.ServicoService;
import com.oficina.veiculos.VeiculoService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrdemServicoService {
    private final OrdemServicoRepository repo;
    private final ClienteService clientes;
    private final VeiculoService veiculos;
    private final FuncionarioService funcionarios;
    private final ServicoService servicos;
    private final AuditoriaService auditoria;

    public OrdemServicoService(OrdemServicoRepository repo, ClienteService clientes, VeiculoService veiculos,
                               FuncionarioService funcionarios, ServicoService servicos, AuditoriaService auditoria) {
        this.repo = repo;
        this.clientes = clientes;
        this.veiculos = veiculos;
        this.funcionarios = funcionarios;
        this.servicos = servicos;
        this.auditoria = auditoria;
    }

    // Os métodos públicos devolvem o DTO montado dentro da transação: com open-in-view desligado,
    // cliente, veículo e itens são carregados sob demanda e falhariam fora dela.
    @Transactional
    public OsDto criar(OsForm form) {
        OrdemServico os = new OrdemServico();
        aplicar(os, form);
        return OsDto.de(repo.save(os));
    }

    @Transactional(readOnly = true)
    public OsDto ver(Long id) {
        return OsDto.de(buscar(id));
    }

    @Transactional
    public OsDto atualizar(Long id, OsForm form) {
        OrdemServico os = buscar(id);
        if (!os.statusAtual().editavel()) {
            throw new NegocioException(HttpStatus.CONFLICT,
                    "Esta OS está " + rotulo(os.statusAtual()) + " e não pode mais ser alterada.");
        }
        aplicar(os, form);
        return OsDto.de(os);
    }

    /** Valida a transição, grava na auditoria. Reabrir uma OS entregue exige confirmação explícita. */
    @Transactional
    public OsDto mudarStatus(Long id, StatusOs destino, boolean confirmar) {
        OrdemServico os = buscar(id);
        StatusOs atual = os.statusAtual();
        if (!atual.podeIrPara(destino)) {
            throw new NegocioException(HttpStatus.CONFLICT,
                    "Uma OS " + rotulo(atual) + " não pode ir para " + rotulo(destino) + ".");
        }
        if (atual == StatusOs.ENTREGUE && !confirmar) {
            throw new NegocioException(HttpStatus.CONFLICT, "Reabrir uma OS entregue exige confirmação.");
        }
        os.setStatus(destino.name());
        os.setAtualizadoEm(LocalDateTime.now());
        auditoria.registrar(atual == StatusOs.ENTREGUE ? "OS_REABERTA" : "OS_STATUS",
                "os " + id + " " + atual + " -> " + destino);
        return OsDto.de(os);
    }

    OrdemServico buscar(Long id) {
        return repo.findById(id).orElseThrow(
                () -> new NegocioException(HttpStatus.NOT_FOUND, "Ordem de serviço não encontrada."));
    }

    private void aplicar(OrdemServico os, OsForm f) {
        List<OsForm.ItemForm> itens = f.itens() == null ? List.of() : f.itens();
        var calculo = CalculoValores.calcular(new CalculoValores.Entrada(
                itens.stream().map(i -> new CalculoValores.Linha(i.quantidade(), i.precoUnit())).toList(),
                f.maoDeObra(), f.pecasValor(), f.taxaCompraTipo(), f.taxaCompra(), f.descontoTipo(), f.desconto()));

        os.setCliente(f.clienteId() == null ? null : clientes.buscar(f.clienteId()));
        os.setVeiculo(f.veiculoId() == null ? null : veiculos.buscar(f.veiculoId()));
        os.setMecanico(f.funcionarioId() == null ? null : mecanico(f.funcionarioId()));
        os.setKmEntrada(f.kmEntrada());
        os.setDefeitoRelatado(vazioParaNulo(f.defeitoRelatado()));
        os.setObservacoes(vazioParaNulo(f.observacoes()));
        os.setPrevisaoEntrega(f.previsaoEntrega());

        os.setMaoDeObra(calculo.maoDeObra());
        os.setMaoDeObraDescricao(vazioParaNulo(f.maoDeObraDescricao()));
        os.setPecasValor(calculo.pecas());
        os.setPecasDescricao(vazioParaNulo(f.pecasDescricao()));
        os.setTaxaCompraTipo(f.taxaCompraTipo() == null ? TipoValor.VALOR : f.taxaCompraTipo());
        os.setTaxaCompra(f.taxaCompra() == null ? java.math.BigDecimal.ZERO : f.taxaCompra());
        os.setDescontoTipo(f.descontoTipo() == null ? TipoValor.VALOR : f.descontoTipo());
        os.setDesconto(f.desconto() == null ? java.math.BigDecimal.ZERO : f.desconto());

        os.setSubtotalServicos(calculo.subtotalServicos());
        os.setTaxaCompraValor(calculo.taxaCompra());
        os.setDescontoValor(calculo.desconto());
        os.setTotal(calculo.total());
        os.setReceitaOficina(calculo.receitaOficina());
        os.setAtualizadoEm(LocalDateTime.now());

        os.getItens().clear();
        int pos = 0;
        for (OsForm.ItemForm i : itens) {
            OsItem item = new OsItem();
            item.setOs(os);
            item.setServico(i.servicoId() == null ? null : servicos.buscar(i.servicoId()));
            item.setDescricao(i.descricao().trim());
            item.setQuantidade(i.quantidade());
            item.setPrecoUnit(i.precoUnit());
            item.setPosicao(pos++);
            os.getItens().add(item);
        }
    }

    private Funcionario mecanico(Long id) {
        Funcionario m = funcionarios.buscar(id);
        if (!m.isAtivo()) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Esse funcionário está inativo.");
        }
        return m;
    }

    static String rotulo(StatusOs s) {
        return switch (s) {
            case ABERTA -> "aberta";
            case EM_EXECUCAO -> "em execução";
            case PRONTA -> "pronta";
            case ENTREGUE -> "entregue";
            case CANCELADA -> "cancelada";
        };
    }

    private static String vazioParaNulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
