package com.oficina.orcamentos;

import com.oficina.clientes.Cliente;
import com.oficina.clientes.ClienteForm;
import com.oficina.clientes.ClienteService;
import com.oficina.empresa.EmpresaService;
import com.oficina.erro.NegocioException;
import com.oficina.ordens.OrdemServicoService;
import com.oficina.ordens.OsDto;
import com.oficina.ordens.OsForm;
import com.oficina.veiculos.Veiculo;
import com.oficina.veiculos.VeiculoForm;
import com.oficina.veiculos.VeiculoService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrcamentoService {
    /** Rascunho que nunca teve o PDF aberto some depois deste prazo. */
    static final Duration RASCUNHO_ESQUECIDO = Duration.ofDays(7);

    private final OrcamentoRepository repo;
    private final ClienteService clientes;
    private final VeiculoService veiculos;
    private final com.oficina.servicos.ServicoService servicos;
    private final OrdemServicoService ordens;
    private final EmpresaService empresa;

    public OrcamentoService(OrcamentoRepository repo, ClienteService clientes, VeiculoService veiculos,
                            com.oficina.servicos.ServicoService servicos, OrdemServicoService ordens, EmpresaService empresa) {
        this.repo = repo;
        this.clientes = clientes;
        this.veiculos = veiculos;
        this.servicos = servicos;
        this.ordens = ordens;
        this.empresa = empresa;
    }

    int horas() {
        return empresa.buscar().getOrcamentoRascunhoHoras();
    }

    // Os métodos públicos devolvem DTOs montados dentro da transação (open-in-view está desligado).
    @Transactional
    public OrcamentoDto criar(OrcamentoForm form) {
        Orcamento o = new Orcamento();
        aplicar(o, form);
        return OrcamentoDto.de(repo.save(o), horas());
    }

    @Transactional
    public OrcamentoDto atualizar(Long id, OrcamentoForm form) {
        Orcamento o = buscar(id);
        aplicar(o, form);
        return OrcamentoDto.de(o, horas());
    }

    @Transactional(readOnly = true)
    public OrcamentoDto ver(Long id) {
        return OrcamentoDto.de(buscar(id), horas());
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<OrcamentoResumoDto> listar(String q, String placa,
                                                                         org.springframework.data.domain.Pageable pageable) {
        int h = horas();
        return repo.buscar(q, placa, pageable).map(o -> OrcamentoResumoDto.de(o, h));
    }

    @Transactional
    public void excluir(Long id) {
        repo.delete(buscar(id));
    }

    Orcamento buscar(Long id) {
        return repo.findById(id).orElseThrow(
                () -> new NegocioException(HttpStatus.NOT_FOUND, "Orçamento não encontrado ou já expirado."));
    }

    /** Marca o início do prazo de expiração, uma única vez. */
    @Transactional
    public void marcarGerado(Orcamento o) {
        if (o.getGeradoEm() == null) o.setGeradoEm(LocalDateTime.now());
    }

    /**
     * Cria a OS com os mesmos itens e valores e apaga o orçamento, tudo numa transação: se qualquer passo
     * falhar (por exemplo, placa repetida ao cadastrar o veículo), nada muda e o orçamento continua lá.
     */
    @Transactional
    public OsDto criarOs(Long id, ConversaoForm conversao) {
        Orcamento o = buscar(id);
        Cliente cliente = o.getCliente();
        if (cliente == null && conversao != null && conversao.cliente() != null) {
            cliente = clientes.criar(new ClienteForm(conversao.cliente().nome(), conversao.cliente().telefone(), null, null, null, null));
        }
        Veiculo veiculo = o.getVeiculo();
        if (veiculo == null && conversao != null && conversao.veiculo() != null) {
            veiculo = veiculos.criar(new VeiculoForm(conversao.veiculo().placa(), null, conversao.veiculo().modelo(), null, null,
                    cliente == null ? null : cliente.getId()));
        }
        OsDto os = ordens.criar(new OsForm(
                cliente == null ? null : cliente.getId(), veiculo == null ? null : veiculo.getId(), null, null, null,
                o.getObservacoes(), null,
                o.getItens().stream().map(i -> new OsForm.ItemForm(i.getServico() == null ? null : i.getServico().getId(),
                        i.getDescricao(), i.getQuantidade(), i.getPrecoUnit())).toList(),
                o.getMaoDeObra(), o.getMaoDeObraDescricao(), o.getPecasValor(), o.getPecasDescricao(),
                o.getTaxaCompraTipo(), o.getTaxaCompra(), o.getDescontoTipo(), o.getDesconto()));
        repo.delete(o);
        return os;
    }

    /** Apaga orçamentos cujo prazo venceu. `agora` vem de fora para o teste controlar o relógio. */
    @Transactional
    public int limparExpirados(LocalDateTime agora) {
        return repo.apagarExpirados(agora.minusHours(horas()), agora.minus(RASCUNHO_ESQUECIDO));
    }

    private void aplicar(Orcamento o, OrcamentoForm f) {
        List<OsForm.ItemForm> itens = f.itens() == null ? List.of() : f.itens();
        o.aplicarValores(f);

        o.setCliente(f.clienteId() == null ? null : clientes.buscar(f.clienteId()));
        // avulso só vale quando não há cadastrado
        o.setClienteNome(o.getCliente() != null ? null : vazioParaNulo(f.clienteNome()));
        o.setClienteTelefone(o.getCliente() != null ? null : vazioParaNulo(f.clienteTelefone()));
        o.setVeiculo(f.veiculoId() == null ? null : veiculos.buscar(f.veiculoId()));
        String placa = o.getVeiculo() != null ? null : com.oficina.veiculos.Placa.validar(f.veiculoPlaca());
        o.setVeiculoPlaca(placa);
        o.setVeiculoModelo(o.getVeiculo() != null ? null : vazioParaNulo(f.veiculoModelo()));
        o.setObservacoes(vazioParaNulo(f.observacoes()));
        o.setAtualizadoEm(LocalDateTime.now());

        o.getItens().clear();
        int pos = 0;
        for (OsForm.ItemForm i : itens) {
            OrcamentoItem item = new OrcamentoItem();
            item.setOrcamento(o);
            item.setServico(i.servicoId() == null ? null : servicos.buscar(i.servicoId()));
            item.setDescricao(i.descricao().trim());
            item.setQuantidade(i.quantidade());
            item.setPrecoUnit(i.precoUnit());
            item.setPosicao(pos++);
            o.getItens().add(item);
        }
    }

    private static String vazioParaNulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
