package com.oficina.financeiro.gateway;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Cobrança online. O resto do sistema só conhece esta interface: hoje quem a implementa é o Asaas, e a
 * maquininha (InfinitePay, PagBank ou Mercado Pago) entra depois por aqui, sem mexer na OS nem no caixa.
 */
public interface GatewayPagamento {
    enum TipoCobranca { PIX, BOLETO, CREDIT_CARD, UNDEFINED }

    /** @param clienteIdExterno id do cliente no gateway, se já existir (senão o gateway cria o cliente) */
    record NovaCobranca(String clienteIdExterno, String clienteNome, String clienteDocumento, String clienteEmail,
                        TipoCobranca tipo, BigDecimal valor, LocalDate vencimento, String descricao, String referencia) {}

    /** @param link página onde o cliente paga (Pix, boleto ou cartão) */
    record CobrancaCriada(String idCobranca, String clienteIdExterno, String link, String status) {}

    record ConsultaCobranca(String idCobranca, String status, BigDecimal valor, LocalDate dataPagamento, String forma) {}

    /** false quando não há chave configurada: o sistema segue funcionando sem cobrança online. */
    boolean ativo();

    CobrancaCriada criarCobranca(NovaCobranca nova);

    /** Cancela uma cobrança ainda não paga. Não é estorno. */
    void cancelarCobranca(String idCobranca);

    ConsultaCobranca consultar(String idCobranca);
}
