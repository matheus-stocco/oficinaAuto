package com.oficina.orcamentos;

import static com.oficina.pdf.BlocoValoresPdf.SECAO;
import static com.oficina.pdf.BlocoValoresPdf.TEXTO;

import com.lowagie.text.*;
import com.oficina.pdf.BlocoValoresPdf;
import com.oficina.pdf.Formato;
import com.oficina.pdf.PdfGerador;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PDF do orçamento. Abrir o PDF com sucesso marca `gerado_em`, que começa o prazo de expiração. */
@Service
public class OrcamentoPdfService {
    private static final Font TITULO = new Font(Font.HELVETICA, 13, Font.BOLD);

    private final OrcamentoService service;
    private final PdfGerador gerador;

    public OrcamentoPdfService(OrcamentoService service, PdfGerador gerador) {
        this.service = service;
        this.gerador = gerador;
    }

    @Transactional
    public byte[] gerar(Long id) {
        Orcamento o = service.buscar(id);
        byte[] pdf = gerador.gerar(doc -> escrever(doc, o)); // falha (ex.: sem logo) antes de marcar
        service.marcarGerado(o);
        return pdf;
    }

    private void escrever(Document doc, Orcamento o) throws DocumentException {
        doc.add(new Paragraph("Orçamento nº " + o.getId(), TITULO));
        doc.add(new Paragraph("Emissão: " + Formato.data(java.time.LocalDate.now()), TEXTO));
        doc.add(Chunk.NEWLINE);

        String nome = o.getCliente() != null ? o.getCliente().getContato().getNome() : o.getClienteNome();
        String telefone = o.getCliente() != null ? o.getCliente().getContato().getTelefone() : o.getClienteTelefone();
        if (nome != null) doc.add(new Paragraph("Cliente: " + nome + (telefone == null ? "" : "   |   Tel. " + telefone), TEXTO));
        if (o.getVeiculo() != null) {
            var v = o.getVeiculo();
            doc.add(new Paragraph("Veículo: " + ((v.getMarca() == null ? "" : v.getMarca() + " ") + v.getModelo()).trim()
                    + (v.getPlaca() == null ? "" : "   |   Placa " + v.getPlaca()), TEXTO));
        } else if (o.getVeiculoModelo() != null || o.getVeiculoPlaca() != null) {
            doc.add(new Paragraph("Veículo: " + (o.getVeiculoModelo() == null ? "" : o.getVeiculoModelo())
                    + (o.getVeiculoPlaca() == null ? "" : "   |   Placa " + o.getVeiculoPlaca()), TEXTO));
        }
        doc.add(Chunk.NEWLINE);

        BlocoValoresPdf.escrever(doc, o.getItens(), o);

        if (o.getObservacoes() != null) {
            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("Observações", SECAO));
            doc.add(new Paragraph(o.getObservacoes(), TEXTO));
        }
    }
}
