package com.oficina.agenda;

import com.oficina.clientes.ClienteService;
import com.oficina.erro.NegocioException;
import com.oficina.funcionarios.Funcionario;
import com.oficina.funcionarios.FuncionarioService;
import com.oficina.ordens.CadastrosNaHora;
import com.oficina.ordens.CadastrosNaHoraService;
import com.oficina.ordens.OrdemServicoService;
import com.oficina.ordens.OsDto;
import com.oficina.ordens.OsForm;
import com.oficina.servicos.ServicoService;
import com.oficina.veiculos.Placa;
import com.oficina.veiculos.VeiculoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgendamentoService {
    /** O calendário mostra no máximo 6 semanas; este teto impede pedir a agenda inteira de uma vez. */
    static final int MAX_DIAS_CALENDARIO = 62;

    private final AgendamentoRepository repo;
    private final ClienteService clientes;
    private final VeiculoService veiculos;
    private final FuncionarioService funcionarios;
    private final ServicoService servicos;
    private final OrdemServicoService ordens;
    private final CadastrosNaHoraService cadastros;

    public AgendamentoService(AgendamentoRepository repo, ClienteService clientes, VeiculoService veiculos,
                              FuncionarioService funcionarios, ServicoService servicos, OrdemServicoService ordens,
                              CadastrosNaHoraService cadastros) {
        this.repo = repo;
        this.clientes = clientes;
        this.veiculos = veiculos;
        this.funcionarios = funcionarios;
        this.servicos = servicos;
        this.ordens = ordens;
        this.cadastros = cadastros;
    }

    // DTOs são montados dentro da transação (open-in-view desligado).
    @Transactional
    public AgendamentoDto criar(AgendamentoForm form) {
        Agendamento a = new Agendamento();
        aplicar(a, form);
        return AgendamentoDto.de(repo.save(a));
    }

    @Transactional
    public AgendamentoDto atualizar(Long id, AgendamentoForm form) {
        Agendamento a = buscar(id);
        aplicar(a, form);
        return AgendamentoDto.de(a);
    }

    @Transactional(readOnly = true)
    public AgendamentoDto ver(Long id) {
        return AgendamentoDto.de(buscar(id));
    }

    @Transactional
    public AgendamentoDto mudarStatus(Long id, StatusAgendamento destino) {
        Agendamento a = buscar(id);
        a.setStatus(destino.name());
        a.setAtualizadoEm(LocalDateTime.now());
        return AgendamentoDto.de(a);
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResumoDto> calendario(LocalDate de, LocalDate ate) {
        if (de == null || ate == null || ate.isBefore(de)) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Informe um período válido (de e ate).");
        }
        if (ChronoUnit.DAYS.between(de, ate) + 1 > MAX_DIAS_CALENDARIO) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "O período pode ter no máximo " + MAX_DIAS_CALENDARIO + " dias.");
        }
        return repo.doPeriodo(de, ate).stream().map(AgendamentoResumoDto::de).toList();
    }

    @Transactional(readOnly = true)
    public Page<AgendamentoResumoDto> consultar(String q, StatusAgendamento status, LocalDate de, LocalDate ate, Pageable pageable) {
        String texto = q == null ? "" : q.trim();
        return repo.buscar(texto, Placa.normalizar(texto), status == null ? "" : status.name(),
                de == null ? LocalDate.of(2000, 1, 1) : de, ate == null ? LocalDate.of(2100, 12, 31) : ate, pageable)
                .map(AgendamentoResumoDto::de);
    }

    /**
     * Abre a OS com cliente, veículo, mecânico e serviços do agendamento (preço do catálogo, ou zero para texto livre),
     * marca o agendamento como realizado e guarda o número da OS. Tudo numa transação: se falhar, nada muda.
     */
    @Transactional
    public OsDto criarOs(Long id, CadastrosNaHora novos) {
        Agendamento a = buscar(id);
        if (a.getOsId() != null) {
            throw new NegocioException(HttpStatus.CONFLICT, "Já existe a OS " + a.getOsId() + " para este agendamento.");
        }
        if (a.statusAtual() == StatusAgendamento.CANCELADO) {
            throw new NegocioException(HttpStatus.CONFLICT, "Um agendamento cancelado não gera OS. Mude a situação antes.");
        }
        var r = cadastros.resolver(a.getCliente(), a.getVeiculo(), novos);
        boolean mecanicoAtivo = a.getMecanico() != null && a.getMecanico().isAtivo();
        OsDto os = ordens.criar(new OsForm(
                r.cliente() == null ? null : r.cliente().getId(), r.veiculo() == null ? null : r.veiculo().getId(),
                mecanicoAtivo ? a.getMecanico().getId() : null, null, null, a.getObservacoes(), null,
                a.getServicos().stream().map(s -> new OsForm.ItemForm(s.getServico() == null ? null : s.getServico().getId(),
                        s.getDescricao(), BigDecimal.ONE, s.getServico() == null ? BigDecimal.ZERO : s.getServico().getPrecoPadrao())).toList(),
                null, null, null, null, null, null, null, null));
        a.setOsId(os.id());
        a.setStatus(StatusAgendamento.REALIZADO.name());
        a.setAtualizadoEm(LocalDateTime.now());
        return os;
    }

    Agendamento buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Agendamento não encontrado."));
    }

    private void aplicar(Agendamento a, AgendamentoForm f) {
        if (f.data().getYear() < 2000 || f.data().getYear() > 2100) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Data fora do intervalo permitido.");
        }
        a.setData(f.data());
        a.setHora(f.hora());
        a.setCliente(f.clienteId() == null ? null : clientes.buscar(f.clienteId()));
        a.setClienteNome(a.getCliente() != null ? null : vazioParaNulo(f.clienteNome()));
        a.setClienteTelefone(a.getCliente() != null ? null : vazioParaNulo(f.clienteTelefone()));
        a.setVeiculo(f.veiculoId() == null ? null : veiculos.buscar(f.veiculoId()));
        a.setVeiculoPlaca(a.getVeiculo() != null ? null : Placa.validar(f.veiculoPlaca()));
        a.setVeiculoModelo(a.getVeiculo() != null ? null : vazioParaNulo(f.veiculoModelo()));
        a.setMecanico(f.funcionarioId() == null ? null : mecanico(f.funcionarioId()));
        a.setObservacoes(vazioParaNulo(f.observacoes()));
        a.setAtualizadoEm(LocalDateTime.now());

        a.getServicos().clear();
        int pos = 0;
        for (AgendamentoForm.ServicoForm s : f.servicos() == null ? List.<AgendamentoForm.ServicoForm>of() : f.servicos()) {
            AgendamentoServico item = new AgendamentoServico();
            item.setAgendamento(a);
            item.setServico(s.servicoId() == null ? null : servicos.buscar(s.servicoId()));
            item.setDescricao(s.descricao().trim());
            item.setPosicao(pos++);
            a.getServicos().add(item);
        }
    }

    private Funcionario mecanico(Long id) {
        Funcionario m = funcionarios.buscar(id);
        if (!m.isAtivo()) throw new NegocioException(HttpStatus.BAD_REQUEST, "Esse funcionário está inativo.");
        return m;
    }

    private static String vazioParaNulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
