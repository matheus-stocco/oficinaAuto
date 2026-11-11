package com.oficina.financeiro;

import com.oficina.auditoria.AuditoriaService;
import com.oficina.clientes.Cliente;
import com.oficina.erro.NegocioException;
import com.oficina.financeiro.gateway.GatewayException;
import com.oficina.financeiro.gateway.GatewayPagamento;
import com.oficina.financeiro.gateway.GatewayPagamento.NovaCobranca;
import com.oficina.ordens.OrdemServico;
import com.oficina.ordens.OrdemServicoRepository;
import com.oficina.ordens.StatusOs;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cobrança online de uma OS (Pix, boleto ou cartão). O dinheiro só entra no caixa quando o webhook avisar que foi pago. */
@Service
public class CobrancaService {
    public record CobrancaDto(Long id, String tipo, BigDecimal valor, LocalDate vencimento, String status, String link) {
        static CobrancaDto de(Cobranca c) {
            return new CobrancaDto(c.getId(), c.getTipo(), c.getValor(), c.getVencimento(), c.getStatus(), c.getLink());
        }
    }

    /** online: há chave configurada (a tela só mostra o botão se for true). disponivel: saldo ainda não cobrado. */
    public record InfoDto(boolean online, boolean temCliente, boolean clienteTemDocumento, BigDecimal disponivel,
                          List<CobrancaDto> cobrancas) {}

    private final OrdemServicoRepository ordens;
    private final CobrancaRepository cobrancas;
    private final FinanceiroOsService financeiro;
    private final GatewayPagamento gateway;
    private final AuditoriaService auditoria;

    public CobrancaService(OrdemServicoRepository ordens, CobrancaRepository cobrancas, FinanceiroOsService financeiro,
                           GatewayPagamento gateway, AuditoriaService auditoria) {
        this.ordens = ordens;
        this.cobrancas = cobrancas;
        this.financeiro = financeiro;
        this.gateway = gateway;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public InfoDto info(Long osId) {
        return montar(os(osId));
    }

    @Transactional
    public InfoDto criar(Long osId, CobrancaForm f) {
        if (!gateway.ativo()) {
            throw new NegocioException(HttpStatus.CONFLICT, "A cobrança online não está configurada.");
        }
        OrdemServico os = os(osId);
        if (os.statusAtual() == StatusOs.CANCELADA) {
            throw new NegocioException(HttpStatus.CONFLICT, "Uma OS cancelada não recebe cobrança.");
        }
        Cliente cliente = os.getCliente();
        if (cliente == null) {
            throw new NegocioException(HttpStatus.CONFLICT, "Para gerar a cobrança online, escolha um cliente na OS.");
        }
        if (cliente.anonimizado()) {
            throw new NegocioException(HttpStatus.CONFLICT, "O cliente desta OS foi anonimizado: não dá para cobrá-lo.");
        }
        String documento = documento(f.cpfCnpj(), cliente);

        BigDecimal disponivel = disponivel(os);
        if (disponivel.signum() <= 0) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Não há saldo a cobrar: a OS está quitada, fiada ou já cobrada.");
        }
        BigDecimal valor = f.valor() == null ? disponivel : f.valor();
        if (valor.compareTo(disponivel) > 0) {
            throw new NegocioException(HttpStatus.BAD_REQUEST,
                    "O valor passa do saldo disponível para cobrança (" + FinanceiroOsService.real(disponivel) + ").");
        }
        LocalDate vencimento = f.vencimento() == null ? LocalDate.now().plusDays(3) : f.vencimento();
        if (vencimento.isBefore(LocalDate.now())) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "O vencimento não pode ser uma data passada.");
        }

        var criada = comTratamento(() -> gateway.criarCobranca(new NovaCobranca(cliente.getAsaasId(),
                cliente.getContato().getNome(), documento, cliente.getContato().getEmail(), f.tipo(), valor, vencimento,
                "Ordem de serviço " + os.getId(), "os-" + os.getId())));

        if (criada.clienteIdExterno() != null) cliente.setAsaasId(criada.clienteIdExterno());
        Cobranca c = new Cobranca();
        c.setOs(os);
        c.setAsaasId(criada.idCobranca());
        c.setTipo(f.tipo().name());
        c.setValor(valor);
        c.setVencimento(vencimento);
        c.setLink(criada.link());
        cobrancas.save(c);
        auditoria.registrar("COBRANCA_CRIADA", "os " + osId + " cobranca " + c.getId() + " " + f.tipo());
        return montar(os);
    }

    /** Cancela no gateway e aqui. Não é estorno: cobrança já paga não pode ser cancelada. */
    @Transactional
    public InfoDto cancelar(Long cobrancaId) {
        Cobranca c = cobrancas.findById(cobrancaId).orElseThrow(
                () -> new NegocioException(HttpStatus.NOT_FOUND, "Cobrança não encontrada."));
        if (!Cobranca.PENDENTE.equals(c.getStatus())) {
            throw new NegocioException(HttpStatus.CONFLICT, "Só é possível cancelar uma cobrança pendente.");
        }
        comTratamento(() -> { gateway.cancelarCobranca(c.getAsaasId()); return null; });
        c.setStatus(Cobranca.CANCELADA);
        auditoria.registrar("COBRANCA_CANCELADA", "os " + c.getOs().getId() + " cobranca " + cobrancaId);
        return montar(c.getOs());
    }

    // ---------- internos ----------

    private InfoDto montar(OrdemServico os) {
        Cliente c = os.getCliente();
        boolean documentoOk = c != null && !c.anonimizado() && documentoValido(c.getDocumento());
        List<CobrancaDto> lista = cobrancas.findByOsIdOrderByIdAsc(os.getId()).stream().map(CobrancaDto::de).toList();
        return new InfoDto(gateway.ativo(), c != null, documentoOk, disponivel(os).max(BigDecimal.ZERO), lista);
    }

    /** Saldo da OS que ainda não foi pago, fiado nem cobrado. */
    private BigDecimal disponivel(OrdemServico os) {
        return financeiro.saldoDe(os).subtract(cobrancas.somaPendentes(os.getId()));
    }

    /** CPF/CNPJ informado agora (e guardado no cliente) ou o que o cliente já tem. O Asaas exige. */
    private static String documento(String informado, Cliente cliente) {
        String digitos = informado == null ? "" : informado.replaceAll("\\D", "");
        if (!digitos.isEmpty()) {
            if (!documentoValido(digitos)) {
                throw new NegocioException(HttpStatus.BAD_REQUEST, "O CPF ou CNPJ deve ter 11 ou 14 dígitos.");
            }
            cliente.setDocumento(digitos);
            return digitos;
        }
        String guardado = cliente.getDocumento() == null ? "" : cliente.getDocumento().replaceAll("\\D", "");
        if (!documentoValido(guardado)) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Informe o CPF ou CNPJ do cliente: o Asaas exige para gerar a cobrança.");
        }
        return guardado;
    }

    // shortcut: só confere o tamanho (11 ou 14 dígitos); o Asaas valida os dígitos verificadores e devolve o motivo
    private static boolean documentoValido(String d) {
        if (d == null) return false;
        String n = d.replaceAll("\\D", "");
        return n.length() == 11 || n.length() == 14;
    }

    private static <T> T comTratamento(java.util.function.Supplier<T> acao) {
        try {
            return acao.get();
        } catch (GatewayException e) {
            throw new NegocioException(e.getStatus(), e.getMessage());
        }
    }

    private OrdemServico os(Long id) {
        return ordens.findById(id).orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Ordem de serviço não encontrada."));
    }
}
